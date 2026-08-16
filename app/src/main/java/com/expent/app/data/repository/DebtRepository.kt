package com.expent.app.data.repository

import com.expent.app.core.DebtPerspective
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.DebtType
import com.expent.app.data.sync.SyncEvent
import com.expent.app.data.sync.SyncEventBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject

class DebtRepository @Inject constructor(
    private val debtDao: DebtDao,
    private val paymentDao: DebtPaymentDao,
    private val authRepository: AuthRepository,
    private val eventBus: SyncEventBus
) {

    suspend fun addDebt(debt: DebtEntity): Long = debtDao.insert(debt)

    /** Share codes already in use locally, so a new code never collides with them. */
    suspend fun getAllShareCodes(): Set<String> = debtDao.getAllShareCodes().toSet()

    /**
     * Updates a debt. Shared debts get a fresh `updatedAt` so the sync engine
     * recognizes the change and pushes it (last-writer-wins).
     */
    suspend fun updateDebt(debt: DebtEntity) {
        val bumped = if (debt.remoteId != null) {
            debt.copy(updatedAt = System.currentTimeMillis())
        } else {
            debt
        }
        debtDao.update(bumped)
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        val remoteId = debt.remoteId
        debtDao.delete(debt)
        remoteId?.let { eventBus.emit(SyncEvent.DebtDeleted(it)) }
    }

    suspend fun deleteDebtById(id: Long) {
        val remoteId = debtDao.getById(id)?.remoteId
        debtDao.deleteById(id)
        remoteId?.let { eventBus.emit(SyncEvent.DebtDeleted(it)) }
    }

    /** Debts with the LENT/BORROWED direction flipped to the signed-in user's perspective. */
    fun observeAll(): Flow<List<DebtWithPaid>> =
        combine(debtDao.observeDebtsWithPaid(), authRepository.authState) { debts, user ->
            debts.map { it.withPerspective(user?.uid) }
        }

    fun observeByIdWithPaid(id: Long): Flow<DebtWithPaid?> =
        combine(debtDao.observeDebtWithPaid(id), authRepository.authState) { debt, user ->
            debt?.withPerspective(user?.uid)
        }

    fun observeById(id: Long): Flow<DebtEntity?> = debtDao.observeById(id)

    /**
     * Records a payment. On a shared debt the payment is stamped with a remote
     * ID, the payer (always the borrowing side of the canonical record), and a
     * fresh timestamp so the sync engine pushes it to the other participant.
     */
    suspend fun addPayment(payment: DebtPaymentEntity): Long {
        val debt = debtDao.getById(payment.debtId)
        if (debt?.remoteId == null) return paymentDao.insert(payment)
        return paymentDao.insert(
            payment.copy(
                remoteId = UUID.randomUUID().toString(),
                payerId = borrowingSide(debt),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePayment(payment: DebtPaymentEntity) {
        val remoteId = payment.remoteId
        paymentDao.delete(payment)
        remoteId?.let { eventBus.emit(SyncEvent.PaymentDeleted(it)) }
    }

    suspend fun deletePaymentById(id: Long) {
        val remoteId = paymentDao.getById(id)?.remoteId
        paymentDao.deleteById(id)
        remoteId?.let { eventBus.emit(SyncEvent.PaymentDeleted(it)) }
    }

    fun observePayments(debtId: Long): Flow<List<DebtPaymentEntity>> =
        paymentDao.observeForDebt(debtId)

    /** Payments always come from the borrower: the other participant for LENT, the creator for BORROWED. */
    private fun borrowingSide(debt: DebtEntity): String? = when (debt.type) {
        DebtType.LENT -> debt.otherParticipantId
        DebtType.BORROWED -> debt.creatorId
    }

    private fun DebtWithPaid.withPerspective(uid: String?): DebtWithPaid =
        copy(debt = debt.copy(type = DebtPerspective.displayedType(debt, uid)))
}

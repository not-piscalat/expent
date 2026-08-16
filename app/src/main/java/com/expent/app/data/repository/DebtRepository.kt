package com.expent.app.data.repository

import com.expent.app.core.DebtPerspective
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.DebtType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class DebtRepository @Inject constructor(
    private val debtDao: DebtDao,
    private val paymentDao: DebtPaymentDao,
    private val authRepository: AuthRepository
) {

    /**
     * Creates a debt stamped with the signed-in user as its owner, so a
     * different account on the same device never sees it. Debts created before
     * this stamping (creatorId null) rely on the legacy fallback in
     * [DebtPerspective.visibleTo].
     */
    suspend fun addDebt(debt: DebtEntity): Long {
        val uid = authRepository.authState.first()?.uid
        return debtDao.insert(if (uid != null) debt.copy(creatorId = uid) else debt)
    }

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

    /**
     * Deletes a debt. Local-only debts are removed outright; shared debts are
     * tombstoned (`deletedAt` + `updatedAt` bump) so the sync engine pushes the
     * deletion to the other participant as an ordinary write — durable across
     * restarts and impossible to race, unlike a hard delete. The debt's
     * payments are tombstoned with it so no orphan docs linger remotely.
     */
    suspend fun deleteDebt(debt: DebtEntity) {
        if (debt.remoteId == null) {
            debtDao.delete(debt)
            return
        }
        val now = System.currentTimeMillis()
        debtDao.update(debt.copy(deletedAt = now, updatedAt = now))
        paymentDao.getByDebtId(debt.id).forEach { payment ->
            if (payment.remoteId != null) {
                paymentDao.update(payment.copy(deletedAt = now, updatedAt = now))
            }
        }
    }

    suspend fun deleteDebtById(id: Long) {
        val debt = debtDao.getById(id) ?: return
        deleteDebt(debt)
    }

    /**
     * Debts the signed-in user may see (participant or creator), with the
     * LENT/BORROWED direction flipped to their perspective. On a shared device
     * this is what keeps one account's debts out of another account's list.
     */
    fun observeAll(): Flow<List<DebtWithPaid>> =
        combine(debtDao.observeDebtsWithPaid(), authRepository.authState) { debts, user ->
            debts.filter { DebtPerspective.visibleTo(it.debt, user?.uid) }
                .map { it.withPerspective(user?.uid) }
        }

    fun observeByIdWithPaid(id: Long): Flow<DebtWithPaid?> =
        combine(debtDao.observeDebtWithPaid(id), authRepository.authState) { debt, user ->
            debt?.takeIf { DebtPerspective.visibleTo(it.debt, user?.uid) }
                ?.withPerspective(user?.uid)
        }

    fun observeById(id: Long): Flow<DebtEntity?> =
        combine(debtDao.observeById(id), authRepository.authState) { debt, user ->
            debt?.takeIf { DebtPerspective.visibleTo(it, user?.uid) }
        }

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

    /**
     * Deletes a payment. Local-only payments are removed outright; shared ones
     * are tombstoned so the deletion propagates as a normal sync write.
     */
    suspend fun deletePayment(payment: DebtPaymentEntity) {
        if (payment.remoteId == null) {
            paymentDao.delete(payment)
            return
        }
        val now = System.currentTimeMillis()
        paymentDao.update(payment.copy(deletedAt = now, updatedAt = now))
    }

    suspend fun deletePaymentById(id: Long) {
        val payment = paymentDao.getById(id) ?: return
        deletePayment(payment)
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

package com.expent.app.data.repository

import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DebtRepository @Inject constructor(
    private val debtDao: DebtDao,
    private val paymentDao: DebtPaymentDao
) {

    suspend fun addDebt(debt: DebtEntity): Long = debtDao.insert(debt)

    suspend fun updateDebt(debt: DebtEntity) = debtDao.update(debt)

    suspend fun deleteDebt(debt: DebtEntity) = debtDao.delete(debt)

    suspend fun deleteDebtById(id: Long) = debtDao.deleteById(id)

    fun observeAll(): Flow<List<DebtWithPaid>> = debtDao.observeDebtsWithPaid()

    fun observeByIdWithPaid(id: Long): Flow<DebtWithPaid?> = debtDao.observeDebtWithPaid(id)

    fun observeById(id: Long): Flow<DebtEntity?> = debtDao.observeById(id)

    suspend fun addPayment(payment: DebtPaymentEntity): Long = paymentDao.insert(payment)

    suspend fun deletePayment(payment: DebtPaymentEntity) = paymentDao.delete(payment)

    suspend fun deletePaymentById(id: Long) = paymentDao.deleteById(id)

    fun observePayments(debtId: Long): Flow<List<DebtPaymentEntity>> =
        paymentDao.observeForDebt(debtId)
}

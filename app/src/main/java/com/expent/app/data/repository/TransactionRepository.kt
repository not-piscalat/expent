package com.expent.app.data.repository

import com.expent.app.data.local.dao.TransactionDao
import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) {

    suspend fun add(transaction: TransactionEntity): Long = dao.insert(transaction)

    suspend fun update(transaction: TransactionEntity) = dao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)

    fun observeById(id: Long): Flow<TransactionEntity?> = dao.observeById(id)

    fun observeAllWithCategory(): Flow<List<TransactionWithCategory>> =
        dao.observeAllWithCategory()

    fun observeBetweenWithCategory(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TransactionWithCategory>> =
        dao.observeBetweenWithCategory(startInclusive, endExclusive)
}

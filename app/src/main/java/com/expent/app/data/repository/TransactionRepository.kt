package com.expent.app.data.repository

import com.expent.app.core.visibleTo
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.local.dao.TransactionDao
import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val authRepository: AuthRepository
) {

    /**
     * Creates a transaction stamped with the signed-in user as its owner, so a
     * different account on the same device never sees it. Created while signed
     * out the transaction stays unowned (visible to everyone, like legacy rows).
     */
    suspend fun add(transaction: TransactionEntity): Long {
        val uid = authRepository.authState.first()?.uid
        return dao.insert(if (uid != null) transaction.copy(ownerId = uid) else transaction)
    }

    suspend fun update(transaction: TransactionEntity) = dao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)

    fun observeById(id: Long): Flow<TransactionEntity?> =
        combine(dao.observeById(id), authRepository.authState) { tx, user ->
            tx?.takeIf { it.visibleTo(user?.uid) }
        }

    /** Transactions the signed-in user may see (their own, plus pre-ownership rows). */
    fun observeAllWithCategory(): Flow<List<TransactionWithCategory>> =
        combine(dao.observeAllWithCategory(), authRepository.authState) { list, user ->
            list.filter { it.transaction.visibleTo(user?.uid) }
        }

    fun observeBetweenWithCategory(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TransactionWithCategory>> =
        combine(dao.observeBetweenWithCategory(startInclusive, endExclusive), authRepository.authState) { list, user ->
            list.filter { it.transaction.visibleTo(user?.uid) }
        }
}

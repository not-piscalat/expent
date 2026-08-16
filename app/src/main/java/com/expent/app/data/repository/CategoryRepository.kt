package com.expent.app.data.repository

import com.expent.app.core.visibleTo
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.local.dao.CategoryDao
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.data.seed.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
    private val authRepository: AuthRepository
) {

    /** The signed-in user's categories (their own, plus pre-ownership rows). */
    fun observeAll(): Flow<List<CategoryEntity>> =
        combine(dao.observeAll(), authRepository.authState) { list, user ->
            list.filter { it.visibleTo(user?.uid) }
        }

    fun observeByType(type: TransactionType): Flow<List<CategoryEntity>> =
        combine(dao.observeByType(type), authRepository.authState) { list, user ->
            list.filter { it.visibleTo(user?.uid) }
        }

    suspend fun getById(id: Long): CategoryEntity? {
        val category = dao.getById(id) ?: return null
        val uid = authRepository.authState.first()?.uid
        return category.takeIf { it.visibleTo(uid) }
    }

    /** Creates a category stamped with the signed-in user as its owner. */
    suspend fun add(category: CategoryEntity): Long {
        val uid = authRepository.authState.first()?.uid
        return dao.insert(if (uid != null) category.copy(ownerId = uid) else category)
    }

    suspend fun update(category: CategoryEntity) = dao.update(category)

    suspend fun delete(category: CategoryEntity) = dao.delete(category)

    /** Seeds the default categories on first launch; safe to call repeatedly. */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        dao.insertAll(DefaultCategories.all)
    }
}

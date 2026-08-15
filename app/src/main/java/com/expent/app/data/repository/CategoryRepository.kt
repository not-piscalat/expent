package com.expent.app.data.repository

import com.expent.app.data.local.dao.CategoryDao
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.data.seed.DefaultCategories
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val dao: CategoryDao
) {

    fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()

    fun observeByType(type: TransactionType): Flow<List<CategoryEntity>> = dao.observeByType(type)

    suspend fun getById(id: Long): CategoryEntity? = dao.getById(id)

    suspend fun add(category: CategoryEntity): Long = dao.insert(category)

    suspend fun update(category: CategoryEntity) = dao.update(category)

    suspend fun delete(category: CategoryEntity) = dao.delete(category)

    /** Seeds the default categories on first launch; safe to call repeatedly. */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        dao.insertAll(DefaultCategories.all)
    }
}

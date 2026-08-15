package com.expent.app.data.repository

import com.expent.app.data.local.dao.RecurringTemplateDao
import com.expent.app.data.local.entity.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepository @Inject constructor(
    private val dao: RecurringTemplateDao
) {

    fun observeAll(): Flow<List<RecurringTemplateEntity>> = dao.observeAll()

    suspend fun getById(id: Long): RecurringTemplateEntity? = dao.getById(id)

    /** Inserts when [template.id] is 0, otherwise updates in place. */
    suspend fun upsert(template: RecurringTemplateEntity) {
        if (template.id == 0L) dao.insert(template) else dao.update(template)
    }

    suspend fun delete(template: RecurringTemplateEntity) = dao.delete(template)
}

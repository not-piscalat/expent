package com.expent.app.data.repository

import com.expent.app.core.visibleTo
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.local.dao.RecurringTemplateDao
import com.expent.app.data.local.entity.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepository @Inject constructor(
    private val dao: RecurringTemplateDao,
    private val authRepository: AuthRepository
) {

    /** The signed-in user's templates (their own, plus pre-ownership rows). */
    fun observeAll(): Flow<List<RecurringTemplateEntity>> =
        combine(dao.observeAll(), authRepository.authState) { list, user ->
            list.filter { it.visibleTo(user?.uid) }
        }

    suspend fun getById(id: Long): RecurringTemplateEntity? {
        val template = dao.getById(id) ?: return null
        val uid = authRepository.authState.first()?.uid
        return template.takeIf { it.visibleTo(uid) }
    }

    /** Inserts when [template.id] is 0, otherwise updates in place. New
     *  templates are stamped with the signed-in user as their owner. */
    suspend fun upsert(template: RecurringTemplateEntity) {
        if (template.id == 0L) {
            val uid = authRepository.authState.first()?.uid
            dao.insert(if (uid != null) template.copy(ownerId = uid) else template)
        } else {
            dao.update(template)
        }
    }

    suspend fun delete(template: RecurringTemplateEntity) = dao.delete(template)
}

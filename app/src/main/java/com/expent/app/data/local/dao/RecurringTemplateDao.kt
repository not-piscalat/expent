package com.expent.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.expent.app.data.local.entity.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTemplateDao {

    @Insert
    suspend fun insert(template: RecurringTemplateEntity): Long

    @Update
    suspend fun update(template: RecurringTemplateEntity)

    @Delete
    suspend fun delete(template: RecurringTemplateEntity)

    @Query("SELECT * FROM recurring_templates ORDER BY title")
    fun observeAll(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates WHERE id = :id")
    suspend fun getById(id: Long): RecurringTemplateEntity?

    @Query("SELECT * FROM recurring_templates")
    suspend fun getAll(): List<RecurringTemplateEntity>

    @Insert
    suspend fun insertAll(templates: List<RecurringTemplateEntity>)

    @Query("DELETE FROM recurring_templates")
    suspend fun clearAll()
}

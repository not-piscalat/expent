package com.expent.app.data.recurring

import com.expent.app.core.RecurringSchedule
import com.expent.app.data.local.dao.RecurringTemplateDao
import com.expent.app.data.local.dao.TransactionDao
import com.expent.app.data.local.entity.TransactionEntity
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns due recurring templates into real transactions. Runs once at app start:
 * for every active template, each occurrence whose due date is on or before
 * today is inserted (with its scheduled date, not today) and the template's
 * next-due date advances — so opening the app after a gap backfills every
 * missed month in one pass.
 */
@Singleton
class RecurringEngine @Inject constructor(
    private val templateDao: RecurringTemplateDao,
    private val transactionDao: TransactionDao
) {

    suspend fun applyDue(today: LocalDate = LocalDate.now()) {
        val zone = ZoneId.systemDefault()
        for (template in templateDao.getAll()) {
            if (!template.isActive) continue
            var due = LocalDate.ofEpochDay(template.nextDueEpochDay)
            var generated = false
            while (!due.isAfter(today)) {
                transactionDao.insert(
                    TransactionEntity(
                        amountCents = template.amountCents,
                        type = template.type,
                        categoryId = template.categoryId,
                        note = template.note,
                        timestamp = due.atStartOfDay(zone).toInstant().toEpochMilli()
                    )
                )
                due = RecurringSchedule.nextDueDate(due, template.frequency, template.dayOfMonth)
                generated = true
            }
            if (generated) {
                templateDao.update(template.copy(nextDueEpochDay = due.toEpochDay()))
            }
        }
    }
}

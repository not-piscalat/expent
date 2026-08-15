package com.expent.app.data.backup

import androidx.room.withTransaction
import com.expent.app.data.local.ExpentDatabase
import com.expent.app.data.local.dao.CategoryDao
import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.dao.RecurringTemplateDao
import com.expent.app.data.local.dao.TransactionDao
import com.expent.app.core.CurrencyOption
import com.expent.app.core.ThemeOption
import com.expent.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupService @Inject constructor(
    private val database: ExpentDatabase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val paymentDao: DebtPaymentDao,
    private val recurringTemplateDao: RecurringTemplateDao,
    private val settingsRepository: SettingsRepository
) {

    suspend fun export(): String {
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            categories = categoryDao.getAll(),
            transactions = transactionDao.getAll(),
            debts = debtDao.getAll(),
            payments = paymentDao.getAll(),
            recurringTemplates = recurringTemplateDao.getAll(),
            settings = BackupSettings(
                currencyCode = settingsRepository.currency.first().code,
                themeCode = settingsRepository.theme.first().code,
                startingBalanceCents = settingsRepository.startingBalance.first()
            )
        )
        return BackupCodec.encode(data)
    }

    /** Replaces all current data with the backup's contents, atomically. */
    suspend fun restore(json: String) {
        val data = BackupCodec.decode(json)
        database.withTransaction {
            recurringTemplateDao.clearAll()
            paymentDao.clearAll()
            debtDao.clearAll()
            transactionDao.clearAll()
            categoryDao.clearAll()

            // Insert parents before children so foreign keys stay valid.
            categoryDao.insertAll(data.categories)
            transactionDao.insertAll(data.transactions)
            debtDao.insertAll(data.debts)
            paymentDao.insertAll(data.payments)
            recurringTemplateDao.insertAll(data.recurringTemplates)
        }
        // Settings are applied outside the transaction; older backups carry none.
        data.settings?.let { settings ->
            settings.currencyCode?.let { settingsRepository.setCurrency(CurrencyOption.fromCode(it)) }
            settings.themeCode?.let { settingsRepository.setTheme(ThemeOption.fromCode(it)) }
            settings.startingBalanceCents?.let { settingsRepository.setStartingBalance(it) }
        }
    }
}

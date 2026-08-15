package com.expent.app.data.backup

import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** App settings that travel with a backup so a restore on a new phone feels whole. */
@Serializable
data class BackupSettings(
    val currencyCode: String? = null,
    val themeCode: String? = null,
    val startingBalanceCents: Long? = null
)

/** A full snapshot of the database, ready to serialize to JSON. */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val payments: List<DebtPaymentEntity> = emptyList(),
    val recurringTemplates: List<RecurringTemplateEntity> = emptyList(),
    /** Null for backups made by older versions: those leave settings untouched. */
    val settings: BackupSettings? = null
)

object BackupCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(data: BackupData): String =
        json.encodeToString(BackupData.serializer(), data)

    fun decode(input: String): BackupData =
        json.decodeFromString(BackupData.serializer(), input)
}

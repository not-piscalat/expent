package com.expent.app.data.backup

import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.TransactionEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A full snapshot of the database, ready to serialize to JSON. */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val payments: List<DebtPaymentEntity> = emptyList()
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

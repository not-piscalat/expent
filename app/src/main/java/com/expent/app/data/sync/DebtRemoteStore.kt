package com.expent.app.data.sync

import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.DebtStatus
import com.expent.app.data.local.entity.DebtType
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A shared debt as it lives in Firestore, keyed by its document ID. */
data class RemoteDebt(
    val docId: String,
    val participants: List<String>,
    val creatorId: String,
    val title: String,
    val personName: String?,
    val type: DebtType,
    val amountCents: Long,
    val note: String?,
    val dueTimestamp: Long?,
    val status: DebtStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long
)

/** A payment of a shared debt as it lives in Firestore. */
data class RemotePayment(
    val docId: String,
    val debtDocId: String,
    val payerId: String?,
    val amountCents: Long,
    val timestamp: Long,
    val note: String?,
    val updatedAt: Long
)

/**
 * The remote side of the mutual-debt sync. Room stays the UI's only data
 * source; this store is the "agreement layer" that two phones share.
 */
interface DebtRemoteStore {

    /** Debts the given user participates in. Emits a fresh list on every change. */
    fun observeMyDebts(uid: String): Flow<List<RemoteDebt>>

    /**
     * All payments across the user's shared debts. Queried with an
     * array-contains filter on `participants` because Firestore rules only
     * permit queries whose own filters satisfy the read rule (a debtId filter
     * would not prove the rule holds and the listen is denied).
     */
    fun observeMyPayments(uid: String): Flow<List<RemotePayment>>

    /** Creates or overwrites the Firestore doc for a shared debt (server timestamp). */
    suspend fun upsertDebt(debt: DebtEntity)

    /** Creates or overwrites the Firestore doc for a shared payment (server timestamp).
     *  [participants] are the parent debt's participants, copied onto the payment doc so
     *  security rules can authorize reads without a parent lookup (rules with get()
     *  cannot guard queries). */
    suspend fun upsertPayment(payment: DebtPaymentEntity, debtRemoteId: String, participants: List<String>)

    suspend fun deleteDebt(remoteId: String)

    suspend fun deletePayment(remoteId: String)
}

@Singleton
class FirestoreDebtStore @Inject constructor() : DebtRemoteStore {

    // Lazy: the store is created at app start, but FirebaseApp is initialized in
    // Application.onCreate — never touch Firestore before then.
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun observeMyDebts(uid: String): Flow<List<RemoteDebt>> = callbackFlow {
        val registration = db.collection(COLLECTION_DEBTS)
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val debts = snapshot?.documents?.mapNotNull { it.toRemoteDebt() } ?: emptyList()
                trySend(debts)
            }
        awaitClose { registration.remove() }
    }

    override fun observeMyPayments(uid: String): Flow<List<RemotePayment>> = callbackFlow {
        val registration = db.collection(COLLECTION_PAYMENTS)
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val payments = snapshot?.documents?.mapNotNull { it.toRemotePayment() } ?: emptyList()
                trySend(payments)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun upsertDebt(debt: DebtEntity) {
        val remoteId = requireNotNull(debt.remoteId) { "Cannot push a debt without a remoteId" }
        db.collection(COLLECTION_DEBTS).document(remoteId).set(debt.toRemoteMap()).await()
    }

    override suspend fun upsertPayment(payment: DebtPaymentEntity, debtRemoteId: String, participants: List<String>) {
        val remoteId = requireNotNull(payment.remoteId) { "Cannot push a payment without a remoteId" }
        db.collection(COLLECTION_PAYMENTS).document(remoteId)
            .set(payment.toRemoteMap(debtRemoteId, participants)).await()
    }

    override suspend fun deleteDebt(remoteId: String) {
        db.collection(COLLECTION_DEBTS).document(remoteId).delete().await()
    }

    override suspend fun deletePayment(remoteId: String) {
        db.collection(COLLECTION_PAYMENTS).document(remoteId).delete().await()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

    private companion object {
        const val COLLECTION_DEBTS = "debts"
        const val COLLECTION_PAYMENTS = "debt_payments"
    }
}

/** Maps a Firestore debt document into [RemoteDebt]; null when the doc is malformed. */
private fun com.google.firebase.firestore.DocumentSnapshot.toRemoteDebt(): RemoteDebt? {
    val docId = id
    val participants = (get("participants") as? List<*>)?.filterIsInstance<String>() ?: return null
    val type = runCatching { DebtType.valueOf(getString("type") ?: "") }.getOrNull() ?: return null
    val status = runCatching { DebtStatus.valueOf(getString("status") ?: "") }.getOrElse { DebtStatus.OPEN }
    return RemoteDebt(
        docId = docId,
        participants = participants,
        creatorId = getString("creatorId") ?: "",
        title = getString("title") ?: "",
        personName = getString("personName"),
        type = type,
        amountCents = getLong("amountCents") ?: 0,
        note = getString("note"),
        dueTimestamp = getLong("dueTimestamp"),
        status = status,
        createdAt = getLong("createdAt") ?: 0,
        updatedAt = (getTimestamp("updatedAt")?.toDate()?.time) ?: (getLong("updatedAt") ?: 0),
        deletedAt = getLong("deletedAt") ?: 0
    )
}

/** Maps a Firestore payment document into [RemotePayment]; null when the doc is malformed. */
private fun com.google.firebase.firestore.DocumentSnapshot.toRemotePayment(): RemotePayment? {
    val debtDocId = getString("debtId") ?: return null
    return RemotePayment(
        docId = id,
        debtDocId = debtDocId,
        payerId = getString("payerId"),
        amountCents = getLong("amountCents") ?: 0,
        timestamp = getLong("timestamp") ?: 0,
        note = getString("note"),
        updatedAt = (getTimestamp("updatedAt")?.toDate()?.time) ?: (getLong("updatedAt") ?: 0)
    )
}

/** Remote form of a local shared debt, ready to write with a fresh server timestamp. */
internal fun DebtEntity.toRemoteMap(): Map<String, Any?> = mapOf(
    "participants" to listOfNotNull(creatorId, otherParticipantId),
    "creatorId" to creatorId,
    "title" to title,
    "personName" to personName,
    "type" to type.name,
    "amountCents" to amountCents,
    "note" to note,
    "dueTimestamp" to dueTimestamp,
    "status" to status.name,
    "createdAt" to createdAt,
    "updatedAt" to FieldValue.serverTimestamp(),
    "deletedAt" to deletedAt
)

/** Remote form of a local shared payment, ready to write with a fresh server timestamp. */
internal fun DebtPaymentEntity.toRemoteMap(debtRemoteId: String, participants: List<String>): Map<String, Any?> = mapOf(
    "debtId" to debtRemoteId,
    "participants" to participants,
    "payerId" to payerId,
    "amountCents" to amountCents,
    "timestamp" to timestamp,
    "note" to note,
    "updatedAt" to FieldValue.serverTimestamp()
)

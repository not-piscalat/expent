package com.expent.app.data.sync

import android.util.Log
import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps shared debts in sync between two accounts. Room stays the UI's only
 * data source; this engine mirrors changes in both directions:
 *
 *  - **Pull** (Firestore -> Room): listens to the user's debts and their
 *    payments, applying last-writer-wins merges keyed by `remoteId`.
 *  - **Push** (Room -> Firestore): watches local changes to shared debts and
 *    writes them with a Firestore server timestamp.
 *
 * Echo-loop prevention lives on the row itself: `lastPushedUpdatedAt` records
 * the `updatedAt` state that was last written to Firestore. A row is pushed
 * only when `updatedAt != lastPushedUpdatedAt` — so a pull-applied write never
 * echoes back, and (because the guard is persisted) a sync restart never
 * re-pushes every row, which used to burn the Firestore write quota on every
 * launch. Local edits bump `updatedAt` in the repository, which is what arms
 * a push; a failed push leaves the row dirty (`lastPushedUpdatedAt` untouched)
 * so the next Room emission or app launch retries it.
 *
 * Deletes are soft: the repository tombstones a shared row (sets `deletedAt`
 * and bumps `updatedAt`) instead of removing it, so the push loop carries the
 * deletion to Firestore like any other write — durable across restarts and
 * immune to the push/delete race that plagued hard deletes. Remote tombstones
 * are applied by marking the local row deleted; tombstoned rows are filtered
 * out of every UI query.
 */
@Singleton
class DebtSyncer @Inject constructor(
    private val userUidFlow: Flow<String?>,
    private val debtDao: DebtDao,
    private val paymentDao: DebtPaymentDao,
    private val remoteStore: DebtRemoteStore,
    private val scope: CoroutineScope
) {

    private var job: Job? = null

    /** Starts listening for auth changes; safe to call once at app start. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            // distinctUntilChanged: authState re-emits on token refresh etc.,
            // and every emission would cancel and restart the whole sync. The
            // per-row push guard survives those restarts, so nothing is
            // re-pushed unless it actually changed.
            userUidFlow.distinctUntilChanged().collectLatest { uid ->
                Log.d(TAG, "sync uid change: $uid")
                if (uid == null) return@collectLatest
                runSync(uid)
            }
        }
    }

    private suspend fun runSync(uid: String) = coroutineScope {
        Log.d(TAG, "runSync started for $uid")
        launch { pushDebts() }
        launch { pushPayments() }
        pullAll(uid)
    }

    // ---------------------------------------------------------------- pull

    /**
     * Pulls debts and payments together so payments are always applied after
     * their parent debt (both snapshots are combined into one emission, so the
     * first arrival of a payment can never precede its debt).
     */
    private suspend fun pullAll(uid: String) {
        var previousDebtIds = emptySet<String>()
        var previousPaymentIds = emptySet<String>()
        combine(
            remoteStore.observeMyDebts(uid).retryWhen { _, _ -> delay(RETRY_DELAY_MS); true },
            remoteStore.observeMyPayments(uid).retryWhen { _, _ -> delay(RETRY_DELAY_MS); true }
        ) { debts, payments -> debts to payments }
            .collect { (remoteDebts, remotePayments) ->
                val debtIds = remoteDebts.map { it.docId }.toSet()

                // Debts that vanished from the snapshot were hard-deleted
                // outside the app (console cleanup); mirror that locally.
                (previousDebtIds - debtIds).forEach { gone ->
                    Log.d(TAG, "pull: debt vanished from snapshot -> local delete: $gone")
                    debtDao.deleteByRemoteId(gone)
                }
                previousDebtIds = debtIds
                Log.d(TAG, "pull snapshot: debts=${remoteDebts.map { it.docId }}")
                remoteDebts.forEach { applyDebt(it) }

                val paymentIds = remotePayments.map { it.docId }.toSet()

                // Payments that vanished from the snapshot were deleted remotely.
                (previousPaymentIds - paymentIds).forEach { gone ->
                    paymentDao.deleteByRemoteId(gone)
                }
                previousPaymentIds = paymentIds
                remotePayments.forEach { applyPayment(it) }
            }
    }

    private suspend fun applyDebt(remote: RemoteDebt) {
        val local = debtDao.getByRemoteId(remote.docId)
        if (local != null && local.updatedAt > remote.updatedAt) return // local edit wins
        if (local != null && local.deletedAt != 0L && remote.deletedAt == 0L) {
            // A local tombstone must never be resurrected by a stale alive
            // snapshot: the delete is pending push, and the remote state may
            // predate it (device clocks vs. server timestamps make the LWW
            // compare unreliable across clock domains). The row stays hidden
            // until the tombstone push lands.
            Log.d(TAG, "applyDebt ${remote.docId}: local tombstone beats alive remote")
            return
        }
        if (remote.deletedAt != 0L) {
            // Remote tombstone: mark the local row deleted, or stay hidden if
            // we never had it. Never insert a row for a deleted debt.
            if (local != null) {
                Log.d(TAG, "applyDebt ${remote.docId}: remote tombstone -> local soft delete")
                debtDao.update(remote.toEntity(local.id))
            }
            return
        }
        val entity = remote.toEntity(local?.id ?: 0L)
        if (local == null) debtDao.insert(entity) else debtDao.update(entity)
    }

    private suspend fun applyPayment(remote: RemotePayment) {
        val parentDebtId = debtDao.getByRemoteId(remote.debtDocId)?.id ?: return // parent not pulled yet
        val local = paymentDao.getByRemoteId(remote.docId)
        if (local != null && local.updatedAt > remote.updatedAt) return // local edit wins
        if (local != null && local.deletedAt != 0L && remote.deletedAt == 0L) {
            // Same rule as debts: a pending local tombstone outranks an alive
            // remote snapshot.
            Log.d(TAG, "applyPayment ${remote.docId}: local tombstone beats alive remote")
            return
        }
        if (remote.deletedAt != 0L) {
            // Remote tombstone: mark the local row deleted (never inserted).
            if (local != null) {
                Log.d(TAG, "applyPayment ${remote.docId}: remote tombstone -> local soft delete")
                paymentDao.update(remote.toEntity(local.id, parentDebtId))
            }
            return
        }
        val entity = remote.toEntity(local?.id ?: 0L, parentDebtId)
        if (local == null) paymentDao.insert(entity) else paymentDao.update(entity)
    }

    // ---------------------------------------------------------------- push

    private suspend fun pushDebts() {
        debtDao.observeSynced().collect { debts ->
            debts.forEach { debt ->
                val remoteId = debt.remoteId ?: return@forEach
                if (debt.updatedAt != debt.lastPushedUpdatedAt) {
                    try {
                        remoteStore.upsertDebt(debt)
                        debtDao.update(debt.copy(lastPushedUpdatedAt = debt.updatedAt))
                    } catch (e: Exception) {
                        Log.w(TAG, "pushDebt failed $remoteId: ${e.message}")
                        // Row stays dirty: the next Room emission or app launch
                        // retries the push.
                    }
                }
            }
        }
    }

    private suspend fun pushPayments() {
        paymentDao.observeSynced().collect { payments ->
            payments.forEach { payment ->
                val remoteId = payment.remoteId ?: return@forEach
                if (payment.updatedAt != payment.lastPushedUpdatedAt) {
                    val parent = debtDao.getById(payment.debtId) ?: return@forEach
                    val debtRemoteId = parent.remoteId ?: return@forEach
                    try {
                        remoteStore.upsertPayment(
                            payment,
                            debtRemoteId,
                            participants = listOfNotNull(parent.creatorId, parent.otherParticipantId)
                        )
                        paymentDao.update(payment.copy(lastPushedUpdatedAt = payment.updatedAt))
                    } catch (e: Exception) {
                        Log.w(TAG, "pushPayment failed $remoteId: ${e.message}")
                        // Retried on the next Room emission or app launch.
                    }
                }
            }
        }
    }

    private companion object {
        const val RETRY_DELAY_MS = 10_000L
        const val TAG = "DebtSync"
    }
}

private fun RemoteDebt.toEntity(localId: Long): DebtEntity = DebtEntity(
    id = localId,
    title = title,
    personName = personName,
    type = type,
    amountCents = amountCents,
    note = note,
    dueTimestamp = dueTimestamp,
    createdAt = createdAt,
    remoteId = docId,
    creatorId = creatorId,
    otherParticipantId = (participants - creatorId).firstOrNull(),
    shareCode = shareCode,
    status = status,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    // The remote already holds this state; mark it pushed so it never echoes.
    lastPushedUpdatedAt = updatedAt
)

private fun RemotePayment.toEntity(localId: Long, localDebtId: Long): DebtPaymentEntity = DebtPaymentEntity(
    id = localId,
    debtId = localDebtId,
    amountCents = amountCents,
    timestamp = timestamp,
    note = note,
    remoteId = docId,
    payerId = payerId,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastPushedUpdatedAt = updatedAt
)

package com.expent.app.data.sync

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
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
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
 * Echo-loop prevention: every remote snapshot records the doc's `updatedAt` in
 * [lastSeenRemoteUpdatedAt]. A Room change is only pushed when its `updatedAt`
 * differs from that last-seen value — so a pull-applied write never echoes back.
 * Local edits bump `updatedAt` in the repository, which is what triggers a push.
 *
 * Deletes travel through [SyncEventBus] because Room flows cannot observe them.
 * Tombstoned (soft) deletes are step 5; for now deletes propagate as hard
 * removals.
 */
@Singleton
class DebtSyncer @Inject constructor(
    private val userUidFlow: Flow<String?>,
    private val debtDao: DebtDao,
    private val paymentDao: DebtPaymentDao,
    private val remoteStore: DebtRemoteStore,
    private val eventBus: SyncEventBus,
    private val scope: CoroutineScope
) {

    private val lastSeenRemoteUpdatedAt = ConcurrentHashMap<String, Long>()
    private var job: Job? = null

    /** Starts listening for auth changes; safe to call once at app start. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            userUidFlow.collectLatest { uid ->
                if (uid == null) return@collectLatest
                runSync(uid)
            }
        }
    }

    private suspend fun runSync(uid: String) = coroutineScope {
        lastSeenRemoteUpdatedAt.clear()
        launch { pushDebts() }
        launch { pushPayments() }
        launch { eventLoop() }
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

                // Debts that vanished from the snapshot were deleted remotely.
                (previousDebtIds - debtIds).forEach { gone ->
                    debtDao.deleteByRemoteId(gone)
                    lastSeenRemoteUpdatedAt.remove(gone)
                }
                previousDebtIds = debtIds
                remoteDebts.forEach { applyDebt(it) }

                val paymentIds = remotePayments.map { it.docId }.toSet()

                // Payments that vanished from the snapshot were deleted remotely.
                (previousPaymentIds - paymentIds).forEach { gone ->
                    paymentDao.deleteByRemoteId(gone)
                    lastSeenRemoteUpdatedAt.remove(gone)
                }
                previousPaymentIds = paymentIds
                remotePayments.forEach { applyPayment(it) }
            }
    }

    private suspend fun applyDebt(remote: RemoteDebt) {
        // Record what the remote side thinks BEFORE deciding, so a newer local
        // edit is recognized as needing a push.
        lastSeenRemoteUpdatedAt[remote.docId] = remote.updatedAt
        val local = debtDao.getByRemoteId(remote.docId)
        if (local != null && local.updatedAt > remote.updatedAt) return // local edit wins
        val entity = remote.toEntity(local?.id ?: 0L)
        if (local == null) debtDao.insert(entity) else debtDao.update(entity)
    }

    private suspend fun applyPayment(remote: RemotePayment) {
        lastSeenRemoteUpdatedAt[remote.docId] = remote.updatedAt
        val parentDebtId = debtDao.getByRemoteId(remote.debtDocId)?.id ?: return // parent not pulled yet
        val local = paymentDao.getByRemoteId(remote.docId)
        if (local != null && local.updatedAt > remote.updatedAt) return // local edit wins
        val entity = remote.toEntity(local?.id ?: 0L, parentDebtId)
        if (local == null) paymentDao.insert(entity) else paymentDao.update(entity)
    }

    // ---------------------------------------------------------------- push

    private suspend fun pushDebts() {
        debtDao.observeSynced().collect { debts ->
            debts.forEach { debt ->
                val remoteId = debt.remoteId ?: return@forEach
                if (lastSeenRemoteUpdatedAt[remoteId] != debt.updatedAt) {
                    try {
                        remoteStore.upsertDebt(debt)
                        lastSeenRemoteUpdatedAt[remoteId] = debt.updatedAt
                    } catch (e: Exception) {
                        // Transient failure: leave lastSeen untouched so the
                        // next Room emission retries.
                    }
                }
            }
        }
    }

    private suspend fun pushPayments() {
        paymentDao.observeSynced().collect { payments ->
            payments.forEach { payment ->
                val remoteId = payment.remoteId ?: return@forEach
                if (lastSeenRemoteUpdatedAt[remoteId] != payment.updatedAt) {
                    val parent = debtDao.getById(payment.debtId) ?: return@forEach
                    val debtRemoteId = parent.remoteId ?: return@forEach
                    try {
                        remoteStore.upsertPayment(
                            payment,
                            debtRemoteId,
                            participants = listOfNotNull(parent.creatorId, parent.otherParticipantId)
                        )
                        lastSeenRemoteUpdatedAt[remoteId] = payment.updatedAt
                    } catch (e: Exception) {
                        // Retried on the next Room emission.
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- deletes

    private suspend fun eventLoop() {
        eventBus.events.collect { event ->
            when (event) {
                is SyncEvent.DebtDeleted -> {
                    try {
                        remoteStore.deleteDebt(event.remoteId)
                    } catch (e: Exception) {
                        // Best-effort; the pull side will converge.
                    }
                    lastSeenRemoteUpdatedAt.remove(event.remoteId)
                }
                is SyncEvent.PaymentDeleted -> {
                    try {
                        remoteStore.deletePayment(event.remoteId)
                    } catch (e: Exception) {
                        // Best-effort.
                    }
                    lastSeenRemoteUpdatedAt.remove(event.remoteId)
                }
            }
        }
    }

    private companion object {
        const val RETRY_DELAY_MS = 10_000L
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
    deletedAt = deletedAt
)

private fun RemotePayment.toEntity(localId: Long, localDebtId: Long): DebtPaymentEntity = DebtPaymentEntity(
    id = localId,
    debtId = localDebtId,
    amountCents = amountCents,
    timestamp = timestamp,
    note = note,
    remoteId = docId,
    payerId = payerId,
    updatedAt = updatedAt
)

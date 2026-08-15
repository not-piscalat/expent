package com.expent.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.expent.app.data.local.ExpentDatabase
import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.DebtStatus
import com.expent.app.data.local.entity.DebtType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** In-memory stand-in for Firestore: state flows plus a record of every write. */
class FakeDebtRemoteStore : DebtRemoteStore {

    data class UpsertedPayment(
        val payment: DebtPaymentEntity,
        val debtRemoteId: String,
        val participants: List<String>
    )

    val debts = MutableStateFlow<List<RemoteDebt>>(emptyList())
    val payments = MutableStateFlow<List<RemotePayment>>(emptyList())
    val upsertedDebts = mutableListOf<DebtEntity>()
    val upsertedPayments = mutableListOf<UpsertedPayment>()
    val deletedDebtIds = mutableListOf<String>()
    val deletedPaymentIds = mutableListOf<String>()
    var debtEmissions = 0

    override fun observeMyDebts(uid: String): Flow<List<RemoteDebt>> =
        debts.onEach { debtEmissions++ }

    override fun observeMyPayments(uid: String): Flow<List<RemotePayment>> = payments

    override suspend fun upsertDebt(debt: DebtEntity) {
        upsertedDebts += debt
    }

    override suspend fun upsertPayment(payment: DebtPaymentEntity, debtRemoteId: String, participants: List<String>) {
        upsertedPayments += UpsertedPayment(payment, debtRemoteId, participants)
    }

    override suspend fun deleteDebt(remoteId: String) {
        deletedDebtIds += remoteId
    }

    override suspend fun deletePayment(remoteId: String) {
        deletedPaymentIds += remoteId
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DebtSyncerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: ExpentDatabase
    private lateinit var debtDao: DebtDao
    private lateinit var paymentDao: DebtPaymentDao
    private lateinit var fakeStore: FakeDebtRemoteStore
    private lateinit var eventBus: SyncEventBus
    private lateinit var uidFlow: MutableStateFlow<String?>
    private lateinit var syncer: DebtSyncer
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, ExpentDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        debtDao = db.debtDao()
        paymentDao = db.debtPaymentDao()
        fakeStore = FakeDebtRemoteStore()
        eventBus = SyncEventBus()
        uidFlow = MutableStateFlow(null)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        syncer = DebtSyncer(uidFlow, debtDao, paymentDao, fakeStore, eventBus, scope)
    }

    @After
    fun tearDown() {
        scope.coroutineContext[Job]?.cancel()
        db.close()
    }

    @Test
    fun `pulls remote debts and payments into Room when signed in`() = runBlocking {
        syncer.start()
        uidFlow.value = "uid-1"

        fakeStore.debts.value = listOf(remoteDebt("doc-1", "uid-1"))
        await { debtDao.getByRemoteId("doc-1") != null }
        val debt = debtDao.getByRemoteId("doc-1")!!
        assertEquals("Shared debt", debt.title)
        assertEquals("uid-1", debt.creatorId)
        assertEquals("uid-2", debt.otherParticipantId)
        assertEquals(DebtStatus.OPEN, debt.status)

        fakeStore.payments.value = listOf(remotePayment("pay-1", "doc-1"))
        await { paymentDao.getByRemoteId("pay-1") != null }
        val payment = paymentDao.getByRemoteId("pay-1")!!
        assertEquals("uid-2", payment.payerId)
        assertEquals(debt.id, payment.debtId)
    }

    @Test
    fun `pull does not overwrite a newer local edit`() = runBlocking {
        debtDao.insert(
            localDebt(remoteId = "doc-1", updatedAt = 200, title = "Local edit")
        )
        syncer.start()
        uidFlow.value = "uid-1"

        fakeStore.debts.value = listOf(remoteDebt("doc-1", "uid-1", updatedAt = 100, title = "Remote"))
        await { fakeStore.debtEmissions >= 1 }
        Thread.sleep(200) // give a wrong overwrite time to happen
        val debt = debtDao.getByRemoteId("doc-1")!!
        assertEquals("Local edit", debt.title)
        assertEquals(200L, debt.updatedAt)
    }

    @Test
    fun `pushes local changes to a shared debt`() = runBlocking {
        debtDao.insert(localDebt(remoteId = "doc-1", updatedAt = 100))
        syncer.start()
        uidFlow.value = "uid-1"

        await { fakeStore.upsertedDebts.any { it.remoteId == "doc-1" } }
        assertEquals(100L, fakeStore.upsertedDebts.single().updatedAt)

        val current = debtDao.getByRemoteId("doc-1")!!
        debtDao.update(current.copy(amountCents = 999, updatedAt = System.currentTimeMillis()))
        await { fakeStore.upsertedDebts.any { it.amountCents == 999L } }
    }

    @Test
    fun `does not echo pulled changes back to the store`() = runBlocking {
        syncer.start()
        uidFlow.value = "uid-1"

        fakeStore.debts.value = listOf(remoteDebt("doc-1", "uid-1", updatedAt = 100))
        await { debtDao.getByRemoteId("doc-1") != null }
        Thread.sleep(300)
        assertEquals(0, fakeStore.upsertedDebts.size)
    }

    @Test
    fun `signing out stops the sync`() = runBlocking {
        syncer.start()
        uidFlow.value = "uid-1"
        fakeStore.debts.value = listOf(remoteDebt("doc-1", "uid-1", updatedAt = 100))
        await { debtDao.getByRemoteId("doc-1") != null }

        uidFlow.value = null
        Thread.sleep(200)
        val countBefore = fakeStore.upsertedDebts.size

        val local = debtDao.getByRemoteId("doc-1")!!
        debtDao.update(local.copy(amountCents = 555, updatedAt = System.currentTimeMillis()))
        Thread.sleep(200)
        assertEquals(countBefore, fakeStore.upsertedDebts.size)
    }

    @Test
    fun `pushes local payments on shared debts with their parent's remote id`() = runBlocking {
        val debtId = debtDao.insert(localDebt(remoteId = "doc-1", updatedAt = 100))
        paymentDao.insert(
            DebtPaymentEntity(
                debtId = debtId, amountCents = 50, timestamp = 1, note = null,
                remoteId = "pay-1", payerId = "uid-2", updatedAt = 50
            )
        )
        syncer.start()
        uidFlow.value = "uid-1"

        await { fakeStore.upsertedPayments.any { it.payment.remoteId == "pay-1" } }
        val pushed = fakeStore.upsertedPayments.single()
        assertEquals("doc-1", pushed.debtRemoteId)
        assertEquals(listOf("uid-1", "uid-2"), pushed.participants)
    }

    @Test
    fun `delete events propagate to the remote store`() = runBlocking {
        syncer.start()
        uidFlow.value = "uid-1"

        eventBus.emit(SyncEvent.DebtDeleted("doc-1"))
        await { fakeStore.deletedDebtIds.contains("doc-1") }

        eventBus.emit(SyncEvent.PaymentDeleted("pay-1"))
        await { fakeStore.deletedPaymentIds.contains("pay-1") }
    }

    @Test
    fun `removes a debt locally when it disappears from the remote snapshot`() = runBlocking {
        syncer.start()
        uidFlow.value = "uid-1"
        fakeStore.debts.value = listOf(remoteDebt("doc-1", "uid-1", updatedAt = 100))
        await { debtDao.getByRemoteId("doc-1") != null }

        fakeStore.debts.value = emptyList()
        await { debtDao.getByRemoteId("doc-1") == null }
    }

    @Test
    fun `removes a payment locally when it disappears from the remote snapshot`() = runBlocking {
        syncer.start()
        uidFlow.value = "uid-1"
        fakeStore.debts.value = listOf(remoteDebt("doc-1", "uid-1", updatedAt = 100))
        await { debtDao.getByRemoteId("doc-1") != null }
        fakeStore.payments.value = listOf(remotePayment("pay-1", "doc-1"))
        await { paymentDao.getByRemoteId("pay-1") != null }

        fakeStore.payments.value = emptyList()
        await { paymentDao.getByRemoteId("pay-1") == null }
    }

    // ------------------------------------------------------------ helpers

    private fun localDebt(remoteId: String, updatedAt: Long, title: String = "Debt") = DebtEntity(
        id = 0,
        title = title,
        personName = null,
        type = DebtType.LENT,
        amountCents = 1_000,
        note = null,
        dueTimestamp = null,
        createdAt = 1,
        remoteId = remoteId,
        creatorId = "uid-1",
        otherParticipantId = "uid-2",
        status = DebtStatus.OPEN,
        updatedAt = updatedAt,
        deletedAt = 0
    )

    private fun remoteDebt(
        docId: String,
        uid: String,
        updatedAt: Long = 100,
        title: String = "Shared debt",
        amountCents: Long = 1_000
    ) = RemoteDebt(
        docId = docId,
        participants = listOf("uid-1", "uid-2"),
        creatorId = "uid-1",
        title = title,
        personName = null,
        type = DebtType.LENT,
        amountCents = amountCents,
        note = null,
        dueTimestamp = null,
        status = DebtStatus.OPEN,
        createdAt = 1,
        updatedAt = updatedAt,
        deletedAt = 0
    )

    private fun remotePayment(docId: String, debtDocId: String, updatedAt: Long = 100) = RemotePayment(
        docId = docId,
        debtDocId = debtDocId,
        payerId = "uid-2",
        amountCents = 200,
        timestamp = 2,
        note = null,
        updatedAt = updatedAt
    )

    private suspend fun await(timeoutMs: Long = 5_000, condition: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(20)
        }
        error("Timed out waiting for condition")
    }
}

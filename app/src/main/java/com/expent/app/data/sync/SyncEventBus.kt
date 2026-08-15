package com.expent.app.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Local-side events the sync engine reacts to by deleting the remote document. */
sealed interface SyncEvent {
    data class DebtDeleted(val remoteId: String) : SyncEvent
    data class PaymentDeleted(val remoteId: String) : SyncEvent
}

/**
 * A tiny in-process bus between the repository (which knows when the user
 * deleted something) and the [DebtSyncer] (which owns remote writes). Room
 * flows cannot observe deletions, so deletes travel through here instead.
 */
@Singleton
class SyncEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    fun emit(event: SyncEvent) {
        _events.tryEmit(event)
    }
}

package com.expent.app.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.ShareCode
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.auth.AuthUser
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.repository.DebtRepository
import com.expent.app.data.sync.DebtRemoteStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A debt the user is about to join, enough to show a preview before committing. */
data class DebtJoinPreview(
    val docId: String,
    val title: String,
    val personName: String?,
    val amountCents: Long,
    val creatorId: String
)

/** The join flow's progress: idle, looking a code up, found, joining, done, or failed. */
sealed interface JoinState {
    data object Idle : JoinState
    data object LookingUp : JoinState
    data class Found(val preview: DebtJoinPreview) : JoinState
    data class NotFound(val code: String) : JoinState
    data object Joining : JoinState
    data class Joined(val title: String) : JoinState
    data class Error(val message: String) : JoinState
}

@HiltViewModel
class DebtsViewModel @Inject constructor(
    debtRepository: DebtRepository,
    private val authRepository: AuthRepository,
    private val remoteStore: DebtRemoteStore
) : ViewModel() {

    val debts: StateFlow<List<DebtWithPaid>> = debtRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** null = signed out; the Debts tab is gated behind a non-null user. */
    val authState: StateFlow<AuthUser?> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _joinState = MutableStateFlow<JoinState>(JoinState.Idle)
    val joinState: StateFlow<JoinState> = _joinState.asStateFlow()

    private var pendingDocId: String? = null
    private var pendingTitle: String? = null

    /**
     * Looks a share code up on the remote side. On success the dialog shows a
     * preview; the actual join happens on [confirmJoin].
     */
    fun lookupJoin(codeInput: String) {
        val code = ShareCode.normalize(codeInput)
        if (code.isEmpty()) return
        viewModelScope.launch {
            val uid = authRepository.authState.first()?.uid ?: return@launch
            _joinState.value = JoinState.LookingUp
            _joinState.value = try {
                val remote = remoteStore.findByShareCode(code)
                if (remote == null) {
                    JoinState.NotFound(code)
                } else {
                    pendingDocId = remote.docId
                    pendingTitle = remote.title
                    JoinState.Found(
                        DebtJoinPreview(
                            docId = remote.docId,
                            title = remote.title,
                            personName = remote.personName,
                            amountCents = remote.amountCents,
                            creatorId = remote.creatorId
                        )
                    )
                }
            } catch (e: Exception) {
                JoinState.Error(e.message ?: "Could not look up that code")
            }
        }
    }

    /** Adds the signed-in user as a participant of the debt found by [lookupJoin]. */
    fun confirmJoin() {
        viewModelScope.launch {
            val uid = authRepository.authState.first()?.uid ?: return@launch
            val docId = pendingDocId ?: return@launch
            _joinState.value = JoinState.Joining
            _joinState.value = try {
                remoteStore.joinDebt(docId, uid)
                JoinState.Joined(pendingTitle ?: "Debt")
            } catch (e: Exception) {
                JoinState.Error(e.message ?: "Could not join this debt")
            }
        }
    }

    fun resetJoin() {
        pendingDocId = null
        pendingTitle = null
        _joinState.value = JoinState.Idle
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogleIdToken(idToken)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}

package com.expent.app.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.auth.AuthUser
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtsViewModel @Inject constructor(
    debtRepository: DebtRepository,
    private val authRepository: AuthRepository
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

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogleIdToken(idToken)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}

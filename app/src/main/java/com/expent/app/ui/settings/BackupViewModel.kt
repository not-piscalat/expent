package com.expent.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.data.backup.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupService: BackupService
) : ViewModel() {

    fun export(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(backupService.export())
        }
    }

    fun restore(json: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(
                runCatching { backupService.restore(json) }
            )
        }
    }
}

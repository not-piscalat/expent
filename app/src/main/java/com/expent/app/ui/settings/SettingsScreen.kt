package com.expent.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.CurrencyOption
import com.expent.app.core.util.MoneyUtil
import com.expent.app.ui.theme.LocalCurrencySymbol
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val currency by settingsViewModel.currency.collectAsStateWithLifecycle()
    val startingBalance by settingsViewModel.startingBalance.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showStartingBalanceDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    fun exportBackup() {
        scope.launch {
            backupViewModel.export { json ->
                val dir = File(context.cacheDir, "backups")
                dir.mkdirs()
                val file = File(dir, "expent-backup.json")
                file.writeText(json)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, null))
            }
        }
    }

    fun restoreFrom(uri: Uri) {
        scope.launch {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
            if (json == null) {
                snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                return@launch
            }
            backupViewModel.restore(json) { result ->
                scope.launch {
                    val message = if (result.isSuccess) {
                        context.getString(R.string.backup_restored)
                    } else {
                        context.getString(R.string.import_failed)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = stringResource(R.string.currency_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            CurrencyOption.entries.forEach { option ->
                val labelRes = when (option) {
                    CurrencyOption.PHP -> R.string.currency_php
                    CurrencyOption.USD -> R.string.currency_usd
                    CurrencyOption.NONE -> R.string.currency_none
                }
                ListItem(
                    headlineContent = { Text(stringResource(labelRes)) },
                    trailingContent = {
                        RadioButton(
                            selected = currency == option,
                            onClick = { settingsViewModel.setCurrency(option) }
                        )
                    },
                    modifier = Modifier.clickable { settingsViewModel.setCurrency(option) }
                )
            }

            Text(
                text = stringResource(R.string.starting_balance),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.starting_balance_label)) },
                supportingContent = {
                    Text(
                        if (startingBalance > 0) {
                            MoneyUtil.format(startingBalance, symbol = LocalCurrencySymbol.current)
                        } else {
                            stringResource(R.string.starting_balance_not_set)
                        }
                    )
                },
                modifier = Modifier.clickable { showStartingBalanceDialog = true }
            )

            Text(
                text = stringResource(R.string.backup),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_backup)) },
                leadingContent = {
                    Icon(imageVector = Icons.Filled.Upload, contentDescription = null)
                },
                modifier = Modifier.clickable { exportBackup() }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_backup)) },
                leadingContent = {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            )
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text(stringResource(R.string.import_backup_title)) },
            text = { Text(stringResource(R.string.import_backup_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri?.let { restoreFrom(it) }
                }) {
                    Text(stringResource(R.string.import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showStartingBalanceDialog) {
        StartingBalanceDialog(
            currentCents = startingBalance,
            onDismiss = { showStartingBalanceDialog = false },
            onSave = { cents ->
                settingsViewModel.setStartingBalance(cents)
                showStartingBalanceDialog = false
            }
        )
    }
}

@Composable
private fun StartingBalanceDialog(
    currentCents: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var input by remember { mutableStateOf(MoneyUtil.toInput(currentCents)) }
    val cents = MoneyUtil.parse(input)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.starting_balance_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = MoneyUtil.sanitizeInput(it) },
                label = { Text(stringResource(R.string.amount_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = cents != null && cents >= 0,
                onClick = { cents?.let(onSave) }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

package com.expent.app.ui.debts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.FormValidation
import com.expent.app.core.util.DateUtil
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.ui.components.DebtSummaryCard
import com.expent.app.ui.components.EmptyState
import com.expent.app.ui.theme.LocalCurrencySymbol
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    debtId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DebtDetailViewModel = hiltViewModel()
) {
    val debt by viewModel.debt.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val shareState by viewModel.shareState.collectAsStateWithLifecycle()
    var showRecordPayment by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteWithUndo by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<DebtPaymentEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val deletedDebtMessage = stringResource(R.string.deleted_debt)
    val deletedPaymentMessage = stringResource(R.string.deleted_payment)
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(deleteWithUndo) {
        if (!deleteWithUndo) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedDebtMessage,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Long
        )
        deleteWithUndo = false
        if (result != SnackbarResult.ActionPerformed) {
            viewModel.deleteDebt()
            onDeleted()
        }
    }

    LaunchedEffect(paymentToDelete) {
        val payment = paymentToDelete ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedPaymentMessage,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Long
        )
        paymentToDelete = null
        if (result != SnackbarResult.ActionPerformed) {
            viewModel.deletePayment(payment.id)
        }
    }

    LaunchedEffect(shareState) {
        val state = shareState
        if (state is ShareState.Error) {
            snackbarHostState.showSnackbar(state.message)
            viewModel.resetShare()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(debt?.debt?.title ?: stringResource(R.string.debt_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (debt != null) {
                        IconButton(onClick = viewModel::shareDebt) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.share_debt)
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit)
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (debt != null) {
                FloatingActionButton(onClick = { showRecordPayment = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.record_payment)
                    )
                }
            }
        }
    ) { innerPadding ->
        val current = debt
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        DebtSummaryCard(current)
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.payments),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (payments.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Payments,
                            title = stringResource(R.string.no_payments_yet),
                            body = stringResource(R.string.no_payments_body),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(payments, key = { it.id }) { payment ->
                        PaymentRow(
                            payment = payment,
                            onDelete = { paymentToDelete = payment }
                        )
                    }
                }
            }
        }
    }

    if (showRecordPayment && debt != null) {
        RecordPaymentDialog(
            onDismiss = { showRecordPayment = false },
            onConfirm = { amountCents, note ->
                viewModel.recordPayment(amountCents, note)
                showRecordPayment = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_debt_title)) },
            text = { Text(stringResource(R.string.delete_debt_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    deleteWithUndo = true
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    (shareState as? ShareState.Ready)?.let { ready ->
        ShareDebtDialog(
            code = ready.code,
            onCopy = {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Expent share code", ready.code))
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.code_copied)) }
            },
            onShareVia = {
                val text = context.getString(R.string.share_code_message, ready.title, ready.code)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, null))
            },
            onDismiss = viewModel::resetShare
        )
    }
}

@Composable
private fun ShareDebtDialog(
    code: String,
    onCopy: () -> Unit,
    onShareVia: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_debt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.share_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 8.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShareVia) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.share_via))
                }
                OutlinedButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.copy_code))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}

@Composable
private fun PaymentRow(payment: DebtPaymentEntity, onDelete: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(MoneyUtil.format(payment.amountCents, symbol = LocalCurrencySymbol.current))
        },
        supportingContent = {
            Text(listOfNotNull(DateUtil.format(payment.timestamp), payment.note).joinToString(" · "))
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    )
}

@Composable
private fun RecordPaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long, note: String?) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val canConfirm = FormValidation.isValidAmount(amountInput)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.record_payment)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = MoneyUtil.sanitizeInput(it) },
                    label = { Text(stringResource(R.string.amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val cents = MoneyUtil.parse(amountInput)
                    if (cents != null) {
                        onConfirm(cents, note.trim().ifEmpty { null })
                    }
                }
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

package com.expent.app.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.util.DateUtil
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.ui.components.DebtSummaryCard
import com.expent.app.ui.components.EmptyState

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
    var showRecordPayment by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
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
                            onDelete = { viewModel.deletePayment(payment.id) }
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
                    viewModel.deleteDebt()
                    onDeleted()
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
}

@Composable
private fun PaymentRow(payment: DebtPaymentEntity, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(MoneyUtil.format(payment.amountCents)) },
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
    val canConfirm = MoneyUtil.parse(amountInput)?.let { it > 0 } == true

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

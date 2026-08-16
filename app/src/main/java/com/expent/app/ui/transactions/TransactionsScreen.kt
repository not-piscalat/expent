package com.expent.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.ui.components.EmptyState
import com.expent.app.ui.components.TransactionRow

@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var deleteWithUndo by remember { mutableStateOf<TransactionEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.deleted_transaction)
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(deleteWithUndo) {
        val entity = deleteWithUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedMessage,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Long
        )
        if (result != SnackbarResult.ActionPerformed) {
            viewModel.delete(entity)
        }
        deleteWithUndo = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_transaction)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = state.selectedMonthStart == null,
                        onClick = { viewModel.selectMonth(null) },
                        label = { Text(stringResource(R.string.all_time)) }
                    )
                }
                items(state.availableMonths, key = { it.startMillis }) { month ->
                    FilterChip(
                        selected = state.selectedMonthStart == month.startMillis,
                        onClick = { viewModel.selectMonth(month.startMillis) },
                        label = { Text(month.label) }
                    )
                }
            }

            if (state.transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = stringResource(
                            if (state.isFiltering) R.string.no_matching_title else R.string.transactions_empty_title
                        ),
                        body = stringResource(
                            if (state.isFiltering) R.string.no_matching_body else R.string.transactions_empty_body
                        ),
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.transactions, key = { it.transaction.id }) { item ->
                        TransactionRow(
                            item = item,
                            onClick = { onEditTransaction(item.transaction.id) },
                            onLongClick = { pendingDelete = item.transaction }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = { Text(stringResource(R.string.delete_transaction_body)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteWithUndo = transaction
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

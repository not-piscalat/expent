package com.expent.app.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.RecurringFrequency
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.ui.components.EmptyState
import com.expent.app.ui.theme.LocalCurrencySymbol
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dueFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    onAddRecurring: () -> Unit,
    onEditRecurring: (Long) -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RecurringTemplateEntity?>(null) }
    var deleteWithUndo by remember { mutableStateOf<RecurringTemplateEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.deleted_recurring)
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(deleteWithUndo) {
        val template = deleteWithUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedMessage,
            actionLabel = undoLabel
        )
        if (result != SnackbarResult.ActionPerformed) {
            viewModel.delete(template)
        }
        deleteWithUndo = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecurring) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_recurring_title)
                )
            }
        }
    ) { innerPadding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.recurring_empty_title),
                    body = stringResource(R.string.recurring_empty_body),
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(templates, key = { it.id }) { template ->
                    ListItem(
                        headlineContent = { Text(template.title) },
                        supportingContent = {
                            Column {
                                Text(
                                    stringResource(R.string.recurring_summary) + ": " +
                                        MoneyUtil.format(template.amountCents, symbol = LocalCurrencySymbol.current) +
                                        if (template.type == TransactionType.INCOME) " · " + stringResource(R.string.income)
                                        else " · " + stringResource(R.string.expense)
                                )
                                Text(
                                    scheduleSummary(template) + " · " +
                                        stringResource(R.string.next_due) + ": " +
                                        LocalDate.ofEpochDay(template.nextDueEpochDay).format(dueFormatter)
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { pendingDelete = template }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        modifier = Modifier.clickable { onEditRecurring(template.id) }
                    )
                }
            }
        }
    }

    pendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_recurring_title)) },
            text = { Text(stringResource(R.string.delete_recurring_body)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteWithUndo = template
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

@Composable
private fun scheduleSummary(template: RecurringTemplateEntity): String =
    if (template.frequency == RecurringFrequency.MONTHLY) {
        stringResource(R.string.recurring_every_month, template.dayOfMonth)
    } else {
        val dayName = DayOfWeek.of(template.dayOfWeek)
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
        stringResource(R.string.recurring_every_week, dayName)
    }

package com.expent.app.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtType
import com.expent.app.ui.components.EmptyState

@Composable
fun DebtsScreen(viewModel: DebtsViewModel = hiltViewModel()) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: add-debt flow (next step) */ }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_debt)
                )
            }
        }
    ) { innerPadding ->
        if (debts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Filled.ImportContacts,
                    title = stringResource(R.string.debts_empty_title),
                    body = stringResource(R.string.debts_empty_body),
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(debts, key = { it.debt.id }) { item ->
                    DebtCard(item)
                }
            }
        }
    }
}

@Composable
private fun DebtCard(item: DebtWithPaid) {
    val debt = item.debt
    val remaining = debt.amountCents - item.totalPaidCents
    val settled = remaining <= 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = debt.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { /* TODO: debt details (next step) */ },
                    label = {
                        Text(
                            stringResource(
                                if (debt.type == DebtType.LENT) R.string.debt_lent else R.string.debt_borrowed
                            )
                        )
                    }
                )
            }
            if (!debt.personName.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.debt_with) + " " + debt.personName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (settled) {
                    stringResource(R.string.debt_settled)
                } else {
                    stringResource(R.string.debt_remaining) + ": " + MoneyUtil.format(remaining)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (settled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = stringResource(R.string.debt_total) + ": " + MoneyUtil.format(debt.amountCents) +
                    " · " + stringResource(R.string.debt_paid) + ": " + MoneyUtil.format(item.totalPaidCents),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val progress = if (debt.amountCents > 0) {
                item.totalPaidCents.toFloat() / debt.amountCents.toFloat()
            } else {
                1f
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

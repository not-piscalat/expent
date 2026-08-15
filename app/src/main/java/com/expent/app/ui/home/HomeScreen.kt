package com.expent.app.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.expent.app.ui.components.EmptyState
import com.expent.app.ui.components.TransactionRow

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.home_this_month),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            BalanceCard(state)
        }

        if (state.monthTransactions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.home_recent_activity),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.monthTransactions.take(5)) { item ->
                TransactionRow(item)
            }
        }
    }
}

@Composable
private fun BalanceCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = MoneyUtil.format(state.balanceCents),
                style = MaterialTheme.typography.displaySmall,
                color = if (state.balanceCents >= 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBlock(
                    label = stringResource(R.string.home_income),
                    value = MoneyUtil.format(state.incomeCents),
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = stringResource(R.string.home_expenses),
                    value = MoneyUtil.format(state.expenseCents),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

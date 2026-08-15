package com.expent.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.expent.app.R
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtType
import com.expent.app.ui.theme.LocalCurrencySymbol

/** Inner content of a debt card: type, person, totals, and progress toward settlement. */
@Composable
fun DebtSummaryCard(item: DebtWithPaid, modifier: Modifier = Modifier) {
    val debt = item.debt
    val remaining = (debt.amountCents - item.totalPaidCents).coerceAtLeast(0)
    val settled = debt.amountCents - item.totalPaidCents <= 0

    Column(modifier = modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = debt.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            AssistChip(
                onClick = { /* informational only */ },
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
                stringResource(R.string.debt_remaining) + ": " +
                    MoneyUtil.format(remaining, symbol = LocalCurrencySymbol.current)
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (settled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        Text(
            text = stringResource(R.string.debt_total) + ": " +
                MoneyUtil.format(debt.amountCents, symbol = LocalCurrencySymbol.current) +
                " · " + stringResource(R.string.debt_paid) + ": " +
                MoneyUtil.format(item.totalPaidCents, symbol = LocalCurrencySymbol.current),
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

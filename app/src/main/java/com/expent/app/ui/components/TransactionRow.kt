package com.expent.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.expent.app.R
import com.expent.app.core.util.DateUtil
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.ui.theme.LocalCurrencySymbol
import com.expent.app.ui.theme.MoneyMedium

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    item: TransactionWithCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val transaction = item.transaction
    val title = item.categoryName
        ?: transaction.note
        ?: stringResource(R.string.uncategorized)

    val subtitleParts = buildList {
        add(DateUtil.format(transaction.timestamp))
        if (item.categoryName != null && !transaction.note.isNullOrBlank()) {
            add(transaction.note)
        }
    }

    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountText = (if (isExpense) "-" else "+") +
        MoneyUtil.format(transaction.amountCents, symbol = LocalCurrencySymbol.current)

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitleParts.joinToString(" · ")) },
        leadingContent = {
            CategoryAvatar(
                iconName = item.categoryIconName,
                colorArgb = item.categoryColorArgb
            )
        },
        trailingContent = {
            Text(
                text = amountText,
                style = MoneyMedium,
                color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}

@Composable
fun CategoryAvatar(iconName: String?, colorArgb: Long?, modifier: Modifier = Modifier) {
    // A coin: the category color minted into a disc with a lighter rim.
    val color = Color(colorArgb ?: 0xFF9E9E9E)
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color, CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryIcons.resolve(iconName),
            contentDescription = null,
            tint = Color.White
        )
    }
}

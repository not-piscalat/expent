package com.expent.app.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.RecurringFrequency
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.ui.components.CategoryIcons
import com.expent.app.ui.theme.LocalCurrencySymbol
import com.expent.app.ui.theme.MoneyInput
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringScreen(
    onDone: () -> Unit,
    onManageCategories: () -> Unit,
    viewModel: AddRecurringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.edit_recurring_title else R.string.add_recurring_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setType(TransactionType.EXPENSE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.expense)) }
                )
                SegmentedButton(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.setType(TransactionType.INCOME) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.income)) }
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.recurring_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.amountInput,
                onValueChange = viewModel::updateAmount,
                label = { Text(stringResource(R.string.amount_label)) },
                prefix = { Text(LocalCurrencySymbol.current, style = MoneyInput) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MoneyInput,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.category_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onManageCategories) {
                    Text(stringResource(R.string.manage))
                }
            }
            val effectiveCategoryId = state.selectedCategoryId ?: categories.firstOrNull()?.id
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = effectiveCategoryId == category.id,
                        onClick = { viewModel.selectCategory(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = CategoryIcons.resolve(category.iconName),
                                contentDescription = null,
                                tint = Color(category.colorArgb)
                            )
                        }
                    )
                }
            }

            Text(
                text = stringResource(R.string.frequency_label),
                style = MaterialTheme.typography.labelLarge
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.frequency == RecurringFrequency.MONTHLY,
                    onClick = { viewModel.setFrequency(RecurringFrequency.MONTHLY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.monthly)) }
                )
                SegmentedButton(
                    selected = state.frequency == RecurringFrequency.WEEKLY,
                    onClick = { viewModel.setFrequency(RecurringFrequency.WEEKLY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.weekly)) }
                )
            }

            if (state.frequency == RecurringFrequency.MONTHLY) {
                Text(
                    text = stringResource(R.string.day_of_month_label),
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((1..31).toList()) { day ->
                        FilterChip(
                            selected = state.dayOfMonth == day,
                            onClick = { viewModel.setDayOfMonth(day) },
                            label = { Text(day.toString()) }
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.day_of_week_label),
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DayOfWeek.entries.toList()) { day ->
                        FilterChip(
                            selected = state.dayOfWeek == day.value,
                            onClick = { viewModel.setDayOfWeek(day.value) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text(stringResource(R.string.note_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.suggestions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.suggested),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.suggestions) { suggestion ->
                        FilterChip(
                            selected = effectiveCategoryId == suggestion.category.id,
                            onClick = { viewModel.selectCategory(suggestion.category.id) },
                            label = { Text(suggestion.category.name) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        viewModel.save()
                        onDone()
                    }
                },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

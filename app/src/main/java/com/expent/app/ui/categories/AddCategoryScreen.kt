package com.expent.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.ui.components.CategoryColors
import com.expent.app.ui.components.CategoryIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    onDone: () -> Unit,
    viewModel: AddCategoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (state.isEditing) R.string.edit_category else R.string.add_category)
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
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.choose_icon),
                style = MaterialTheme.typography.labelLarge
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CategoryIcons.all, key = { it.first }) { (name, icon) ->
                    FilterChip(
                        selected = state.iconName == name,
                        onClick = { viewModel.selectIcon(name) },
                        label = {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            Text(
                text = stringResource(R.string.choose_color),
                style = MaterialTheme.typography.labelLarge
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(CategoryColors.palette, key = { it }) { colorArgb ->
                    ColorSwatch(
                        colorArgb = colorArgb,
                        selected = state.colorArgb == colorArgb,
                        onClick = { viewModel.selectColor(colorArgb) }
                    )
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

@Composable
private fun ColorSwatch(colorArgb: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(colorArgb))
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

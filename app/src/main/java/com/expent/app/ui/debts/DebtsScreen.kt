package com.expent.app.ui.debts

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.ShareCode
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.auth.AuthUser
import com.expent.app.data.auth.GoogleSignIn
import com.expent.app.ui.components.DebtSummaryCard
import com.expent.app.ui.components.EmptyState
import com.expent.app.ui.components.ExpentFab
import com.expent.app.ui.components.LedgerCard
import com.expent.app.ui.theme.LocalCurrencySymbol
import kotlinx.coroutines.launch

@Composable
fun DebtsScreen(
    onAddDebt: () -> Unit,
    onOpenDebt: (Long) -> Unit,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val authUser by viewModel.authState.collectAsStateWithLifecycle()
    val joinState by viewModel.joinState.collectAsStateWithLifecycle()
    var showJoinDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(joinState) {
        val state = joinState
        if (state is JoinState.Joined) {
            showJoinDialog = false
            snackbarHostState.showSnackbar(context.getString(R.string.joined_debt, state.title))
            viewModel.resetJoin()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (authUser != null) {
                ExpentFab(
                    onClick = onAddDebt,
                    contentDescription = stringResource(R.string.add_debt)
                )
            }
        }
    ) { innerPadding ->
        val user = authUser
        if (user == null) {
            SignInGate(
                onSignIn = {
                    scope.launch {
                        val token = try {
                            GoogleSignIn.getIdToken(context)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                e.message ?: context.getString(R.string.sign_in_failed)
                            )
                            null
                        }
                        if (token != null) {
                            viewModel.signInWithGoogleIdToken(token)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SignedInHeader(
                    user = user,
                    onSignOut = viewModel::signOut,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedButton(
                    onClick = {
                        viewModel.resetJoin()
                        showJoinDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.join_shared_debt))
                }
                if (debts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
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
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(debts, key = { it.debt.id }) { item ->
                            LedgerCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenDebt(item.debt.id) }
                            ) {
                                DebtSummaryCard(item)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinDebtDialog(
            joinState = joinState,
            onCodeSubmit = viewModel::lookupJoin,
            onConfirmJoin = viewModel::confirmJoin,
            onDismiss = {
                showJoinDialog = false
                viewModel.resetJoin()
            }
        )
    }
}

@Composable
private fun JoinDebtDialog(
    joinState: JoinState,
    onCodeSubmit: (String) -> Unit,
    onConfirmJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    val code = ShareCode.normalize(codeInput)
    val busy = joinState is JoinState.LookingUp || joinState is JoinState.Joining

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.join_shared_debt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (val state = joinState) {
                    is JoinState.Found -> {
                        Text(
                            text = state.preview.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        state.preview.personName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(R.string.join_confirm_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = MoneyUtil.format(
                                state.preview.amountCents,
                                symbol = LocalCurrencySymbol.current
                            ),
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                    is JoinState.NotFound -> {
                        Text(
                            text = stringResource(R.string.join_not_found, state.code),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is JoinState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> Unit
                }
                if (joinState is JoinState.Idle || joinState is JoinState.NotFound || joinState is JoinState.Error) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = ShareCode.normalize(it) },
                        label = { Text(stringResource(R.string.share_code_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            if (joinState is JoinState.Found) {
                TextButton(onClick = onConfirmJoin) {
                    Text(stringResource(R.string.join))
                }
            } else {
                TextButton(
                    enabled = code.length == ShareCode.CODE_LENGTH && !busy,
                    onClick = { onCodeSubmit(code) }
                ) {
                    Text(stringResource(R.string.look_up))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SignInGate(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.debts_sign_in_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.debts_sign_in_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSignIn) {
            Text(stringResource(R.string.sign_in_with_google))
        }
    }
}

@Composable
private fun SignedInHeader(
    user: AuthUser,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    LedgerCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName ?: user.email ?: user.uid,
                    style = MaterialTheme.typography.titleSmall
                )
                if (user.email != null) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onSignOut) {
                Text(stringResource(R.string.sign_out))
            }
        }
    }
}

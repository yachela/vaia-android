package com.vaia.presentation.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.domain.model.Expense
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.VaiaDatePickerField
import com.vaia.presentation.ui.common.formatDateForDisplay
import com.vaia.presentation.ui.common.moveFocusOnEnterOrTab
import com.vaia.presentation.ui.common.normalizeDateForApi
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.ui.theme.SunAccent
import com.vaia.presentation.viewmodel.ExpensesViewModel

import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    viewModel: ExpensesViewModel
) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val receiptState by viewModel.receiptState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val expenseCreatedSuccessfully = stringResource(R.string.expense_created_successfully)
    val expenseUpdatedSuccessfully = stringResource(R.string.expense_updated_successfully)
    val expenseDeletedSuccessfully = stringResource(R.string.expense_deleted_successfully)
    val expenseCategories = stringArrayResource(R.array.expense_categories).toList()

    LaunchedEffect(createState) {
        when (createState) {
            is ExpensesViewModel.CreateState.Success -> {
                showCreateDialog = false
                snackbarHostState.showSnackbar(expenseCreatedSuccessfully)
                viewModel.resetCreateState()
            }
            is ExpensesViewModel.CreateState.Error -> {
                snackbarHostState.showSnackbar((createState as ExpensesViewModel.CreateState.Error).message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            is ExpensesViewModel.UpdateState.Success -> {
                snackbarHostState.showSnackbar(expenseUpdatedSuccessfully)
                viewModel.resetUpdateState()
            }
            is ExpensesViewModel.UpdateState.Error -> {
                snackbarHostState.showSnackbar((updateState as ExpensesViewModel.UpdateState.Error).message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(deleteState) {
        when (deleteState) {
            is ExpensesViewModel.DeleteState.Success -> {
                snackbarHostState.showSnackbar(expenseDeletedSuccessfully)
                viewModel.resetDeleteState()
            }
            is ExpensesViewModel.DeleteState.Error -> {
                snackbarHostState.showSnackbar((deleteState as ExpensesViewModel.DeleteState.Error).message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(receiptState) {
        when (val s = receiptState) {
            is ExpensesViewModel.ReceiptState.Ready -> {
                openReceiptFile(context, s.bytes)
                viewModel.resetReceiptState()
            }
            is ExpensesViewModel.ReceiptState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetReceiptState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expenses)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_expense))
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding()
            ) {
                AppQuickBar(
                    currentRoute = "trips",
                    onHome = onNavigateHome,
                    onMap = onNavigateOrganizer, // TODO: Implement explore navigation
                    onTrips = onNavigateTrips,
                    onCalendar = onNavigateCalendar,
                    onCurrency = {}
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading && expenses.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error ?: stringResource(R.string.unknown_error), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        WaypathButton(
                            text = stringResource(R.string.retry),
                            onClick = { viewModel.loadExpenses() }
                        )
                    }
                }

                expenses.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.no_expenses))
                        Spacer(modifier = Modifier.height(16.dp))
                        WaypathButton(
                            text = stringResource(R.string.add_expense),
                            onClick = { showCreateDialog = true }
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 16.dp,
                            bottom = 100.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                stringResource(R.string.expenses_summary),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.expenses_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(expenses) { expense ->
                            ExpenseItem(
                                expense = expense,
                                isLoadingReceipt = receiptState is ExpensesViewModel.ReceiptState.Loading,
                                onReceiptClick = { viewModel.downloadReceipt(expense.id) },
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateExpenseDialog(
            isLoading = createState is ExpensesViewModel.CreateState.Loading,
            serverError = (createState as? ExpensesViewModel.CreateState.Error)?.message ?: error,
            onDismiss = { showCreateDialog = false },
            onConfirm = { description, amount, category, date ->
                viewModel.createExpense(description, amount, category, date)
            },
            expenseCategories = expenseCategories
        )
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    isLoadingReceipt: Boolean = false,
    onReceiptClick: () -> Unit = {},
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(SunAccent.copy(alpha = 0.55f))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.description, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(expense.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                        Text("$${expense.amount}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    }
                    Text(formatDateForDisplay(expense.date), style = MaterialTheme.typography.bodySmall)
                }
                if (expense.receiptImageUrl != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isLoadingReceipt) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onReceiptClick) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = stringResource(R.string.view_receipt),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openReceiptFile(context: Context, bytes: ByteArray) {
    val file = File(context.cacheDir, "recibo_${System.currentTimeMillis()}")
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseDialog(
    isLoading: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit,
    expenseCategories: List<String>
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(expenseCategories.first()) }
    var date by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val descriptionRequiredError = stringResource(R.string.description_required_error)
    val amountNumericError = stringResource(R.string.amount_numeric_error)
    val dateValidError = stringResource(R.string.date_valid_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_expense)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager)
                )

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        expenseCategories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                VaiaDatePickerField(
                    value = date,
                    label = stringResource(R.string.date),
                    enabled = !isLoading,
                    onDateSelected = { date = it },
                    modifier = Modifier.fillMaxWidth()
                )

                val message = localError ?: serverError
                if (!message.isNullOrBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            WaypathButton(
                text = stringResource(R.string.save),
                onClick = {
                    val normalizedDate = normalizeDateForApi(date)
                    val amountValue = amount.replace(",", ".").toDoubleOrNull()
                    localError = when {
                        description.isBlank() -> descriptionRequiredError
                        amountValue == null -> amountNumericError
                        normalizedDate == null -> dateValidError
                        else -> null
                    }

                    if (localError == null && amountValue != null && !isLoading) {
                        onConfirm(description.trim(), amountValue, category, normalizedDate.orEmpty())
                    }
                },
                enabled = !isLoading
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(stringResource(R.string.cancel)) }
        }
    )
}

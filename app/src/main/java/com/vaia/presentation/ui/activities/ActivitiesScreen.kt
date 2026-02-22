package com.vaia.presentation.ui.activities

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.domain.model.Activity
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.VaiaDatePickerField
import com.vaia.presentation.ui.common.VaiaTimePickerField
import com.vaia.presentation.ui.common.formatDateForDisplay
import com.vaia.presentation.ui.common.formatTimeForDisplay
import com.vaia.presentation.ui.common.moveFocusOnEnterOrTab
import com.vaia.presentation.ui.common.normalizeDateForApi
import com.vaia.presentation.ui.common.normalizeTimeForApi
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.ui.theme.SunAccent
import com.vaia.presentation.viewmodel.ActivitiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToRoadmap: () -> Unit,
    onNavigateToDocuments: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    viewModel: ActivitiesViewModel
) {
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    var activityToDelete by remember { mutableStateOf<Activity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val activityCreatedSuccessfully = stringResource(R.string.activity_created_successfully)
    val activityUpdatedSuccessfully = stringResource(R.string.activity_updated_successfully)
    val activityDeletedSuccessfully = stringResource(R.string.activity_deleted_successfully)

    LaunchedEffect(createState) {
        when (createState) {
            is ActivitiesViewModel.CreateState.Success -> {
                showCreateDialog = false
                snackbarHostState.showSnackbar(activityCreatedSuccessfully)
                viewModel.resetCreateState()
            }
            is ActivitiesViewModel.CreateState.Error -> {
                snackbarHostState.showSnackbar((createState as ActivitiesViewModel.CreateState.Error).message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            is ActivitiesViewModel.UpdateState.Success -> {
                activityToEdit = null
                snackbarHostState.showSnackbar(activityUpdatedSuccessfully)
                viewModel.resetUpdateState()
            }
            is ActivitiesViewModel.UpdateState.Error -> {
                snackbarHostState.showSnackbar((updateState as ActivitiesViewModel.UpdateState.Error).message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(deleteState) {
        when (deleteState) {
            is ActivitiesViewModel.DeleteState.Success -> {
                activityToDelete = null
                snackbarHostState.showSnackbar(activityDeletedSuccessfully)
                viewModel.resetDeleteState()
            }
            is ActivitiesViewModel.DeleteState.Error -> {
                snackbarHostState.showSnackbar((deleteState as ActivitiesViewModel.DeleteState.Error).message)
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activities)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToExpenses) {
                        Icon(Icons.Default.List, contentDescription = stringResource(R.string.expenses))
                    }
                    TextButton(onClick = onNavigateToRoadmap) {
                        Text(stringResource(R.string.roadmap_trip))
                    }
                    IconButton(onClick = { onNavigateToDocuments(tripId) }) {
                        Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.documents))
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_activity))
                    }
                }
            )
        },
        bottomBar = {
            AppQuickBar(
                currentRoute = "trips",
                onHome = onNavigateHome,
                onTrips = onNavigateTrips,
                onProfile = onNavigateProfile
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SkyBackground.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(padding)
        ) {
            when {
                isLoading && activities.isEmpty() -> {
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
                            onClick = { viewModel.loadActivities() }
                        )
                    }
                }

                activities.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.no_activities))
                        Spacer(modifier = Modifier.height(16.dp))
                        WaypathButton(
                            text = stringResource(R.string.add_activity),
                            onClick = { showCreateDialog = true }
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                stringResource(R.string.activity_plan),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.activities_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(activities) { activity ->
                            ActivityItem(
                                activity = activity,
                                onClick = {},
                                onEdit = { activityToEdit = activity },
                                onDelete = { activityToDelete = activity }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateActivityDialog(
            dialogTitle = stringResource(R.string.new_activity),
            confirmText = stringResource(R.string.save_button),
            initialTitle = "",
            initialDescription = "",
            initialDate = "",
            initialTime = "",
            initialLocation = "",
            initialCost = "",
            isLoading = createState is ActivitiesViewModel.CreateState.Loading,
            serverError = (createState as? ActivitiesViewModel.CreateState.Error)?.message ?: error,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, description, date, time, location, cost ->
                viewModel.createActivity(title, description, date, time, location, cost)
            }
        )
    }

    activityToEdit?.let { activity ->
        CreateActivityDialog(
            dialogTitle = stringResource(R.string.edit_activity),
            confirmText = stringResource(R.string.save_button),
            initialTitle = activity.title,
            initialDescription = activity.description,
            initialDate = formatDateForDisplay(activity.date),
            initialTime = formatTimeForDisplay(activity.time),
            initialLocation = activity.location,
            initialCost = activity.cost.toString(),
            isLoading = updateState is ActivitiesViewModel.UpdateState.Loading,
            serverError = (updateState as? ActivitiesViewModel.UpdateState.Error)?.message,
            onDismiss = {
                activityToEdit = null
                viewModel.resetUpdateState()
            },
            onConfirm = { title, description, date, time, location, cost ->
                viewModel.updateActivity(activity.id, title, description, date, time, location, cost)
            }
        )
    }

    activityToDelete?.let { activity ->
        AlertDialog(
            onDismissRequest = {
                if (deleteState !is ActivitiesViewModel.DeleteState.Loading) {
                    activityToDelete = null
                    viewModel.resetDeleteState()
                }
            },
            title = { Text(stringResource(R.string.delete_activity_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_activity_confirmation, activity.title))
                    val message = (deleteState as? ActivitiesViewModel.DeleteState.Error)?.message
                    if (!message.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteActivity(activity.id) },
                    enabled = deleteState !is ActivitiesViewModel.DeleteState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (deleteState is ActivitiesViewModel.DeleteState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.delete_button))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        activityToDelete = null
                        viewModel.resetDeleteState()
                    },
                    enabled = deleteState !is ActivitiesViewModel.DeleteState.Loading
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ActivityItem(
    activity: Activity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(activity.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(activity.description, style = MaterialTheme.typography.bodyMedium)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_activity))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_activity_title))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${formatDateForDisplay(activity.date)} ${formatTimeForDisplay(activity.time)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("$${activity.cost}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (activity.location.isNotBlank()) {
                Text(
                    text = stringResource(R.string.location_prefix, activity.location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val encodedQuery = Uri.encode(activity.location)
                        uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityDialog(
    dialogTitle: String,
    confirmText: String,
    initialTitle: String,
    initialDescription: String,
    initialDate: String,
    initialTime: String,
    initialLocation: String,
    initialCost: String,
    isLoading: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Double) -> Unit
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var date by remember(initialDate) { mutableStateOf(initialDate) }
    var time by remember(initialTime) { mutableStateOf(initialTime) }
    var location by remember(initialLocation) { mutableStateOf(initialLocation) }
    var cost by remember(initialCost) { mutableStateOf(initialCost) }
    var localError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val titleRequiredError = stringResource(R.string.title_required_error)
    val dateValidError = stringResource(R.string.date_valid_error)
    val timeFormatError = stringResource(R.string.time_format_error)
    val amountNumericError = stringResource(R.string.amount_numeric_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
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
                VaiaDatePickerField(
                    value = date,
                    label = stringResource(R.string.date),
                    enabled = !isLoading,
                    onDateSelected = { date = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager)
                )
                VaiaTimePickerField(
                    value = time,
                    label = stringResource(R.string.time),
                    enabled = !isLoading,
                    onTimeSelected = { time = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager)
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(R.string.location)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager, isDoneField = true)
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text(stringResource(R.string.cost)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
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
                        text = confirmText,
                        onClick = {
                            val normalizedDate = normalizeDateForApi(date)
                            val normalizedTime = normalizeTimeForApi(time)
                            val costValue = if (cost.isBlank()) 0.0 else cost.replace(",", ".").toDoubleOrNull()
                            localError = when {
                                title.isBlank() -> titleRequiredError
                                normalizedDate == null -> dateValidError
                                time.isNotBlank() && normalizedTime == null -> timeFormatError
                                costValue == null -> amountNumericError
                                else -> null
                            }

                            if (localError == null && !isLoading) {
                                onConfirm(
                                    title.trim(),
                                    description.trim(),
                                    normalizedDate.orEmpty(),
                                    normalizedTime.orEmpty(),
                                    location.trim(),
                                    costValue ?: 0.0
                                )
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

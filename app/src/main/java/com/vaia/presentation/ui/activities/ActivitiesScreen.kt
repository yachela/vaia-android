package com.vaia.presentation.ui.activities

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    onNavigateCalendar: () -> Unit,
    onNavigateOrganizer: () -> Unit,
    viewModel: ActivitiesViewModel
) {
    val context = LocalContext.current
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val suggestionsState by viewModel.suggestionsState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    var activityToDelete by remember { mutableStateOf<Activity?>(null) }
    var showSuggestionsSheet by remember { mutableStateOf(false) }
    var suggestionToAdd by remember { mutableStateOf<ActivitySuggestion?>(null) }
    var suggestionDate by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val activityCreatedSuccessfully = stringResource(R.string.activity_created_successfully)
    val activityUpdatedSuccessfully = stringResource(R.string.activity_updated_successfully)
    val activityDeletedSuccessfully = stringResource(R.string.activity_deleted_successfully)

    LaunchedEffect(exportState) {
        if (exportState is ActivitiesViewModel.ExportState.PdfReady) {
            val bytes = (exportState as ActivitiesViewModel.ExportState.PdfReady).bytes
            val file = File(context.cacheDir, "itinerario-$tripId.pdf")
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_itinerary)))
            viewModel.resetExportState()
        } else if (exportState is ActivitiesViewModel.ExportState.Error) {
            snackbarHostState.showSnackbar((exportState as ActivitiesViewModel.ExportState.Error).message)
            viewModel.resetExportState()
        }
    }

    LaunchedEffect(createState) {
        when (createState) {
            is ActivitiesViewModel.CreateState.Success -> {
                showCreateDialog = false
                suggestionToAdd = null
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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.activities),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (exportState is ActivitiesViewModel.ExportState.Loading) {
                        CircularProgressIndicator(modifier = androidx.compose.ui.Modifier.size(24.dp).padding(4.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.exportItinerary() }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.export_pdf))
                        }
                    }
                    IconButton(onClick = {
                        showSuggestionsSheet = true
                        viewModel.loadSuggestions()
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.ia_suggestions))
                    }
                    IconButton(
                        onClick = { shareItinerary(context, activities) },
                        enabled = activities.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_itinerary))
                    }
                    IconButton(onClick = onNavigateToExpenses) {
                        Icon(Icons.Default.List, contentDescription = stringResource(R.string.expenses))
                    }
                    IconButton(onClick = onNavigateToRoadmap) {
                        Icon(Icons.Default.Map, contentDescription = stringResource(R.string.roadmap_trip))
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
                onExplore = {}, // TODO: Implement explore navigation
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
                    val grouped = remember(activities) {
                        activities
                            .sortedWith(compareBy(
                                { normalizeDateForApi(it.date) ?: it.date },
                                { it.time }
                            ))
                            .groupBy { normalizeDateForApi(it.date) ?: it.date }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        grouped.forEach { (date, dayActivities) ->
                            stickyHeader(key = "header_$date") {
                                ActivityDayHeader(date = date)
                            }
                            items(dayActivities, key = { it.id }) { activity ->
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

    if (showSuggestionsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                showSuggestionsSheet = false
                viewModel.resetSuggestionsState()
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(stringResource(R.string.ia_suggestions), style = MaterialTheme.typography.titleLarge)
                }
                when (val state = suggestionsState) {
                    is ActivitiesViewModel.SuggestionsState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ActivitiesViewModel.SuggestionsState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    is ActivitiesViewModel.SuggestionsState.Success -> {
                        state.suggestions.forEach { suggestion ->
                            androidx.compose.material3.Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(suggestion.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        suggestion.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("📍 ${suggestion.location}", style = MaterialTheme.typography.bodySmall)
                                        Text("🕐 ${suggestion.time}", style = MaterialTheme.typography.bodySmall)
                                        if (suggestion.cost > 0) Text("💵 ${suggestion.cost.toInt()} USD", style = MaterialTheme.typography.bodySmall)
                                    }
                                    WaypathButton(
                                        text = stringResource(R.string.add),
                                        onClick = {
                                            suggestionToAdd = suggestion
                                            suggestionDate = ""
                                        },
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    suggestionToAdd?.let { suggestion ->
        AlertDialog(
            onDismissRequest = { suggestionToAdd = null },
            title = { Text(stringResource(R.string.select_date_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.suggestion_activity_label, suggestion.title), style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    VaiaDatePickerField(
                        value = suggestionDate,
                        label = stringResource(R.string.date),
                        enabled = true,
                        onDateSelected = { suggestionDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normalized = normalizeDateForApi(suggestionDate) ?: suggestionDate
                        viewModel.acceptSuggestion(suggestion, normalized)
                    },
                    enabled = suggestionDate.isNotBlank() && createState !is ActivitiesViewModel.CreateState.Loading
                ) {
                    if (createState is ActivitiesViewModel.CreateState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.add))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { suggestionToAdd = null }) { Text(stringResource(R.string.cancel)) }
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
fun ActivityDayHeader(date: String) {
    val label = remember(date) {
        try {
            val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .parse(date)
            java.text.SimpleDateFormat("EEEE, d 'de' MMMM", java.util.Locale("es", "ES"))
                .format(parsed!!)
                .replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            date
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text("$${activity.cost}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            if (activity.location.isNotBlank()) {
                Text(
                    text = stringResource(R.string.location_prefix, activity.location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
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

private fun shareItinerary(context: android.content.Context, activities: List<Activity>) {
    if (activities.isEmpty()) return

    val grouped = activities
        .sortedWith(compareBy({ normalizeDateForApi(it.date) ?: it.date }, { it.time }))
        .groupBy { normalizeDateForApi(it.date) ?: it.date }

    val sb = StringBuilder("📋 Itinerario de actividades\n\n")
    grouped.forEach { (date, dayActivities) ->
        val label = try {
            val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(date)
            java.text.SimpleDateFormat("EEEE, d 'de' MMMM", java.util.Locale("es", "ES"))
                .format(parsed!!)
                .replaceFirstChar { it.uppercase() }
        } catch (_: Exception) { date }

        sb.append("📅 $label\n")
        dayActivities.forEach { activity ->
            sb.append("• ${activity.time} - ${activity.title} (📍 ${activity.location})")
            if (activity.cost > 0) sb.append(" · 💵 ${activity.cost.toInt()} USD")
            sb.append("\n")
        }
        sb.append("\n")
    }
    sb.append("---\nCompartido desde VAIA ✈️")

    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.share_itinerary)))
}

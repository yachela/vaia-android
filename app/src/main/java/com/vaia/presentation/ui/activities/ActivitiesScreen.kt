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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import com.vaia.R
import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion
import com.vaia.presentation.ui.common.ActivityCardSkeleton
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.PlaceAutocompleteField
import com.vaia.presentation.ui.common.VaiaDatePickerField
import com.vaia.presentation.ui.common.VaiaTimePickerField
import com.vaia.presentation.ui.common.formatDateForDisplay
import com.vaia.presentation.ui.common.formatTimeForDisplay
import com.vaia.presentation.ui.common.moveFocusOnEnterOrTab
import com.vaia.presentation.ui.common.normalizeDateForApi
import com.vaia.presentation.ui.common.normalizeTimeForApi
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.theme.SkyBackground
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.ui.text.style.TextAlign
import com.vaia.presentation.navigation.PackingList
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
    onNavigateToPackingList: (String, String, Int) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    onNavigateCurrency: () -> Unit = {},
    viewModel: ActivitiesViewModel
) {
    val context = LocalContext.current
    val trip by viewModel.trip.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val accommodations by viewModel.accommodations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val suggestionsState by viewModel.suggestionsState.collectAsState()
    val visibleSuggestions by viewModel.visibleSuggestions.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateAccommodationDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    var activityToDelete by remember { mutableStateOf<Activity?>(null) }
    var showSuggestionsSheet by remember { mutableStateOf(false) }
    var suggestionToAdd by remember { mutableStateOf<ActivitySuggestion?>(null) }
    var suggestionDate by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val activityCreatedSuccessfully = stringResource(R.string.activity_created_successfully)
    val activityUpdatedSuccessfully = stringResource(R.string.activity_updated_successfully)
    val activityDeletedSuccessfully = stringResource(R.string.activity_deleted_successfully)
    val suggestionAddedSuccess = stringResource(R.string.suggestion_added)
    val suggestionDismissed = stringResource(R.string.suggestion_dismissed)
    val exportPdfError = stringResource(R.string.export_pdf_error)

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
            snackbarHostState.showSnackbar(exportPdfError)
            viewModel.resetExportState()
        }
    }

    LaunchedEffect(createState) {
        when (createState) {
            is ActivitiesViewModel.CreateState.Success -> {
                showCreateDialog = false
                showCreateAccommodationDialog = false
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
    
    LaunchedEffect(visibleSuggestions) {
        if (suggestionsState is ActivitiesViewModel.SuggestionsState.Success && visibleSuggestions.isEmpty()) {
            // Suggestions were dismissed
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
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.list),
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
                    if (exportState is ActivitiesViewModel.ExportState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.exportItinerary() }) {
                            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.export_pdf))
                        }
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
                    onCurrency = onNavigateCurrency
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading && activities.isEmpty() -> {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = 100.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(5) { ActivityCardSkeleton() }
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
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
                            .padding(padding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = stringResource(R.string.map),
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_activities),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_activities_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
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
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 16.dp,
                            bottom = 120.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MenuButton(
                                        icon = Icons.Default.Map,
                                        label = stringResource(R.string.roadmap_trip),
                                        onClick = onNavigateToRoadmap,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MenuButton(
                                        icon = Icons.Default.Folder,
                                        label = stringResource(R.string.documents),
                                        onClick = { onNavigateToDocuments(tripId) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MenuButton(
                                        icon = Icons.Default.List,
                                        label = stringResource(R.string.expenses),
                                        onClick = onNavigateToExpenses,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MenuButton(
                                        icon = Icons.Default.Luggage,
                                        label = "Equipaje",
                                        onClick = {
                                            trip?.let {
                                                val days = calculateDaysUntil(it.startDate)
                                                onNavigateToPackingList(it.id, it.title, days)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Alojamientos",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    IconButton(
                                        onClick = { showCreateAccommodationDialog = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Añadir alojamiento",
                                            tint = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                if (accommodations.isEmpty()) {
                                    Text(
                                        text = "No hay alojamientos registrados",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        accommodations.forEach { accommodation ->
                                            AccommodationItem(
                                                activity = accommodation,
                                                onEdit = { activityToEdit = accommodation },
                                                onDelete = { activityToDelete = accommodation }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.activity_plan),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    IconButton(
                                        onClick = { showCreateDialog = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = stringResource(R.string.add_activity),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                if (activities.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.no_activities_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                                    )
                                } else {
                                    val grouped = activities
                                        .sortedWith(compareBy(
                                            { normalizeDateForApi(it.date) ?: it.date },
                                            { it.time }
                                        ))
                                        .groupBy { normalizeDateForApi(it.date) ?: it.date }

                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        grouped.forEach { (date, dayActivities) ->
                                            ActivityDayHeader(date = date)
                                            dayActivities.forEach { activity ->
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

    if (showCreateAccommodationDialog) {
        CreateActivityDialog(
            dialogTitle = "Nuevo Alojamiento",
            confirmText = stringResource(R.string.save_button),
            initialTitle = "",
            initialDescription = "#alojamiento",
            initialDate = "",
            initialTime = "",
            initialLocation = "",
            initialCost = "",
            isLoading = createState is ActivitiesViewModel.CreateState.Loading,
            serverError = (createState as? ActivitiesViewModel.CreateState.Error)?.message ?: error,
            onDismiss = { showCreateAccommodationDialog = false },
            onConfirm = { title, description, date, time, location, cost ->
                val finalTitle = if (title.startsWith("[HOSPEDAJE]", ignoreCase = true)) title else "[HOSPEDAJE] $title"
                val finalDescription = if (description.contains("#alojamiento", ignoreCase = true)) description else "$description #alojamiento"
                viewModel.createActivity(finalTitle, finalDescription, date, time, location, cost)
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
                    Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.ia_suggestions), tint = MaterialTheme.colorScheme.tertiary)
                    Text(stringResource(R.string.ia_suggestions), style = MaterialTheme.typography.titleLarge)
                }
                when (val state = suggestionsState) {
                    is ActivitiesViewModel.SuggestionsState.Loading -> {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ActivitiesViewModel.SuggestionsState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.ia_suggestions),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.loadSuggestions() }
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                    is ActivitiesViewModel.SuggestionsState.Success -> {
                        if (visibleSuggestions.isEmpty()) {
                            EmptySuggestionsMessage(
                                onRequestNewSuggestions = {
                                    viewModel.loadSuggestions()
                                }
                            )
                        } else {
                            visibleSuggestions.forEach { suggestion ->
                                SwipeableSuggestionCard(
                                    suggestion = suggestion,
                                    onAccept = {
                                        suggestionToAdd = suggestion
                                        suggestionDate = ""
                                    },
                                    onDismiss = {
                                        viewModel.dismissSuggestion(suggestion)
                                    }
                                )
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
fun MenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AccommodationItem(
    activity: Activity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val cleanTitle = activity.title.removePrefix("[HOSPEDAJE] ").removePrefix("[hospedaje] ")
    
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Map, // Podría usarse un icono de casa si estuviera disponible
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activity.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        val encodedQuery = Uri.encode(activity.location)
                        uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                    }
                )
                Text(
                    text = formatDateForDisplay(activity.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent) // Cambiado de background sólido a transparente
            .padding(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Un pequeño acento de color para separar visualmente los días
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary // Usamos el color primario para la fecha
            )
        }
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
                    Text(
                        text = (activity.title as Any?)?.toString() ?: "Sin título",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (activity.description as Any?)?.toString() ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                    text = "${formatDateForDisplay((activity.date as Any?)?.toString() ?: "")} ${formatTimeForDisplay((activity.time as Any?)?.toString() ?: "")}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "$${activity.cost ?: 0.0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            val location = (activity.location as Any?)?.toString() ?: ""
            if (location.isNotBlank()) {
                Text(
                    text = stringResource(R.string.location_prefix, location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.clickable {
                        val encodedQuery = Uri.encode(location)
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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LocalActivity,
                            contentDescription = "Activity Title Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Description Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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
                PlaceAutocompleteField(
                    label = stringResource(R.string.location),
                    value = location,
                    onValueChange = { location = it },
                    enabled = !isLoading,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text(stringResource(R.string.cost)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = "Cost Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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

    val sb = StringBuilder("Itinerario de actividades\n\n")
    grouped.forEach { (date, dayActivities) ->
        val label = try {
            val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(date)
            java.text.SimpleDateFormat("EEEE, d 'de' MMMM", java.util.Locale("es", "ES"))
                .format(parsed!!)
                .replaceFirstChar { it.uppercase() }
        } catch (_: Exception) { date }

        sb.append("$label\n")
        dayActivities.forEach { activity ->
            sb.append("• ${activity.time} - ${activity.title} (Ubicación: ${activity.location})")
            if (activity.cost > 0) sb.append(" · Costo: ${activity.cost.toInt()} USD")
            sb.append("\n")
        }
        sb.append("\n")
    }
    sb.append("---\nCompartido desde VAIA")

    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.share_itinerary)))
}

private fun calculateDaysUntil(dateString: String): Int {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val tripDate = sdf.parse(dateString) ?: return 0
        
        val now = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        
        val diff = tripDate.time - now.time
        (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }
}

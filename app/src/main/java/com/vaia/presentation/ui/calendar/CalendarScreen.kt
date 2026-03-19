package com.vaia.presentation.ui.calendar

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaia.R
import com.vaia.di.AppContainer
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.normalizeDateForApi
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.viewmodel.CalendarViewModel
import com.vaia.presentation.viewmodel.CalendarViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ── Data model ────────────────────────────────────────────────────────────────

private sealed class CalendarEvent {
    data class Vaia(
        val activityId: String,
        val title: String,
        val time: String,
        val location: String,
        val tripTitle: String,
        val tripId: String
    ) : CalendarEvent()

    data class System(
        val id: Long,
        val title: String,
        val location: String?,
        val startMillis: Long
    ) : CalendarEvent()
}

private sealed class ListItem {
    data class Header(val date: LocalDate) : ListItem()
    data class Event(val event: CalendarEvent) : ListItem()
}

private data class SystemEventRaw(
    val id: Long,
    val title: String,
    val location: String?,
    val startMillis: Long,
    val date: LocalDate
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    appContainer: AppContainer,
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateOrganizer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val calendarViewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModelFactory(appContainer.tripRepository, appContainer.activityRepository)
    )
    val vmState by calendarViewModel.state.collectAsState()

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var systemEvents by remember { mutableStateOf<List<SystemEventRaw>>(emptyList()) }

    suspend fun loadSystemEvents() {
        systemEvents = withContext(Dispatchers.IO) { querySystemEvents(context) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCalendarPermission = granted
        if (granted) scope.launch { loadSystemEvents() }
        else scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.calendar_permission_required))
        }
    }

    LaunchedEffect(Unit) {
        if (hasCalendarPermission) loadSystemEvents()
    }

    // Merge VAIA + system events grouped by date
    val listItems: List<ListItem> = remember(vmState, systemEvents) {
        buildListItems(vmState, systemEvents)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_title)) },
                actions = {
                    IconButton(onClick = {
                        calendarViewModel.load()
                        if (hasCalendarPermission) scope.launch { loadSystemEvents() }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        },
        bottomBar = {
            AppQuickBar(
                currentRoute = "calendar",
                onHome = onNavigateHome,
                onExplore = {}, // TODO: Add explore navigation
                onTrips = onNavigateTrips,
                onProfile = onNavigateProfile
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        when {
            vmState is CalendarViewModel.State.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            vmState is CalendarViewModel.State.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (vmState as CalendarViewModel.State.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SkyBackground.copy(alpha = 0.3f))
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Banner to connect system calendar
                    if (!hasCalendarPermission) {
                        item {
                            SystemCalendarBanner(
                                onConnect = {
                                    permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }
                            )
                        }
                    }

                    if (listItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        stringResource(R.string.calendar_no_events),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(listItems, key = { item ->
                            when (item) {
                                is ListItem.Header -> "header_${item.date}"
                                is ListItem.Event -> when (val e = item.event) {
                                    is CalendarEvent.Vaia -> "vaia_${e.activityId}"
                                    is CalendarEvent.System -> "sys_${e.id}_${e.startMillis}"
                                }
                            }
                        }) { item ->
                            when (item) {
                                is ListItem.Header -> DateHeader(item.date)
                                is ListItem.Event -> EventCard(item.event)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun DateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> {
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
                .replaceFirstChar { it.uppercase() }
            val formatted = date.format(DateTimeFormatter.ofPattern("d MMM", Locale("es")))
            "$dayName, $formatted"
        }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun EventCard(event: CalendarEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color stripe
            val stripeColor = if (event is CalendarEvent.Vaia)
                MintPrimary
            else
                MaterialTheme.colorScheme.secondary

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(stripeColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                when (event) {
                    is CalendarEvent.Vaia -> {
                        Text(
                            event.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            event.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (event.location.isNotBlank()) {
                            Text(
                                event.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            event.tripTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MintPrimary
                        )
                    }
                    is CalendarEvent.System -> {
                        Text(
                            event.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (!event.location.isNullOrBlank()) {
                            Text(
                                event.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            stringResource(R.string.calendar_system_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemCalendarBanner(onConnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.calendar_connect_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    stringResource(R.string.calendar_connect_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onConnect) {
                Text(stringResource(R.string.calendar_connect_action))
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildListItems(
    vmState: CalendarViewModel.State,
    systemEvents: List<SystemEventRaw>
): List<ListItem> {
    val vaiaActivities = (vmState as? CalendarViewModel.State.Ready)?.activities ?: emptyList()

    // Map VAIA activities to (date, event)
    val vaiaByDate = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()
    for (ta in vaiaActivities) {
        val dateStr = normalizeDateForApi(ta.activity.date) ?: continue
        val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
        vaiaByDate.getOrPut(date) { mutableListOf() }.add(
            CalendarEvent.Vaia(
                activityId = ta.activity.id,
                title = ta.activity.title,
                time = ta.activity.time,
                location = ta.activity.location,
                tripTitle = ta.trip.title,
                tripId = ta.trip.id
            )
        )
    }

    // Map system events to (date, event)
    val systemByDate = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()
    for (se in systemEvents) {
        systemByDate.getOrPut(se.date) { mutableListOf() }.add(
            CalendarEvent.System(se.id, se.title, se.location, se.startMillis)
        )
    }

    // Merge all dates
    val allDates = (vaiaByDate.keys + systemByDate.keys).toSortedSet()
    val items = mutableListOf<ListItem>()
    for (date in allDates) {
        items.add(ListItem.Header(date))
        val dayEvents = mutableListOf<CalendarEvent>()
        dayEvents.addAll(vaiaByDate[date] ?: emptyList())
        dayEvents.addAll(systemByDate[date] ?: emptyList())
        // Sort VAIA by time string, system events appended after
        dayEvents.sortWith(compareBy {
            when (it) {
                is CalendarEvent.Vaia -> it.time
                is CalendarEvent.System -> {
                    val local = Instant.ofEpochMilli(it.startMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                    local.toString()
                }
            }
        })
        dayEvents.forEach { items.add(ListItem.Event(it)) }
    }
    return items
}

private fun querySystemEvents(context: android.content.Context): List<SystemEventRaw> {
    val now = System.currentTimeMillis()
    val endRange = now + (1000L * 60 * 60 * 24 * 365) // 1 año

    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
        ContentUris.appendId(this, now)
        ContentUris.appendId(this, endRange)
    }.build()

    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.EVENT_LOCATION
    )

    val cursor = context.contentResolver.query(
        uri, projection, null, null,
        "${CalendarContract.Instances.BEGIN} ASC"
    ) ?: return emptyList()

    return cursor.use {
        val eventIdIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
        val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val beginIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val locationIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
        val zone = ZoneId.systemDefault()
        val result = mutableListOf<SystemEventRaw>()

        while (it.moveToNext()) {
            val begin = it.getLong(beginIdx)
            val date = Instant.ofEpochMilli(begin).atZone(zone).toLocalDate()
            result.add(
                SystemEventRaw(
                    id = it.getLong(eventIdIdx),
                    title = it.getString(titleIdx) ?: "(Sin título)",
                    location = it.getString(locationIdx)?.takeIf { l -> l.isNotBlank() },
                    startMillis = begin,
                    date = date
                )
            )
        }
        result
    }
}

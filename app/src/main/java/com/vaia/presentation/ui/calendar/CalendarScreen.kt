@file:Suppress("NewApi")

package com.vaia.presentation.ui.calendar

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vaia.R
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.TopBar
import com.vaia.presentation.ui.common.normalizeDateForApi
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.viewmodel.CalendarViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ── Data model ────────────────────────────────────────────────────────────────

private sealed class CalendarEvent {
    data class Vaia(
        val activityId: String,
        val title: String,
        val time: String,
        val location: String,
        val tripTitle: String,
        val tripId: String,
        val date: LocalDate
    ) : CalendarEvent()

    data class System(
        val id: Long,
        val title: String,
        val location: String?,
        val startMillis: Long,
        val date: LocalDate
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
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    onNavigateCurrency: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val vmState by calendarViewModel.state.collectAsState()

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var systemEvents by remember { mutableStateOf<List<SystemEventRaw>>(emptyList()) }
    
    // Filtro por fecha seleccionada
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val startOfWeek = remember(selectedDate) { selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val endOfWeek = remember(selectedDate) { startOfWeek.plusDays(6) }

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

    val listItems: List<ListItem> = remember(vmState, systemEvents, startOfWeek, endOfWeek) {
        buildFilteredListItems(vmState, systemEvents, startOfWeek, endOfWeek)
    }

    val allEventCounts = remember(vmState, systemEvents) {
        val counts = mutableMapOf<LocalDate, Int>()
        val vaiaActivities = (vmState as? CalendarViewModel.State.Ready)?.activities ?: emptyList()
        for (ta in vaiaActivities) {
            val dateStr = normalizeDateForApi(ta.activity.date) ?: continue
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
            counts[date] = (counts[date] ?: 0) + 1
        }
        for (se in systemEvents) {
            counts[se.date] = (counts[se.date] ?: 0) + 1
        }
        counts
    }

    Scaffold(
        topBar = {
            TopBar(onNotificationsClick = onNavigateToNotifications, onProfileClick = onNavigateProfile )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                AppQuickBar(
                    currentRoute = "calendar",
                    onHome = onNavigateHome,
                    onMap = onNavigateOrganizer,
                    onTrips = onNavigateTrips,
                    onCalendar = onNavigateCalendar,
                    onCurrency = onNavigateCurrency
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (!hasCalendarPermission) {
                SystemCalendarBanner(
                    onConnect = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                )
            }
            // Calendario Mensual (Controlador)
            MonthlyCalendar(
                selectedDate = selectedDate,
                eventCounts = allEventCounts,
                onDateSelected = { selectedDate = it },
                onGoToToday = { selectedDate = LocalDate.now() },
                modifier = Modifier.padding(16.dp)
            )

            // Cabecera de la sección de lista
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Actividades de la Semana",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${startOfWeek.format(DateTimeFormatter.ofPattern("d MMM", Locale("es")))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("d MMM", Locale("es")))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    calendarViewModel.load()
                    if (hasCalendarPermission) scope.launch { loadSystemEvents() }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            // Lista de Actividades
            Box(modifier = Modifier.weight(1f)) {
                when {
                    vmState is CalendarViewModel.State.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    vmState is CalendarViewModel.State.Error -> {
                        Text((vmState as CalendarViewModel.State.Error).message, 
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (listItems.isEmpty()) {
                                item { EmptyStateView() }
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
                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ── Componentes UI ────────────────────────────────────────────────────────────

@Composable
private fun MonthlyCalendar(
    selectedDate: LocalDate,
    eventCounts: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onGoToToday, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Hoy", style = MaterialTheme.typography.labelLarge, color = MintPrimary)
                    }
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                    }
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }

            // Días de la semana
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
                listOf("L", "M", "M", "J", "V", "S", "D").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Grid de días
            val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
            val days = (0 until totalCells).map { i ->
                if (i < startOffset || i >= startOffset + daysInMonth) null
                else currentMonth.atDay(i - startOffset + 1)
            }

            days.chunked(7).forEach { weekDays ->
                // Determinar si esta fila contiene la fecha seleccionada
                val isSelectedWeek = weekDays.any { it != null && it == selectedDate }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelectedWeek) MintPrimary.copy(alpha = 0.12f) else Color.Transparent)
                ) {
                    weekDays.forEach { date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        date == LocalDate.now() -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = date != null) { date?.let { onDateSelected(it) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                val count = eventCounts[date] ?: 0
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            date == LocalDate.now() -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (count > 0) {
                                        EventIndicatorDots(count = count, isSelected = false)
                                    } else {
                                        Box(modifier = Modifier.height(10.dp)) // Espacio equivalente a los puntos
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

@Composable
private fun EventIndicatorDots(count: Int, isSelected: Boolean) {
    val dotsToShow = count.coerceAtMost(4)
    val dotColor = if (isSelected) Color.White else MintPrimary
    val dotSize = 4.dp
    val spacing = 2.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier.height(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            if (dotsToShow >= 2) {
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(dotColor))
            } else {
                Spacer(modifier = Modifier.size(dotSize))
            }
        }
        if (dotsToShow >= 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                if (dotsToShow >= 4) {
                    Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                } else {
                    Spacer(modifier = Modifier.size(dotSize))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(dotSize))
        }
    }
}


@Composable
private fun DateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana"
        else -> {
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
            val formatted = date.format(DateTimeFormatter.ofPattern("d MMM", Locale("es")))
            "$dayName, $formatted"
        }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MintPrimary,
        modifier = Modifier.padding(top = 12.dp, start = 4.dp)
    )
}

@Composable
private fun EventCard(event: CalendarEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (stripeColor, label) = if (event is CalendarEvent.Vaia) 
                MintPrimary to event.tripTitle 
            else 
                MaterialTheme.colorScheme.secondary to "Google Calendar"

            Box(modifier = Modifier.width(4.dp).height(45.dp).clip(RoundedCornerShape(2.dp)).background(stripeColor))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (event is CalendarEvent.Vaia) event.title else (event as CalendarEvent.System).title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    val timeText = if (event is CalendarEvent.Vaia) event.time else {
                        val e = event as CalendarEvent.System
                        Instant.ofEpochMilli(e.startMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
                    }
                    Text(timeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val location = if (event is CalendarEvent.Vaia) event.location else (event as CalendarEvent.System).location
                    if (!location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = stripeColor, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Sin actividades esta semana", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Usa el calendario para explorar otros días", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun SystemCalendarBanner(
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sincroniza tu calendario", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Ver eventos externos junto a tus viajes", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onConnect, shape = RoundedCornerShape(10.dp)) {
                Text("Conectar", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildFilteredListItems(
    vmState: CalendarViewModel.State,
    systemEvents: List<SystemEventRaw>,
    startOfWeek: LocalDate,
    endOfWeek: LocalDate
): List<ListItem> {
    val vaiaActivities = (vmState as? CalendarViewModel.State.Ready)?.activities ?: emptyList()
    val eventsByDate = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()

    for (ta in vaiaActivities) {
        val dateStr = normalizeDateForApi(ta.activity.date) ?: continue
        val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
        if (!date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)) {
            eventsByDate.getOrPut(date) { mutableListOf() }.add(
                CalendarEvent.Vaia(ta.activity.id, ta.activity.title, ta.activity.time, ta.activity.location, ta.trip.title, ta.trip.id, date)
            )
        }
    }

    for (se in systemEvents) {
        if (!se.date.isBefore(startOfWeek) && !se.date.isAfter(endOfWeek)) {
            eventsByDate.getOrPut(se.date) { mutableListOf() }.add(
                CalendarEvent.System(se.id, se.title, se.location, se.startMillis, se.date)
            )
        }
    }

    val items = mutableListOf<ListItem>()
    eventsByDate.keys.toSortedSet().forEach { date ->
        items.add(ListItem.Header(date))
        val dayEvents = eventsByDate[date]!!.sortedBy {
            when (it) {
                is CalendarEvent.Vaia -> it.time
                is CalendarEvent.System -> Instant.ofEpochMilli(it.startMillis).atZone(ZoneId.systemDefault()).toLocalTime().toString()
            }
        }
        dayEvents.forEach { items.add(ListItem.Event(it)) }
    }
    return items
}

private fun querySystemEvents(context: android.content.Context): List<SystemEventRaw> {
    val startRange = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30) // 30 días atrás
    val endRange = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365) // 1 año adelante
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
        ContentUris.appendId(this, startRange)
        ContentUris.appendId(this, endRange)
    }.build()

    val projection = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.EVENT_LOCATION)
    val cursor = context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC") ?: return emptyList()

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
            result.add(SystemEventRaw(it.getLong(eventIdIdx), it.getString(titleIdx) ?: "(Sin título)", it.getString(locationIdx)?.takeIf { l -> l.isNotBlank() }, begin, date))
        }
        result
    }
}

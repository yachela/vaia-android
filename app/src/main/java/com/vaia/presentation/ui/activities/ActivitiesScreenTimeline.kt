package com.vaia.presentation.ui.activities

import android.content.Intent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.viewmodel.ActivitiesViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreenTimeline(
    tripId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToDocuments: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    viewModel: ActivitiesViewModel
) {
    val context = LocalContext.current
    val timelineData by viewModel.timelineData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Itinerario", "Gastos", "Checklist")

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            1 -> onNavigateToExpenses()
            2 -> onNavigateToDocuments(tripId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.activities),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add activity */ }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_activity))
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
            Column(
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
                    .padding(top = padding.calculateTopPadding())
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                when {
                    isLoading && timelineData == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                error ?: stringResource(R.string.unknown_error),
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            WaypathButton(
                                text = stringResource(R.string.retry),
                                onClick = { viewModel.loadActivities() }
                            )
                        }
                    }

                    timelineData == null -> {
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
                                onClick = { /* TODO: Show create dialog */ }
                            )
                        }
                    }

                    else -> {
                        TimelineContent(
                            timelineData = timelineData!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineContent(
    timelineData: ActivitiesViewModel.TimelineData,
    modifier: Modifier = Modifier
) {
    var selectedDayIndex by remember { mutableStateOf(0) }
    val totalDays = timelineData.days.size
    val selectedDay = timelineData.days.getOrNull(selectedDayIndex) ?: timelineData.days.firstOrNull()
    
    Column(modifier = modifier) {
        if (totalDays > 1) {
            ScrollableTabRow(
                selectedTabIndex = selectedDayIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                timelineData.days.forEachIndexed { index, dayData ->
                    Tab(
                        selected = selectedDayIndex == index,
                        onClick = { selectedDayIndex = index },
                        text = {
                            Text(
                                text = stringResource(R.string.day_tab_label, dayData.dayNumber),
                                fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
        
        selectedDay?.let { day ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "header_${day.date}") {
                    DayHeaderTimeline(dayData = day, destination = timelineData.destination)
                }

                if (day.activities.isEmpty()) {
                    item(key = "empty_${day.date}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_activities_day),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(day.activities, key = { it.activity.id }) { activityWithStatus ->
                        TimelineActivityItem(
                            activityWithStatus = activityWithStatus,
                            isLast = activityWithStatus == day.activities.last()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayHeaderTimeline(
    dayData: ActivitiesViewModel.DayData,
    destination: String
) {
    val formattedDate = remember(dayData.date) {
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayData.date)
            SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                .format(parsed!!)
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            dayData.date
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Dia ${dayData.dayNumber} de ${dayData.totalDays} en $destination",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = dayData.progressPercentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${dayData.completedCount} de ${dayData.totalCount} actividades completadas (${dayData.progressPercentage}%)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TimelineActivityItem(
    activityWithStatus: ActivitiesViewModel.ActivityWithStatus,
    isLast: Boolean
) {
    val activity = activityWithStatus.activity
    val status = activityWithStatus.status
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline indicator column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = when (status) {
                            ActivitiesViewModel.ActivityStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            ActivitiesViewModel.ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                            ActivitiesViewModel.ActivityStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    ActivitiesViewModel.ActivityStatus.COMPLETED -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    ActivitiesViewModel.ActivityStatus.IN_PROGRESS -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.onTertiary, CircleShape)
                        )
                    }
                    ActivitiesViewModel.ActivityStatus.PENDING -> {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Activity content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 8.dp else 0.dp)
        ) {
            Text(
                text = activity.time.ifBlank { "Sin hora" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (activity.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (activity.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val encodedQuery = Uri.encode(activity.location)
                        uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                    }
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activity.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

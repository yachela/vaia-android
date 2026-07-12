package com.vaia.presentation.ui.roadmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.domain.model.Activity
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.formatDateForDisplay
import com.vaia.presentation.ui.common.formatTimeForDisplay
import com.vaia.presentation.ui.common.normalizeDateForApi
import com.vaia.presentation.ui.common.normalizeTimeForApi
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SalmonOrange
import com.vaia.presentation.viewmodel.ActivitiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    onNavigateCurrency: () -> Unit = {},
    viewModel: ActivitiesViewModel
) {
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sortedActivities = sortActivitiesForRoadmap(activities)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.roadmap_trip)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    currentRoute = "trips", // O la ruta que corresponda
                    onHome = onNavigateHome,
                    onMap = onNavigateOrganizer,
                    onTrips = onNavigateTrips,
                    onCalendar = onNavigateCalendar,
                    onCurrency = onNavigateCurrency
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading && sortedActivities.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null && sortedActivities.isEmpty() -> {
                    Text(
                        text = error ?: stringResource(R.string.unknown_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                sortedActivities.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.empty_roadmap),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding() + 16.dp,
                            bottom = 100.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            val trip by viewModel.trip.collectAsState()
                            val origin = sortedActivities.firstOrNull()?.location?.ifBlank { stringResource(R.string.origin_label) }
                                ?: stringResource(R.string.origin_label)
                            val destination = sortedActivities.lastOrNull()?.location?.ifBlank { stringResource(R.string.destination_label) }
                                ?: stringResource(R.string.destination_label)
                            TripDetailHeader(
                                origin = origin,
                                destination = destination,
                                activitiesCount = sortedActivities.size,
                                startDate = trip?.startDate ?: "",
                                endDate = trip?.endDate ?: ""
                            )
                        }
                        item {
                            TripRoadmapCanvas(activities = sortedActivities)
                        }
                        itemsIndexed(sortedActivities) { index, activity ->
                            ActivityRoadmapStop(
                                index = index,
                                total = sortedActivities.size,
                                activity = activity
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripDetailHeader(
    origin: String,
    destination: String,
    activitiesCount: Int,
    startDate: String = "",
    endDate: String = ""
) {
    val progressTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val progressLineColor = MaterialTheme.colorScheme.tertiary

    // Calcular progreso real basado en fechas
    val progress = remember(startDate, endDate) {
        try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val start = fmt.parse(startDate.take(10))?.time ?: return@remember 0f
            val end = fmt.parse(endDate.take(10))?.time ?: return@remember 0f
            val now = System.currentTimeMillis()
            when {
                now <= start -> 0f
                now >= end -> 1f
                else -> ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
            }
        } catch (_: Exception) { 0f }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.route_trip), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.route_from_to, origin, destination),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (startDate.isNotBlank() && endDate.isNotBlank()) {
                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val displayFmt = java.text.SimpleDateFormat("d MMM", java.util.Locale("es", "ES"))
                    val startStr = try { displayFmt.format(fmt.parse(startDate.take(10))!!) } catch (_: Exception) { startDate }
                    val endStr = try { displayFmt.format(fmt.parse(endDate.take(10))!!) } catch (_: Exception) { endDate }
                    Text("$startStr - $endStr", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(stringResource(R.string.month_may_date), style = MaterialTheme.typography.bodySmall)
                }
                Text("•")
                Text(stringResource(R.string.stops_count, activitiesCount), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(14.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                val totalWidth = constraints.maxWidth.toFloat()
                val iconHalfWidth = 24f  // ~24dp en px aprox, para centrar el icono
                val trackStart = 20f
                val trackEnd = totalWidth - 20f
                val busX = trackStart + (trackEnd - trackStart) * progress

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height / 2f
                    // Track completo
                    drawLine(
                        color = progressTrackColor,
                        start = Offset(trackStart, y),
                        end = Offset(trackEnd, y),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    // Progreso recorrido
                    if (progress > 0f) {
                        drawLine(
                            color = progressLineColor,
                            start = Offset(trackStart, y),
                            end = Offset(busX, y),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                // Ícono del bus posicionado dinámicamente
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = stringResource(R.string.route),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(start = with(androidx.compose.ui.platform.LocalDensity.current) {
                            (busX - iconHalfWidth).coerceAtLeast(0f).toDp()
                        })
                )
            }
        }
    }
}

private enum class RoadmapActivityStatus {
    COMPLETED,
    IN_PROGRESS,
    PENDING
}

private fun getActivityStatus(activity: Activity): RoadmapActivityStatus {
    val now = java.util.Calendar.getInstance()
    val activityTime = try {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val dateTimeStr = "${activity.date} ${activity.time.ifBlank { "00:00" }}"
        val parsedDate = dateFormat.parse(dateTimeStr)
        java.util.Calendar.getInstance().apply { time = parsedDate!! }
    } catch (_: Exception) {
        null
    } ?: return RoadmapActivityStatus.PENDING

    val diffMinutes = (now.timeInMillis - activityTime.timeInMillis) / (1000 * 60)
    return when {
        activityTime.before(now) && diffMinutes > 120 -> RoadmapActivityStatus.COMPLETED
        diffMinutes in 0..120 -> RoadmapActivityStatus.IN_PROGRESS
        else -> RoadmapActivityStatus.PENDING
    }
}

@Composable
private fun TripRoadmapCanvas(activities: List<Activity>) {
    val widthDp = (activities.size * 96).coerceAtLeast(360)
    val inactiveLineColor = MaterialTheme.colorScheme.outlineVariant
    val activeLineColor = SalmonOrange
    val completedColor = SalmonOrange
    val pendingColor = MaterialTheme.colorScheme.outlineVariant

    val backgroundColor = MaterialTheme.colorScheme.background

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.visual_route),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Canvas(
                    modifier = Modifier
                        .width(widthDp.dp)
                        .height(96.dp)
                ) {
                    val spacing = (size.width - 64f) / (activities.size - 1).coerceAtLeast(1)
                    val points = activities.indices.map { index ->
                        val x = 32f + (index * spacing)
                        val y = size.height / 2f
                        Offset(x, y)
                    }

                    if (points.isNotEmpty()) {
                        // 1. Dibujar línea inactiva de fondo
                        drawLine(
                            color = inactiveLineColor,
                            start = points.first(),
                            end = points.last(),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )

                        // 2. Calcular hasta qué índice está completado o en curso
                        val lastActiveIndex = activities.indexOfLast { 
                            getActivityStatus(it) != RoadmapActivityStatus.PENDING 
                        }

                        // 3. Dibujar línea activa (SalmonOrange) hasta el último punto activo
                        if (lastActiveIndex > 0) {
                            drawLine(
                                color = activeLineColor,
                                start = points.first(),
                                end = points[lastActiveIndex],
                                strokeWidth = 6f,
                                cap = StrokeCap.Round
                            )
                        }

                        // 4. Dibujar círculos para cada punto
                        activities.forEachIndexed { index, activity ->
                            val status = getActivityStatus(activity)
                            val point = points[index]
                            
                            val circleColor = when (status) {
                                RoadmapActivityStatus.COMPLETED -> completedColor
                                RoadmapActivityStatus.IN_PROGRESS -> completedColor
                                RoadmapActivityStatus.PENDING -> pendingColor
                            }

                            drawCircle(
                                color = circleColor,
                                radius = 10f,
                                center = point
                            )
                            drawCircle(
                                color = if (status == RoadmapActivityStatus.IN_PROGRESS) SalmonOrange else backgroundColor,
                                radius = if (status == RoadmapActivityStatus.IN_PROGRESS) 6f else 4f,
                                center = point
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRoadmapStop(
    index: Int,
    total: Int,
    activity: Activity
) {
    val status = remember(activity) { getActivityStatus(activity) }
    val timelineColor = when (status) {
        RoadmapActivityStatus.COMPLETED -> SalmonOrange
        RoadmapActivityStatus.IN_PROGRESS -> SalmonOrange
        RoadmapActivityStatus.PENDING -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    }

    val pendingCircleColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Canvas(modifier = Modifier.height(20.dp)) {
                val circleColor = when (status) {
                    RoadmapActivityStatus.COMPLETED -> SalmonOrange
                    RoadmapActivityStatus.IN_PROGRESS -> SalmonOrange
                    RoadmapActivityStatus.PENDING -> pendingCircleColor
                }
                drawCircle(color = circleColor, radius = 7f, center = Offset(size.width / 2f, 10f))
                if (status == RoadmapActivityStatus.IN_PROGRESS) {
                    drawCircle(color = Color.White, radius = 3f, center = Offset(size.width / 2f, 10f))
                }
            }
            if (index < total - 1) {
                Canvas(modifier = Modifier.height(78.dp)) {
                    drawLine(
                        color = timelineColor,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 3f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (status) {
                    RoadmapActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        activity.title, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Badge de estado
                    val statusText = when (status) {
                        RoadmapActivityStatus.COMPLETED -> "Realizado"
                        RoadmapActivityStatus.IN_PROGRESS -> "En curso"
                        RoadmapActivityStatus.PENDING -> "Pendiente"
                    }
                    val statusBgColor = when (status) {
                        RoadmapActivityStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                        RoadmapActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                        RoadmapActivityStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val statusTextColor = when (status) {
                        RoadmapActivityStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
                        RoadmapActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
                        RoadmapActivityStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusBgColor,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${formatDateForDisplay(activity.date)} ${formatTimeForDisplay(activity.time)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.exclusive_tour),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text("$${activity.cost}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                if (activity.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.location_prefix, activity.location), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun sortActivitiesForRoadmap(activities: List<Activity>): List<Activity> {
    return activities.sortedWith(
        compareBy<Activity>(
            { normalizeDateForApi(it.date) ?: "9999-99-99" },
            { normalizeTimeForApi(it.time) ?: "99:99" },
            { it.title.lowercase() }
        )
    )
}

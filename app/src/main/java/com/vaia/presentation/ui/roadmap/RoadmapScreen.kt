package com.vaia.presentation.ui.roadmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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
            AppQuickBar(
                currentRoute = "trips",
                onHome = onNavigateHome,
                onTrips = onNavigateTrips,
                onProfile = onNavigateProfile
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            val origin = sortedActivities.firstOrNull()?.location?.ifBlank { "Origen" } ?: "Origen"
                            val destination = sortedActivities.lastOrNull()?.location?.ifBlank { "Destino" } ?: "Destino"
                            TripDetailHeader(origin = origin, destination = destination, activitiesCount = sortedActivities.size)
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
    activitiesCount: Int
) {
    val progressTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.route_trip), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$origin a $destination", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.month_may_date), style = MaterialTheme.typography.bodySmall)
                Text("•")
                Text(stringResource(R.string.stops_count, activitiesCount), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height / 2f
                    drawLine(
                        color = progressTrackColor,
                        start = Offset(20f, y),
                        end = Offset(size.width - 20f, y),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = MintPrimary,
                        start = Offset(20f, y),
                        end = Offset(size.width * 0.62f, y),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun TripRoadmapCanvas(activities: List<Activity>) {
    val density = LocalDensity.current
    val nodeSpacingPx = with(density) { 120.dp.toPx() }
    val widthDp = (activities.size * 120).coerceAtLeast(360)
    val backgroundColor = MaterialTheme.colorScheme.background
    val onSurfaceMuted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

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
                        .height(210.dp)
                ) {
                    val points = activities.indices.map { index ->
                        val x = 40f + (index * nodeSpacingPx)
                        val y = if (index % 2 == 0) 70f else 145f
                        Offset(x, y)
                    }
                    if (points.isNotEmpty()) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val current = points[i]
                                val control = Offset((prev.x + current.x) / 2f, (prev.y + current.y) / 2f - 24f)
                                quadraticBezierTo(control.x, control.y, current.x, current.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = SalmonOrange,
                            style = Stroke(width = 8f, cap = StrokeCap.Round)
                        )
                        points.forEachIndexed { index, point ->
                            drawCircle(color = backgroundColor, radius = 16f, center = point)
                            drawCircle(color = SalmonOrange, radius = 11f, center = point)
                            drawCircle(color = backgroundColor, radius = 3f, center = point)
                            val dotY = if (index % 2 == 0) point.y + 34f else point.y - 34f
                            drawCircle(color = onSurfaceMuted, radius = 2f, center = Offset(point.x, dotY))
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
    val timelineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Canvas(modifier = Modifier.height(20.dp)) {
                drawCircle(color = SalmonOrange, radius = 7f, center = Offset(size.width / 2f, 10f))
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(activity.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                        color = MaterialTheme.colorScheme.primary
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

package com.vaia.presentation.ui.roadmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    onCurrency = {}
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
                            val origin = sortedActivities.firstOrNull()?.location?.ifBlank { stringResource(R.string.origin_label) }
                                ?: stringResource(R.string.origin_label)
                            val destination = sortedActivities.lastOrNull()?.location?.ifBlank { stringResource(R.string.destination_label) }
                                ?: stringResource(R.string.destination_label)
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
    val progressLineColor = MaterialTheme.colorScheme.tertiary
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
                        color = progressLineColor,
                        start = Offset(20f, y),
                        end = Offset(size.width * 0.62f, y),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun TripRoadmapCanvas(activities: List<Activity>) {
    val widthDp = (activities.size * 96).coerceAtLeast(360)
    val lineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
    val pointColor = MaterialTheme.colorScheme.tertiary
    val innerPointColor = MaterialTheme.colorScheme.background

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
                        drawLine(
                            color = lineColor,
                            start = points.first(),
                            end = points.last(),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        points.forEach { point ->
                            drawCircle(
                                color = pointColor,
                                radius = 10f,
                                center = point
                            )
                            drawCircle(
                                color = innerPointColor,
                                radius = 4f,
                                center = point
                            )
                        }
                        drawLine(
                            color = SalmonOrange,
                            start = points.first(),
                            end = points.last(),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
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

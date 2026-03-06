package com.vaia.presentation.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.domain.model.Trip
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.formatDateForDisplay
import com.vaia.presentation.ui.common.normalizeDateForApi
import com.vaia.presentation.ui.theme.ErrorRed
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SkyBackground
import com.vaia.presentation.ui.theme.SunAccent
import com.vaia.presentation.viewmodel.TripsViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToActivities: (String) -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateOrganizer: () -> Unit,
    viewModel: TripsViewModel
) {
    val trips by viewModel.trips.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadTrips() }

    val today = remember { LocalDate.now().toString() }
    val featuredTrip = remember(trips, today) {
        val active = trips.firstOrNull { trip ->
            val start = normalizeDateForApi(trip.startDate) ?: ""
            val end = normalizeDateForApi(trip.endDate) ?: ""
            start <= today && end >= today
        }
        active ?: trips
            .filter { (normalizeDateForApi(it.startDate) ?: "") > today }
            .minByOrNull { normalizeDateForApi(it.startDate) ?: it.startDate }
    }
    val otherTrips = remember(trips, featuredTrip) {
        trips.filter { it.id != featuredTrip?.id }.take(5)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
        bottomBar = {
            AppQuickBar(
                currentRoute = "home",
                onHome = {},
                onTrips = onNavigateTrips,
                onProfile = onNavigateProfile,
                onCalendar = onNavigateCalendar,
                onMap = onNavigateOrganizer
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(SkyBackground.copy(alpha = 0.6f), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(padding)
        ) {
            when {
                isLoading && trips.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.home_summary_title),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.home_summary_subtitle, trips.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (featuredTrip != null) {
                            item(key = "featured_${featuredTrip.id}") {
                                FeaturedTripCard(
                                    trip = featuredTrip,
                                    isActive = (normalizeDateForApi(featuredTrip.startDate) ?: "") <= today &&
                                            (normalizeDateForApi(featuredTrip.endDate) ?: "") >= today,
                                    today = today,
                                    onClick = { onNavigateToActivities(featuredTrip.id) }
                                )
                            }
                        } else if (!isLoading) {
                            item {
                                EmptyHomeCard(onNavigateTrips = onNavigateTrips)
                            }
                        }

                        if (otherTrips.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.trips),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            items(otherTrips, key = { it.id }) { trip ->
                                CompactTripCard(
                                    trip = trip,
                                    onClick = { onNavigateToActivities(trip.id) }
                                )
                            }
                        }

                        if (trips.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.home_go_to_trips),
                                    modifier = Modifier.clickable(onClick = onNavigateTrips),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedTripCard(
    trip: Trip,
    isActive: Boolean,
    today: String,
    onClick: () -> Unit
) {
    val daysRemaining = remember(trip.endDate, today) {
        try {
            val end = LocalDate.parse(normalizeDateForApi(trip.endDate) ?: "")
            ChronoUnit.DAYS.between(LocalDate.now(), end).coerceAtLeast(0)
        } catch (_: Exception) { 0L }
    }
    val spentRatio = if (trip.budget > 0) {
        (trip.totalExpenses / trip.budget).toFloat().coerceIn(0f, 1f)
    } else 0f
    val barColor = when {
        spentRatio >= 1f -> ErrorRed
        spentRatio >= 0.7f -> SunAccent
        else -> MintPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isActive) MintPrimary.copy(alpha = 0.2f) else SunAccent.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isActive) stringResource(R.string.trip_active_label)
                               else stringResource(R.string.trip_upcoming_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isActive) MintPrimary else SunAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = trip.destination,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formatDateForDisplay(trip.startDate)} – ${formatDateForDisplay(trip.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeStat(label = stringResource(R.string.activities), value = trip.activitiesCount.toString())
                HomeStat(label = stringResource(R.string.expenses), value = trip.expensesCount.toString())
                HomeStat(
                    label = stringResource(R.string.days_remaining, daysRemaining),
                    value = daysRemaining.toString(),
                    valueColor = if (daysRemaining <= 3L) ErrorRed else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Budget progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.budget_spent_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$${trip.totalExpenses.toInt()} / $${trip.budget.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = spentRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SunAccent, contentColor = Color.Black)
            ) {
                Text(
                    stringResource(R.string.view_trip),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun HomeStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactTripCard(trip: Trip, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trip.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    trip.destination,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                formatDateForDisplay(trip.startDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyHomeCard(onNavigateTrips: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.home_no_trips),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_no_trips_cta),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateTrips,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SunAccent, contentColor = Color.Black)
            ) {
                Text(
                    stringResource(R.string.create_first_trip),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

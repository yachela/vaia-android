package com.vaia.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vaia.R

@Composable
fun AppQuickBar(
    currentRoute: String,
    onHome: () -> Unit,
    onTrips: () -> Unit,
    onProfile: () -> Unit,
    onCalendar: () -> Unit = {},
    onMap: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    fun tap(action: () -> Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        NavigationBar(
            modifier = Modifier
                .background(Color.Transparent),
            containerColor = Color.Transparent
        ) {
            val colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationBarItem(
                selected = currentRoute == "home",
                onClick = { tap(onHome) },
                colors = colors,
                icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home)) }
            )
            NavigationBarItem(
                selected = currentRoute == "calendar",
                onClick = { tap(onCalendar) },
                colors = colors,
                icon = { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.calendar)) }
            )
            NavigationBarItem(
                selected = currentRoute == "trips",
                onClick = { tap(onTrips) },
                colors = colors,
                icon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.trips)) }
            )
            NavigationBarItem(
                selected = currentRoute == "organizer",
                onClick = { tap(onMap) },
                colors = colors,
                icon = { Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.organizer_tab)) }
            )
            NavigationBarItem(
                selected = currentRoute == "profile",
                onClick = { tap(onProfile) },
                colors = colors,
                icon = { Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile_title)) }
            )
        }
    }
}

package com.vaia.presentation.ui.activities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.domain.model.ActivitySuggestion
import com.vaia.presentation.ui.theme.ErrorRed
import com.vaia.presentation.ui.theme.MintPrimary
import kotlin.math.roundToInt

@Composable
fun SwipeableSuggestionCard(
    suggestion: ActivitySuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val swipeThreshold = with(density) { 100.dp.toPx() }

    var offsetX by remember { mutableStateOf(0f) }
    var isExiting by remember { mutableStateOf(false) }
    var hapticFiredAt by remember { mutableStateOf(0) } // 0=none, 1=accept zone, -1=dismiss zone
    val alpha = remember { Animatable(1f) }
    
    val backgroundColor = when {
        offsetX > swipeThreshold / 2 -> MintPrimary.copy(alpha = 0.3f)
        offsetX < -swipeThreshold / 2 -> ErrorRed.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val iconColor = when {
        offsetX > swipeThreshold / 2 -> MintPrimary
        offsetX < -swipeThreshold / 2 -> ErrorRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val showAcceptIcon = offsetX > swipeThreshold / 4
    val showDismissIcon = offsetX < -swipeThreshold / 4
    
    LaunchedEffect(isExiting) {
        if (isExiting) {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }
    
    AnimatedVisibility(
        visible = !isExiting,
        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundColor, RoundedCornerShape(16.dp)),
                contentAlignment = if (offsetX > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showAcceptIcon) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.accept),
                            tint = MintPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    if (showDismissIcon) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.dismiss_suggestion),
                                tint = ErrorRed,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                    }
                }
            }
            
            Card(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .pointerInput(suggestion) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    offsetX > swipeThreshold -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isExiting = true
                                        onAccept()
                                    }
                                    offsetX < -swipeThreshold -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isExiting = true
                                        onDismiss()
                                    }
                                    else -> {
                                        offsetX = 0f
                                        hapticFiredAt = 0
                                    }
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                offsetX = (offsetX + dragAmount).coerceIn(-300f, 300f)
                                // Vibración al entrar en zona de acción
                                val zone = when {
                                    offsetX > swipeThreshold / 2 -> 1
                                    offsetX < -swipeThreshold / 2 -> -1
                                    else -> 0
                                }
                                if (zone != 0 && zone != hapticFiredAt) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    hapticFiredAt = zone
                                } else if (zone == 0) {
                                    hapticFiredAt = 0
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = suggestion.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.swipe_right_accept),
                                style = MaterialTheme.typography.labelSmall,
                                color = MintPrimary
                            )
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = suggestion.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = suggestion.location,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = suggestion.time,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (suggestion.cost > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${suggestion.cost.toInt()} USD",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.swipe_left_dismiss),
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed.copy(alpha = 0.7f)
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = ErrorRed.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySuggestionsMessage(
    onRequestNewSuggestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Celebration,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = stringResource(R.string.no_more_suggestions_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            text = stringResource(R.string.no_more_suggestions_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

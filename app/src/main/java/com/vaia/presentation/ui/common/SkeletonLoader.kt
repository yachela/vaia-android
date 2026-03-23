package com.vaia.presentation.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun shimmerColor(): Color {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )
    return MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerColor())
    )
}

@Composable
fun TripCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cover image placeholder
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 160.dp, cornerRadius = 22.dp)
        // Title
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f), height = 20.dp)
        // Destination row
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f), height = 14.dp)
        // Chips row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(modifier = Modifier.width(120.dp), height = 32.dp, cornerRadius = 16.dp)
            SkeletonBox(modifier = Modifier.width(80.dp), height = 32.dp, cornerRadius = 16.dp)
        }
        // Stats bar
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 52.dp, cornerRadius = 16.dp)
    }
}

@Composable
fun ActivityCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.55f), height = 18.dp)
            SkeletonBox(modifier = Modifier.width(60.dp), height = 18.dp)
        }
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f), height = 13.dp)
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.45f), height = 13.dp)
    }
}

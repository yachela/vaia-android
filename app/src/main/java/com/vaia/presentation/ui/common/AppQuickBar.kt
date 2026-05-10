package com.vaia.presentation.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp

@Composable
fun AppQuickBar(
    currentRoute: String,
    onHome: () -> Unit,
    onMap: () -> Unit,
    onTrips: () -> Unit,
    onCalendar: () -> Unit,
    onCurrency: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "vaia_anim")

    val containerColor = Color(0xFF1E1E1E)
    val activeIconColor = Color(0xFF308ADD)
    val activeIconColorCenter = Color(0xFFEAF3FC)
    val inactiveIconColor = Color(0xFF4C4D50)

    val cutoutWidth = with(density) { 95.dp.toPx() }
    val cutoutDepth = with(density) { 55.dp.toPx() }

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "aura_rot"
    )

    val fullWidthUShape = GenericShape { size: Size, _ ->
        val w = size.width
        val h = size.height
        val center = w / 2
        reset()
        moveTo(0f, 0f)
        lineTo(center - cutoutWidth / 1.5f, 0f)
        cubicTo(center - cutoutWidth / 2.5f, 0f, center - cutoutWidth / 2.5f, cutoutDepth, center, cutoutDepth)
        cubicTo(center + cutoutWidth / 2.5f, cutoutDepth, center + cutoutWidth / 2.5f, 0f, center + cutoutWidth / 1.5f, 0f)
        lineTo(w, 0f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. FONDO DE LA BARRA (Gris Oscuro)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(fullWidthUShape) // Aplicamos la forma aquí
                .background(containerColor) // El color gris oscuro
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CASA
                IconButton(onClick = onHome) {
                    Icon(Icons.Default.Home, null, tint = if(currentRoute=="home") activeIconColor else inactiveIconColor)
                }
                // MAPA
                IconButton(onClick = onMap) {
                    Icon(Icons.Default.Map, null, tint = if(currentRoute=="map") activeIconColor else inactiveIconColor)
                }

                Spacer(modifier = Modifier.width(85.dp)) // Espacio para la maleta

                // CALENDARIO
                IconButton(onClick = onCalendar) {
                    Icon(Icons.Default.DateRange, null, tint = if(currentRoute=="calendar") activeIconColor else inactiveIconColor)
                }
                // MONEDA
                IconButton(onClick = onCurrency) {
                    Icon(Icons.Default.Payments, null, tint = if(currentRoute=="currency") activeIconColor else inactiveIconColor)
                }
            }
        }

        // 2. BOTÓN CENTRAL: LA MALETA
        Box(
            modifier = Modifier.padding(bottom = 22.dp).size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Aura Celeste
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation)
                    .background(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, activeIconColor, Color.Transparent, activeIconColor, Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            // Centro del botón (Gris) con el icono de Maleta
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(containerColor)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTrips()
                    },
                contentAlignment = Alignment.Center
            ) {
                val isTripsActive = currentRoute == "trips"

                Icon(
                    imageVector = Icons.Default.BusinessCenter,
                    contentDescription = null,
                    // Aplicamos la lógica semántica:
                    // Si está activa es Celeste (primary), si no, es Blanca (onSurface).
                    tint = if (isTripsActive) activeIconColor else  activeIconColorCenter,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

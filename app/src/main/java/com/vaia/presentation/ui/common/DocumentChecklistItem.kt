package com.vaia.presentation.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaia.R
import com.vaia.presentation.ui.theme.InkBlack
import com.vaia.presentation.ui.theme.InkMuted
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.MintSecondary
import com.vaia.presentation.ui.theme.SuccessGreen

@Composable
fun DocumentCategoryItem(
    category: String,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) MintSecondary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "background"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isCompleted) SuccessGreen else InkMuted,
        animationSpec = tween(300),
        label = "icon"
    )

    val textColor by animateColorAsState(
        targetValue = if (isCompleted) InkBlack else InkMuted,
        animationSpec = tween(300),
        label = "text"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isCompleted) SuccessGreen.copy(alpha = 0.2f) else InkMuted.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getCategoryLabel(category),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
            Text(
                text = getCategoryDescription(category),
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted
            )
        }

        if (isCompleted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Completado",
                tint = SuccessGreen,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(InkMuted.copy(alpha = 0.2f))
            )
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "flight" -> Icons.Filled.Flight
        "accommodation" -> Icons.Filled.Hotel
        "id" -> Icons.Filled.ConfirmationNumber
        "insurance" -> Icons.Filled.Security
        "tours" -> Icons.Filled.LocalActivity
        "receipts" -> Icons.Filled.Receipt
        "photos" -> Icons.Filled.PhotoCamera
        else -> Icons.Filled.Description
    }
}

fun getCategoryLabel(category: String): String {
    return when (category) {
        "flight" -> "Boletos"
        "accommodation" -> "Hotel/Alojamiento"
        "id" -> "Identificación"
        "insurance" -> "Seguro"
        "tours" -> "Tours"
        "receipts" -> "Recibos"
        "photos" -> "Fotos"
        else -> "Otros"
    }
}

fun getCategoryDescription(category: String): String {
    return when (category) {
        "flight" -> "Boletos de avión, tren o bus"
        "accommodation" -> "Reservas de hotel o Airbnb"
        "id" -> "Pasaporte, DNI o visa"
        "insurance" -> "Póliza de seguro de viaje"
        "tours" -> "Confirmaciones de tours"
        "receipts" -> "Recibos de gastos"
        "photos" -> "Fotos del viaje"
        else -> "Documentos varios"
    }
}

val documentCategories = listOf(
    "flight",
    "accommodation", 
    "id",
    "insurance",
    "tours",
    "receipts",
    "photos",
    "other"
)

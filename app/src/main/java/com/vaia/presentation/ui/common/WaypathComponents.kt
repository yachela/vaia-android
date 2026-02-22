package com.vaia.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaia.presentation.ui.theme.InkBlack
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SurfaceWhite

@Composable
fun WaypathCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = { content() }
    )
}

@Composable
fun WaypathBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MintPrimary.copy(alpha = 0.25f),
    textColor: Color = InkBlack
) {
    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = textColor, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun WaypathButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true
) {
    val container = if (primary) MintPrimary else SurfaceWhite
    val content = InkBlack
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 54.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (primary) 2.dp else 0.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

package com.vaia.presentation.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SalmonOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputOverlay(
    helper: VoiceInputHelper,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val state by helper.state.collectAsState()
    val rmsDb by helper.rmsDb.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera del bottom sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Asistente de Voz",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is VoiceInputHelper.State.Idle -> {
                    Text(
                        text = "Presiona el botón para empezar a hablar",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    IconButton(
                        onClick = { helper.startListening() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MintPrimary)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Iniciar micrófono", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                is VoiceInputHelper.State.Listening -> {
                    Text(
                        text = "Escuchando tu viaje...",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = MintPrimary),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Dinos destino, tipo de viaje, presupuesto y fechas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animación del ecualizador/onda de sonido
                    SoundWaveEqualizer(rmsDb = rmsDb)

                    Spacer(modifier = Modifier.height(16.dp))

                    IconButton(
                        onClick = { helper.stopListening() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SalmonOrange)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Detener micrófono", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                is VoiceInputHelper.State.Processing -> {
                    Text(
                        text = "Procesando audio localmente...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = MintPrimary, modifier = Modifier.size(48.dp))
                }

                is VoiceInputHelper.State.Success -> {
                    val text = (state as VoiceInputHelper.State.Success).text
                    Text(
                        text = "Entendido:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "\"$text\"",
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { helper.startListening() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reintentar")
                        }
                        Button(
                            onClick = { onSuccess(text) },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Usar Texto")
                        }
                    }
                }

                is VoiceInputHelper.State.Error -> {
                    val message = (state as VoiceInputHelper.State.Error).message
                    Text(
                        text = "Ocurrió un inconveniente",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SalmonOrange),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { helper.startListening() },
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
                    ) {
                        Text("Intentar de nuevo")
                    }
                }
            }
        }
    }
}

@Composable
fun SoundWaveEqualizer(rmsDb: Float) {
    val barHeights = remember { listOf(0.3f, 0.6f, 1.0f, 0.7f, 0.4f) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(80.dp)
    ) {
        barHeights.forEach { baseHeight ->
            // Escalar de forma limpia usando rmsDb del micrófono nativo
            val dbScale = (rmsDb + 2f).coerceAtLeast(0f)
            val animatedHeight by animateDpAsState(
                targetValue = (12 + (dbScale * baseHeight * 7)).dp,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
                label = "soundBarHeight"
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(MintPrimary, MintPrimary.copy(alpha = 0.6f))
                        )
                    )
            )
        }
    }
}

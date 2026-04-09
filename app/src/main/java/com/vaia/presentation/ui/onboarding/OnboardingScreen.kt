package com.vaia.presentation.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaia.presentation.ui.theme.BluePrimary
import com.vaia.presentation.ui.theme.BlueLight

data class OnboardingPreferences(
    val travelerType: String = "",      // "aventurero", "cultural", "relajado", "gastronomico"
    val currency: String = "USD",       // "USD", "EUR", "ARS", "MXN", etc.
    val groupType: String = ""          // "solo", "pareja", "familia", "amigos"
)

@Composable
fun OnboardingScreen(
    userName: String = "",
    onFinish: (OnboardingPreferences) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var prefs by remember { mutableStateOf(OnboardingPreferences()) }
    val totalSteps = 3

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BluePrimary,
                        MaterialTheme.colorScheme.background
                    ),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Indicador de pasos
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (index == currentStep) 32.dp else 16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index <= currentStep) Color.White
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Contenido animado por paso
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "onboarding-step"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(userName = userName)
                    1 -> TravelerTypeStep(
                        selected = prefs.travelerType,
                        onSelect = { prefs = prefs.copy(travelerType = it) }
                    )
                    2 -> GroupAndCurrencyStep(
                        selectedGroup = prefs.groupType,
                        selectedCurrency = prefs.currency,
                        onGroupSelect = { prefs = prefs.copy(groupType = it) },
                        onCurrencySelect = { prefs = prefs.copy(currency = it) }
                    )
                }
            }

            // Botones de navegación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Atrás", color = Color.White.copy(alpha = 0.8f))
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            onFinish(prefs)
                        }
                    },
                    enabled = when (currentStep) {
                        1 -> prefs.travelerType.isNotBlank()
                        2 -> prefs.groupType.isNotBlank()
                        else -> true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = BluePrimary,
                        disabledContainerColor = Color.White.copy(alpha = 0.4f),
                        disabledContentColor = BluePrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        if (currentStep < totalSteps - 1) "Siguiente" else "¡Empezar!",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomeStep(userName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Flight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (userName.isNotBlank()) "¡Hola, $userName!" else "¡Bienvenido a VAIA!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tu asistente de viajes con IA. Vamos a personalizar tu experiencia.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.15f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                OnboardingFeatureRow(Icons.Default.Map, "Itinerarios personalizados con IA")
                Spacer(Modifier.height(12.dp))
                OnboardingFeatureRow(Icons.Default.WbCloudy, "Lista de equipaje según el clima real")
                Spacer(Modifier.height(12.dp))
                OnboardingFeatureRow(Icons.Default.AccountBalanceWallet, "Control de gastos por viaje")
                Spacer(Modifier.height(12.dp))
                OnboardingFeatureRow(Icons.Default.Assignment, "Checklist de documentos de viaje")
            }
        }
    }
}

@Composable
private fun OnboardingFeatureRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun TravelerTypeStep(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        Triple("aventurero", Icons.Default.Landscape, "Aventurero\nNaturaleza y adrenalina"),
        Triple("cultural", Icons.Default.AccountBalance, "Cultural\nHistoria, arte y museos"),
        Triple("relajado", Icons.Default.BeachAccess, "Relajado\nPlayas y descanso"),
        Triple("gastronomico", Icons.Default.Restaurant, "Gastronómico\nComida y experiencias culinarias")
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "¿Cómo eres como viajero?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Esto nos ayuda a personalizar tu itinerario",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        options.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                row.forEach { (key, icon, label) ->
                    OnboardingOptionCard(
                        icon = icon,
                        label = label,
                        selected = selected == key,
                        onClick = { onSelect(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GroupAndCurrencyStep(
    selectedGroup: String,
    selectedCurrency: String,
    onGroupSelect: (String) -> Unit,
    onCurrencySelect: (String) -> Unit
) {
    val groups = listOf(
        Triple("solo", Icons.Default.Luggage, "Solo"),
        Triple("pareja", Icons.Default.Favorite, "En pareja"),
        Triple("familia", Icons.Default.Group, "Familia"),
        Triple("amigos", Icons.Default.People, "Amigos")
    )
    val currencies = listOf("USD", "EUR", "ARS", "MXN", "CLP", "COP", "BRL")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "¿Con quién viajás?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            groups.forEach { (key, icon, label) ->
                OnboardingOptionCard(
                    icon = icon,
                    label = label,
                    selected = selectedGroup == key,
                    onClick = { onGroupSelect(key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Moneda preferida",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))

        // Grid de monedas
        currencies.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                row.forEach { currency ->
                    Surface(
                        onClick = { onCurrencySelect(currency) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedCurrency == currency)
                            Color.White
                        else
                            Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = currency,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedCurrency == currency) BluePrimary else Color.White
                        )
                    }
                }
                if (row.size < 4) {
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun OnboardingOptionCard(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color.White else Color.White.copy(alpha = 0.15f),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(2.dp, Color.White)
        else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Box {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(BluePrimary, CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(10.dp)
                        )
                    }
                }
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) BluePrimary else Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

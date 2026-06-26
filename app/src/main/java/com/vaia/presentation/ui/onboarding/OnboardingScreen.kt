package com.vaia.presentation.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.vaia.R
import com.vaia.presentation.ui.theme.BlueAccent
import com.vaia.presentation.ui.theme.BluePrimary
import com.vaia.presentation.ui.theme.BlueLight
import com.vaia.presentation.ui.theme.BlueDeep

data class OnboardingPreferences(
    val travelerTypes: Set<String> = emptySet(),
    val currency: String = "USD",
    val groupType: String = ""
)

@Composable
fun OnboardingScreen(
    userName: String = "",
    isDark: Boolean = false,
    onFinish: (OnboardingPreferences) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var prefs by remember { mutableStateOf(OnboardingPreferences()) }
    val totalSteps = 3
    var planeFlying by remember { mutableStateOf(false) }
    var planeFlyingRight by remember { mutableStateOf(false) }
    var isLanding by remember { mutableStateOf(false) }

    val planeOffsetY by animateFloatAsState(
        targetValue = if (planeFlying) -600f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutLinearInEasing),
        label = "planeOffset",
        finishedListener = {
            if (planeFlying) {
                currentStep = 1
                planeFlying = false
            }
        }
    )
    val planeAlpha by animateFloatAsState(
        targetValue = if (planeFlying) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "planeAlpha"
    )

    val planeOffsetX by animateFloatAsState(
        targetValue = if (planeFlyingRight) 1200f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutLinearInEasing),
        label = "planeOffsetX",
        finishedListener = {
            if (planeFlyingRight) {
                currentStep = 2
                planeFlyingRight = false
            }
        }
    )
    val planeAlphaRight by animateFloatAsState(
        targetValue = if (planeFlyingRight) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "planeAlphaRight"
    )

    val isDark = isDark

    val gradientColors = if (isDark) {
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimary,
            MaterialTheme.colorScheme.background
        )
    } else {
        listOf(BluePrimary, BlueAccent, BlueLight)
    }

    val waveColor = if (isDark)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    else
        Color.White.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(
                colors = gradientColors,
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val path1 = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.25f, h * 0.72f, w * 0.5f, h * 0.84f, w * 0.75f, h * 0.76f)
                cubicTo(w * 0.88f, h * 0.71f, w * 0.95f, h * 0.78f, w, h * 0.74f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path1, waveColor)

            val path2 = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h * 0.88f)
                cubicTo(w * 0.3f, h * 0.82f, w * 0.6f, h * 0.92f, w * 0.85f, h * 0.85f)
                cubicTo(w * 0.93f, h * 0.82f, w * 0.97f, h * 0.88f, w, h * 0.86f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path2, waveColor.copy(alpha = waveColor.alpha * 0.6f))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

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
                    0 -> WelcomeStep(
                        userName = userName,
                        planeOffsetY = planeOffsetY,
                        planeAlpha = planeAlpha
                    )
                    1 -> TravelerTypeStep(
                        selected = prefs.travelerTypes,
                        onSelect = { key ->
                            val current = prefs.travelerTypes
                            val updated = if (current.contains(key)) current - key
                                          else if (current.size < 3) current + key
                                          else current
                            prefs = prefs.copy(travelerTypes = updated)
                        },
                        planeOffsetX = planeOffsetX,
                        planeAlpha = planeAlphaRight
                    )
                    2 -> GroupAndCurrencyStep(
                        selectedGroup = prefs.groupType,
                        selectedCurrency = prefs.currency,
                        onGroupSelect = { prefs = prefs.copy(groupType = it) },
                        onCurrencySelect = { prefs = prefs.copy(currency = it) },
                        isLanding = isLanding,
                        onLandingFinished = { onFinish(prefs) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text(stringResource(R.string.onboarding_btn_back), color = Color.White.copy(alpha = 0.8f))
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep == 0) {
                            planeFlying = true
                        } else if (currentStep == 1) {
                            planeFlyingRight = true
                        } else if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            isLanding = true
                        }
                    },
                    enabled = when (currentStep) {
                        1 -> prefs.travelerTypes.isNotEmpty()
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
                        if (currentStep < totalSteps - 1) stringResource(R.string.onboarding_btn_next) else stringResource(R.string.onboarding_btn_start),
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
private fun WelcomeStep(
    userName: String,
    planeOffsetY: Float = 0f,
    planeAlpha: Float = 1f
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Flight,
            contentDescription = stringResource(R.string.onboarding_cd_plane),
            tint = Color.White.copy(alpha = planeAlpha),
            modifier = Modifier
                .size(100.dp)
                .offset(y = with(androidx.compose.ui.platform.LocalDensity.current) { planeOffsetY.toDp() })
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (userName.isNotBlank()) stringResource(R.string.onboarding_welcome_title_name, userName) else stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
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
                OnboardingFeatureRow(Icons.Default.Map, stringResource(R.string.onboarding_feature_itinerary))
                Spacer(Modifier.height(12.dp))
                OnboardingFeatureRow(Icons.Default.WbCloudy, stringResource(R.string.onboarding_feature_packing))
                Spacer(Modifier.height(12.dp))
                OnboardingFeatureRow(Icons.Default.AccountBalanceWallet, stringResource(R.string.onboarding_feature_expenses))
                Spacer(Modifier.height(12.dp))
                OnboardingFeatureRow(Icons.Default.Assignment, stringResource(R.string.onboarding_feature_checklist))
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
private fun TravelerTypeStep(
    selected: Set<String>,
    onSelect: (String) -> Unit,
    planeOffsetX: Float = 0f,
    planeAlpha: Float = 1f
) {
    val options = listOf(
        Triple("aventurero", Icons.Default.Landscape, stringResource(R.string.onboarding_traveler_aventurero)),
        Triple("cultural", Icons.Default.AccountBalance, stringResource(R.string.onboarding_traveler_cultural)),
        Triple("relajado", Icons.Default.BeachAccess, stringResource(R.string.onboarding_traveler_relajado)),
        Triple("gastronomico", Icons.Default.Restaurant, stringResource(R.string.onboarding_traveler_gastronomico)),
        Triple("mochilero", Icons.Default.Luggage, stringResource(R.string.onboarding_traveler_mochilero)),
        Triple("fotografo", Icons.Default.CameraAlt, stringResource(R.string.onboarding_traveler_fotografo)),
        Triple("naturaleza", Icons.Default.Park, stringResource(R.string.onboarding_traveler_naturaleza)),
        Triple("lujo", Icons.Default.Star, stringResource(R.string.onboarding_traveler_lujo))
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.onboarding_traveler_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.onboarding_traveler_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        options.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                row.forEach { (key, icon, label) ->
                    val isSelected = selected.contains(key)
                    val isDisabled = !isSelected && selected.size >= 3
                    OnboardingOptionCard(
                        icon = icon,
                        label = label,
                        selected = isSelected,
                        onClick = { if (!isDisabled) onSelect(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (selected.isNotEmpty()) {
            Text(
                stringResource(R.string.onboarding_traveler_selected, selected.size),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(Modifier.weight(1f))

        val infiniteTransition = rememberInfiniteTransition(label = "plane")
        val planeY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -12f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = androidx.compose.animation.core.EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "planeFloat"
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = stringResource(R.string.onboarding_cd_plane),
                tint = Color.White.copy(alpha = planeAlpha),
                modifier = Modifier
                    .size(100.dp)
                    .offset(
                        x = with(androidx.compose.ui.platform.LocalDensity.current) { planeOffsetX.toDp() },
                        y = with(androidx.compose.ui.platform.LocalDensity.current) { planeY.toDp() }
                    )
                    .rotate(90f)
            )
        }
    }
}

@Composable
private fun GroupAndCurrencyStep(
    selectedGroup: String,
    selectedCurrency: String,
    onGroupSelect: (String) -> Unit,
    onCurrencySelect: (String) -> Unit,
    isLanding: Boolean = false,
    onLandingFinished: () -> Unit = {}
) {
    val groups = listOf(
        Triple("solo", Icons.Default.Person, stringResource(R.string.onboarding_group_solo)),
        Triple("pareja", Icons.Default.Favorite, stringResource(R.string.onboarding_group_pareja)),
        Triple("familia", Icons.Default.Group, stringResource(R.string.onboarding_group_familia)),
        Triple("amigos", Icons.Default.People, stringResource(R.string.onboarding_group_amigos))
    )
    val currencies = listOf(
        "USD" to "🇺🇸", "EUR" to "🇪🇺", "ARS" to "🇦🇷", "COP" to "🇨🇴",
        "MXN" to "🇲🇽", "BRL" to "🇧🇷", "CLP" to "🇨🇱", "PEN" to "🇵🇪"
    )

    val planeOffsetX by animateFloatAsState(
        targetValue = if (isLanding) 0f else -600f,
        animationSpec = tween(900, easing = FastOutLinearInEasing),
        label = "landingX",
        finishedListener = { if (isLanding) onLandingFinished() }
    )
    val planeOffsetY by animateFloatAsState(
        targetValue = if (isLanding) 0f else -120f,
        animationSpec = tween(900, easing = FastOutLinearInEasing),
        label = "landingY"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            stringResource(R.string.onboarding_details_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.onboarding_details_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.onboarding_group_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        groups.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                row.forEach { (key, icon, label) ->
                    OnboardingOptionCard(
                        icon = icon,
                        label = label,
                        selected = selectedGroup == key,
                        onClick = { onGroupSelect(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.onboarding_currency_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        currencies.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                row.forEach { (code, flag) ->
                    val selected = selectedCurrency == code
                    Surface(
                        onClick = { onCurrencySelect(code) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.18f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                flag,
                                style = MaterialTheme.typography.bodyMedium,
                                // flag emoji es puramente decorativo, el código lo describe
                            )
                            Text(
                                code,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) BluePrimary else Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLanding) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent)
                            ),
                            RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.BottomCenter)
                )
                // Avión: clip para mostrar solo la silueta sin la línea del piso
                Box(
                    modifier = Modifier
                        .size(width = 168.dp, height = 132.dp) // altura recortada oculta línea del piso
                        .clipToBounds()
                        .offset(
                            x = with(androidx.compose.ui.platform.LocalDensity.current) { planeOffsetX.toDp() },
                            y = with(androidx.compose.ui.platform.LocalDensity.current) { planeOffsetY.toDp() }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.FlightLand,
                        contentDescription = stringResource(R.string.onboarding_cd_plane_landing),
                        tint = Color.White,
                        modifier = Modifier.size(450.dp)
                    )
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
                        contentDescription = stringResource(R.string.onboarding_cd_option_selected, label),
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
                    contentDescription = stringResource(R.string.onboarding_cd_option_unselected, label),
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

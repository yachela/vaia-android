package com.vaia.presentation.ui.asktrip

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaia.R
import com.vaia.domain.model.Stay
import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripPhase
import com.vaia.domain.model.TripQuestion
import com.vaia.domain.model.UnavailableReason
import com.vaia.presentation.viewmodel.AskTripViewModel
import com.vaia.presentation.viewmodel.QaTurn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * "Preguntale a tu viaje": el usuario elige entre preguntas prearmadas, nunca
 * escribe texto libre. Las que son sobre sus datos se calculan acá mismo contra
 * Room; las de consejos las responde el modelo vía `/ask`.
 *
 * Ver docs/spec-chat-viaje.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskTripScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    viewModel: AskTripViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showQuestions by remember { mutableStateOf(false) }

    LaunchedEffect(tripId) { viewModel.load(tripId) }

    // Con la conversación vacía la pantalla queda en blanco y no se ve qué se
    // puede preguntar: se abre el listado solo la primera vez.
    LaunchedEffect(uiState.isLoading, uiState.available) {
        if (!uiState.isLoading && uiState.turns.isEmpty() && uiState.available.isNotEmpty()) {
            showQuestions = true
        }
    }

    // Que la última respuesta quede a la vista, como en cualquier conversación.
    LaunchedEffect(uiState.turns.size) {
        if (uiState.turns.isNotEmpty()) {
            listState.animateScrollToItem(uiState.turns.size)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ask_trip_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.turns.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clear() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.ask_trip_clear)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isLoading && !uiState.hasNoData) {
                AskBar(onClick = { showQuestions = true })
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.hasNoData -> {
                    Text(
                        text = stringResource(R.string.ask_trip_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                    )
                }

                else -> Conversation(turns = uiState.turns, listState = listState)
            }
        }

        if (showQuestions) {
            QuestionSheet(
                available = uiState.available,
                onAsk = { question ->
                    showQuestions = false
                    viewModel.ask(question)
                },
                onDismiss = { showQuestions = false }
            )
        }
    }
}

/** Única acción de la pantalla: abrir el listado de preguntas. */
@Composable
private fun AskBar(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Icon(
                imageVector = Icons.Default.QuestionAnswer,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ask_trip_choose_question))
        }
    }
}

@Composable
private fun Conversation(
    turns: List<QaTurn>,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnswerBubble(stringResource(R.string.ask_trip_intro), emptyList())
                    Text(
                        text = stringResource(R.string.ask_trip_local_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(turns.size) { index ->
                val turn = turns[index]
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestionBubble(stringResource(labelOf(turn.question)))
                    val insight = turn.insight
                    if (insight == null) {
                        TypingBubble()
                    } else {
                        val answer = formatInsight(insight)
                        AnswerBubble(answer.headline, answer.lines)
                        // El disclaimer solo donde escribió el modelo: las respuestas
                        // calculadas son exactas y avisar lo contrario confunde.
                        if (insight is TripInsight.Generated) {
                            Text(
                                text = stringResource(R.string.ai_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun QuestionBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Tres puntos animados mientras se revela la respuesta. La respuesta ya está
 * calculada: esto le da al ojo tiempo de seguir la burbuja nueva, no simula
 * un procesamiento que no ocurre.
 */
@Composable
private fun TypingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics { contentDescription = TYPING_DESCRIPTION },
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val transition = rememberInfiniteTransition(label = "typing")
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 160),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(alpha)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                }
            }
        }
    }
}

private const val TYPING_DESCRIPTION = "Preparando la respuesta"

@Composable
private fun AnswerBubble(headline: String, lines: List<String>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                lines.forEach { line ->
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Las preguntas viven en un sheet y no en un panel fijo: son más de diez y
 * dejaban la conversación reducida a un par de líneas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionSheet(
    available: List<TripQuestion>,
    onAsk: (TripQuestion) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (generative, instant) = available.partition { it.needsAi }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // LazyColumn y no Column: con más de diez preguntas el contenido no entra
        // y la sección de IA quedaba abajo del corte, sin forma de llegar.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Un viaje recién creado puede no habilitar ninguna pregunta.
            if (available.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.ask_trip_no_questions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (instant.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.ask_section_instant),
                        subtitle = stringResource(R.string.ask_section_instant_hint)
                    )
                }
                items(instant.size) { QuestionChip(instant[it], onAsk) }
            }

            if (generative.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SectionHeader(
                        title = stringResource(R.string.ask_section_ai),
                        subtitle = stringResource(R.string.ask_section_ai_hint)
                    )
                }
                items(generative.size) { QuestionChip(generative[it], onAsk, isAi = true) }
            }
        }
    }
}

/**
 * El subtítulo es lo que explica para qué está cada grupo: sin eso, la
 * distinción entre una respuesta calculada y una escrita por el modelo no se
 * entiende mirando la lista.
 */
@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionChip(
    question: TripQuestion,
    onAsk: (TripQuestion) -> Unit,
    isAi: Boolean = false
) {
    Surface(
        onClick = { onAsk(question) },
        shape = RoundedCornerShape(20.dp),
        color = if (isAi) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isAi) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = stringResource(R.string.ask_ai_badge),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = stringResource(labelOf(question)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isAi) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

private fun labelOf(question: TripQuestion): Int = when (question) {
    TripQuestion.DAYS_UNTIL_TRIP -> R.string.ask_q_days_until_trip
    TripQuestion.NEXT_ACTIVITIES -> R.string.ask_q_next_activities
    TripQuestion.FREE_DAYS -> R.string.ask_q_free_days
    TripQuestion.WHERE_I_STAY -> R.string.ask_q_where_i_stay
    TripQuestion.DOCUMENTATION -> R.string.ask_q_documentation
    TripQuestion.DAILY_COST -> R.string.ask_q_daily_cost
    TripQuestion.LOCAL_TRANSPORT -> R.string.ask_q_local_transport
    TripQuestion.LOCAL_TIPS -> R.string.ask_q_local_tips
    TripQuestion.TOTAL_SPENT -> R.string.ask_q_total_spent
    TripQuestion.TOP_CATEGORY -> R.string.ask_q_top_category
    TripQuestion.REMAINING_BUDGET -> R.string.ask_q_remaining_budget
    TripQuestion.PENDING_PACKING -> R.string.ask_q_pending_packing
}

private data class FormattedAnswer(val headline: String, val lines: List<String> = emptyList())

/** Traduce los datos del insight a la redacción que ve el usuario. */
@Composable
private fun formatInsight(insight: TripInsight): FormattedAnswer = when (insight) {

    is TripInsight.DaysUntilTrip -> FormattedAnswer(
        when (insight.phase) {
            TripPhase.BEFORE -> when (insight.days) {
                0 -> stringResource(R.string.ask_a_days_today)
                1 -> stringResource(R.string.ask_a_days_before_one)
                else -> stringResource(R.string.ask_a_days_before, insight.days)
            }
            TripPhase.DURING -> if (insight.days <= 0) {
                stringResource(R.string.ask_a_days_during_last)
            } else {
                stringResource(R.string.ask_a_days_during, insight.days)
            }
            TripPhase.AFTER -> stringResource(R.string.ask_a_days_after)
        }
    )

    is TripInsight.NextActivities -> if (insight.date == null || insight.activities.isEmpty()) {
        FormattedAnswer(stringResource(R.string.ask_a_next_none))
    } else {
        FormattedAnswer(
            headline = stringResource(R.string.ask_a_next_header, formatDate(insight.date)),
            lines = insight.activities.map { activity ->
                listOfNotNull(
                    activity.time,
                    activity.title.takeIf { it.isNotBlank() },
                    activity.location?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
            }
        )
    }

    is TripInsight.FreeDays -> if (insight.dates.isEmpty()) {
        FormattedAnswer(stringResource(R.string.ask_a_free_none))
    } else {
        val shown = insight.dates.take(MAX_LISTED_DAYS)
        val extra = insight.dates.size - shown.size
        FormattedAnswer(
            headline = stringResource(
                R.string.ask_a_free_some,
                insight.dates.size,
                insight.totalDays
            ),
            lines = shown.map { formatDate(it) } +
                if (extra > 0) listOf(stringResource(R.string.ask_a_free_more, extra)) else emptyList()
        )
    }

    is TripInsight.Generated -> FormattedAnswer(insight.answer)

    is TripInsight.Unavailable -> FormattedAnswer(
        when (insight.reason) {
            UnavailableReason.OFFLINE -> stringResource(R.string.ask_a_offline)
            UnavailableReason.RATE_LIMITED -> stringResource(R.string.ask_a_rate_limited)
            UnavailableReason.SERVICE_ERROR -> stringResource(R.string.ask_a_service_error)
        }
    )

    is TripInsight.Accommodation -> {
        // Con el viaje en curso importa dónde dormís hoy; el resto es contexto.
        val others = insight.stays.filter { it !== insight.current }
        when {
            insight.current != null -> FormattedAnswer(
                headline = stringResource(R.string.ask_a_stay_current, describeStay(insight.current)),
                lines = others.map { stayLine(it) }
            )
            insight.stays.size == 1 -> FormattedAnswer(
                stringResource(R.string.ask_a_stay_one, describeStay(insight.stays.single()))
            )
            else -> FormattedAnswer(
                headline = stringResource(R.string.ask_a_stay_many, insight.stays.size),
                lines = insight.stays.map { stayLine(it) }
            )
        }
    }

    is TripInsight.TotalSpent -> FormattedAnswer(
        insight.percentUsed?.let { percent ->
            stringResource(
                R.string.ask_a_total_spent_budget,
                formatAmount(insight.total),
                formatAmount(insight.budget),
                percent
            )
        } ?: stringResource(R.string.ask_a_total_spent, formatAmount(insight.total))
    )

    is TripInsight.TopCategory -> FormattedAnswer(
        stringResource(
            R.string.ask_a_top_category,
            insight.category,
            formatAmount(insight.amount),
            insight.percentOfTotal
        )
    )

    is TripInsight.RemainingBudget -> FormattedAnswer(
        when {
            insight.isOverBudget ->
                stringResource(R.string.ask_a_remaining_over, formatAmount(abs(insight.remaining)))
            insight.perDay != null && insight.daysLeft != null -> stringResource(
                R.string.ask_a_remaining_per_day,
                formatAmount(insight.remaining),
                formatAmount(insight.perDay!!),
                insight.daysLeft!!
            )
            else -> stringResource(
                R.string.ask_a_remaining,
                formatAmount(insight.remaining),
                formatAmount(insight.budget)
            )
        }
    )

    is TripInsight.PendingPacking -> if (insight.pending == 0) {
        FormattedAnswer(stringResource(R.string.ask_a_packing_done, insight.total))
    } else {
        FormattedAnswer(
            headline = stringResource(R.string.ask_a_packing_pending, insight.pending, insight.total),
            lines = insight.byCategory.map { entry ->
                stringResource(R.string.ask_a_packing_category, entry.category, entry.count)
            }
        )
    }

    TripInsight.NotEnoughData -> FormattedAnswer(stringResource(R.string.ask_trip_not_enough_data))
}

/** Nombre del hospedaje con la dirección al lado, si está cargada. */
private fun describeStay(stay: Stay): String =
    listOfNotNull(stay.name, stay.location?.takeIf { it.isNotBlank() }).joinToString(" · ")

@Composable
private fun stayLine(stay: Stay): String = stay.checkIn?.let { checkIn ->
    stringResource(R.string.ask_a_stay_line_dated, describeStay(stay), formatDate(checkIn))
} ?: describeStay(stay)

private const val MAX_LISTED_DAYS = 5

private val SPANISH = Locale("es", "AR")
private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH)

private fun formatDate(date: LocalDate): String =
    date.format(DAY_FORMAT).replaceFirstChar { it.uppercase(SPANISH) }

/** Montos sin decimales cuando son redondos, que es el caso habitual. */
private fun formatAmount(value: Double): String {
    val rounded = value.roundToLong()
    return if (abs(value - rounded) < 0.01) {
        String.format(SPANISH, "%,d", rounded)
    } else {
        String.format(SPANISH, "%,.2f", value)
    }
}

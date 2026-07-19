package com.vaia.presentation.ui.asktrip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaia.R
import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripPhase
import com.vaia.domain.model.TripQuestion
import com.vaia.presentation.viewmodel.AskTripViewModel
import com.vaia.presentation.viewmodel.QaTurn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * "Preguntale a tu viaje": el usuario elige entre preguntas prearmadas y la app
 * las contesta con los datos que ya tiene en Room. Sin input libre, sin IA y sin
 * red — ver docs/spec-chat-viaje.md.
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

    LaunchedEffect(tripId) { viewModel.load(tripId) }

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

                else -> Conversation(
                    turns = uiState.turns,
                    available = uiState.available,
                    listState = listState,
                    onAsk = viewModel::ask
                )
            }
        }
    }
}

@Composable
private fun Conversation(
    turns: List<QaTurn>,
    available: List<TripQuestion>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAsk: (TripQuestion) -> Unit
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
                    val answer = formatInsight(turn.insight)
                    AnswerBubble(answer.headline, answer.lines)
                }
            }
        }

        QuestionPicker(available = available, onAsk = onAsk)
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

@Composable
private fun QuestionPicker(available: List<TripQuestion>, onAsk: (TripQuestion) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Un viaje recién creado, o uno ya terminado y sin gastos, puede no
            // habilitar ninguna pregunta: mejor decirlo que mostrar el título solo.
            if (available.isEmpty()) {
                Text(
                    text = stringResource(R.string.ask_trip_no_questions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Text(
                text = stringResource(R.string.ask_trip_more_questions),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            available.forEach { question ->
                Surface(
                    onClick = { onAsk(question) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(labelOf(question)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

private fun labelOf(question: TripQuestion): Int = when (question) {
    TripQuestion.DAYS_UNTIL_TRIP -> R.string.ask_q_days_until_trip
    TripQuestion.NEXT_ACTIVITIES -> R.string.ask_q_next_activities
    TripQuestion.FREE_DAYS -> R.string.ask_q_free_days
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

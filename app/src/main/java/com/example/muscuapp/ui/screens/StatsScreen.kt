package com.example.muscuapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.ui.components.MuscuCard
import com.example.muscuapp.ui.components.MuscuTopBar
import com.example.muscuapp.ui.components.SquircleShape
import com.example.muscuapp.ui.theme.MuscuTheme
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.util.Locale

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = emptyList())
    val modelProducer = remember { CartesianChartModelProducer.build() }

    val muscleCategories = remember(workouts) {
        workouts
            .flatMap { it.exercises }
            .map { it.exercise.category }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    val totalVolume = remember(workouts) {
        workouts.sumOf { workout ->
            workout.exercises.sumOf { ex ->
                (ex.exercise.sets * ex.exercise.reps * ex.exercise.weight).toDouble()
            }
        }
    }

    LaunchedEffect(workouts) {
        if (workouts.isNotEmpty()) {
            val volumeData = workouts.takeLast(10).reversed().map { workout ->
                workout.exercises.sumOf { ex ->
                    (ex.exercise.sets * ex.exercise.reps * ex.exercise.weight).toDouble()
                }
            }
            modelProducer.tryRunTransaction {
                lineSeries { series(volumeData) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuscuTheme.colors.background)
    ) {
        MuscuTopBar(
            title = "Statistiques & Progrès",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = MuscuTheme.spacing.medium,
                vertical = MuscuTheme.spacing.medium
            ),
            verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.large)
        ) {
            item {
                MuscuCard(borderColor = MuscuTheme.colors.primary.copy(alpha = 0.35f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.medium)) {
                        SectionHeader(
                            title = "Volume total",
                            subtitle = "10 dernières séances",
                            accent = MuscuTheme.colors.primary
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(SquircleShape(2.5f))
                                .background(MuscuTheme.colors.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            if (workouts.isNotEmpty()) {
                                CartesianChartHost(
                                    chart = rememberCartesianChart(
                                        rememberLineCartesianLayer(),
                                        startAxis = rememberStartAxis(),
                                        bottomAxis = rememberBottomAxis(),
                                    ),
                                    modelProducer = modelProducer,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                EmptyState(
                                    title = "Pas assez de données",
                                    subtitle = "Enchaîne quelques séances pour voir la courbe de progression."
                                )
                            }
                        }
                    }
                }
            }

            item {
                MuscuCard(borderColor = MuscuTheme.colors.secondary.copy(alpha = 0.35f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.medium)) {
                        SectionHeader(
                            title = "Répartition musculaire",
                            subtitle = if (muscleCategories.isEmpty()) "Aucune séance enregistrée" else "Lecture par groupes musculaires",
                            accent = MuscuTheme.colors.secondary
                        )

                        if (muscleCategories.isEmpty()) {
                            EmptyState(
                                title = "Aucune répartition disponible",
                                subtitle = "Ajoute des exercices pour voir quels groupes dominent tes séances."
                            )
                        } else {
                            val total = muscleCategories.sumOf { it.second }
                            Column(verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.small)) {
                                muscleCategories.forEach { (category, count) ->
                                    MuscleProgressRow(
                                        category = category,
                                        count = count,
                                        total = total
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                MuscuCard(borderColor = MuscuTheme.colors.textSecondary.copy(alpha = 0.25f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Volume cumulé",
                                style = MuscuTheme.typography.labelSmall,
                                color = MuscuTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatVolume(totalVolume),
                                style = MuscuTheme.typography.titleLarge,
                                color = MuscuTheme.colors.textPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp)
                                .clip(SquircleShape(5f))
                                .background(MuscuTheme.colors.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(4.dp)
                .clip(SquircleShape(5f))
                .background(accent)
        )

        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MuscuTheme.typography.titleMedium,
            color = MuscuTheme.colors.textPrimary,
            fontWeight = FontWeight.Black
        )

        Text(
            text = subtitle,
            style = MuscuTheme.typography.labelSmall,
            color = MuscuTheme.colors.textSecondary
        )
    }
}

@Composable
private fun MuscleProgressRow(
    category: String,
    count: Int,
    total: Int
) {
    val percentage = if (total > 0) count.toFloat() / total.toFloat() else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = category.uppercase(Locale.getDefault()),
            modifier = Modifier.width(92.dp),
            style = MuscuTheme.typography.labelSmall,
            color = MuscuTheme.colors.textSecondary,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MuscuTheme.colors.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MuscuTheme.colors.primary)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "${(percentage * 100).toInt()}%",
            style = MuscuTheme.typography.labelSmall,
            color = MuscuTheme.colors.textPrimary,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MuscuTheme.typography.titleMedium,
                color = MuscuTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MuscuTheme.typography.bodyMedium,
                color = MuscuTheme.colors.textSecondary
            )
        }
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", volume / 1_000_000)
        volume >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", volume / 1_000)
        else -> String.format(Locale.getDefault(), "%.0f", volume)
    }
}

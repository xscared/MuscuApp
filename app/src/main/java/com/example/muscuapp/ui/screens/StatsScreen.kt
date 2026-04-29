package com.example.muscuapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = emptyList())
    
    val modelProducer = remember { CartesianChartModelProducer.build() }
    
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiques & Progrès") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Volume Total (10 dernières séances)", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(top = 8.dp)
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
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Pas assez de données")
                        }
                    }
                }
            }
            
            item {
                Text("Répartition Musculaire", style = MaterialTheme.typography.titleMedium)
                val categories = workouts.flatMap { it.exercises }.map { it.exercise.category }
                    .groupingBy { it }.eachCount()
                
                Column(Modifier.padding(top = 8.dp)) {
                    categories.forEach { (cat, count) ->
                        val total = categories.values.sum()
                        val percentage = count.toFloat() / total
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(cat, Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier.weight(1f).height(8.dp).padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

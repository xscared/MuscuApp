package com.example.muscuapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.data.local.ExerciseSetEntity
import com.example.muscuapp.data.local.ExerciseWithSets
import com.example.muscuapp.data.local.WorkoutSessionEntity
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWorkoutScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = null)
    val workout = workouts?.find { it.session.sessionId == sessionId }
    
    var timeElapsed by remember { mutableStateOf(0L) }
    var restTimeLeft by remember { mutableStateOf(0) }
    var isRestTimerRunning by remember { mutableStateOf(false) }
    
    var showFinishSummary by remember { mutableStateOf(false) }

    LaunchedEffect(workout?.session?.startTime) {
        val startTime = workout?.session?.startTime ?: System.currentTimeMillis()
        while (workout?.session?.isLive == true) {
            timeElapsed = System.currentTimeMillis() - startTime
            delay(1000)
        }
    }

    LaunchedEffect(isRestTimerRunning, restTimeLeft) {
        if (isRestTimerRunning && restTimeLeft > 0) {
            delay(1000)
            restTimeLeft--
        } else if (restTimeLeft == 0) {
            isRestTimerRunning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(workout?.session?.title ?: "Live", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatElapsedTime(timeElapsed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showFinishSummary = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("TERMINER", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = restTimeLeft > 0,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "REPOS : ${restTimeLeft}s",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Row {
                            IconButton(onClick = { restTimeLeft += 15 }) {
                                Icon(Icons.Default.Add, "+15s")
                            }
                            TextButton(onClick = { restTimeLeft = 0 }) {
                                Text("PASSER", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (workout == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(workout.exercises.sortedBy { it.exercise.order }) { exerciseWithSets ->
                        LiveExerciseCard(
                            exerciseWithSets = exerciseWithSets,
                            onToggleSet = { set, completed ->
                                viewModel.toggleSetCompletion(set, completed)
                                if (completed) {
                                    restTimeLeft = 60
                                    isRestTimerRunning = true
                                }
                            },
                            onUpdateSet = { set ->
                                viewModel.updateSet(set)
                            },
                            onAddWarmup = {
                                viewModel.addWarmupSet(exerciseWithSets.exercise.id, 10, exerciseWithSets.exercise.weight * 0.4f)
                            },
                            onDeleteWarmup = { set ->
                                viewModel.deleteSet(set)
                            }
                        )
                    }
                }
            }
        }

        if (showFinishSummary && workout != null) {
            val totalVolume = workout.exercises.sumOf { ex -> 
                ex.sets.filter { it.isCompleted }.sumOf { (it.weight * it.reps).toDouble() }
            }
            val setsDone = workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

            AlertDialog(
                onDismissRequest = { showFinishSummary = false },
                title = { Text("Séance terminée ! 🔥") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Félicitations pour ton entraînement.")
                        Divider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Durée :")
                            Text(formatElapsedTime(timeElapsed), fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Volume total :")
                            Text("${totalVolume.toInt()} kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Séries complétées :")
                            Text("$setsDone", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.endWorkout(workout.session) {
                            onBack()
                        }
                    }) {
                        Text("Enregistrer et quitter")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishSummary = false }) {
                        Text("Continuer l'entraînement")
                    }
                }
            )
        }
    }
}

@Composable
fun LiveExerciseCard(
    exerciseWithSets: ExerciseWithSets,
    onToggleSet: (ExerciseSetEntity, Boolean) -> Unit,
    onUpdateSet: (ExerciseSetEntity) -> Unit,
    onAddWarmup: () -> Unit,
    onDeleteWarmup: (ExerciseSetEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(exerciseWithSets.exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onAddWarmup) {
                    Text("+ Échauffement", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SÉRIE", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("POIDS (KG)", modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                Text("REPS", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Affichage des séries triées : échauffement d'abord, puis timestamp
            val sortedSets = exerciseWithSets.sets.sortedWith(compareBy<ExerciseSetEntity> { !it.isWarmup }.thenBy { it.timestamp })
            
            sortedSets.forEachIndexed { index, set ->
                LiveSetRow(
                    index = index,
                    set = set,
                    onToggle = { onToggleSet(set, it) },
                    onUpdate = onUpdateSet,
                    onDelete = if (set.isWarmup) { { onDeleteWarmup(set) } } else null
                )
            }
        }
    }
}

@Composable
fun LiveSetRow(
    index: Int,
    set: ExerciseSetEntity,
    onToggle: (Boolean) -> Unit,
    onUpdate: (ExerciseSetEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var weightText by remember(set.weight) { mutableStateOf(set.weight.toString()) }
    var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                if (set.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.1f) 
                else if (set.isWarmup) Color(0xFFFF9800).copy(alpha = 0.1f)
                else Color.Transparent
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label (E ou Index)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (set.isWarmup) {
                Surface(color = Color(0xFFFF9800), shape = CircleShape) {
                    Text("E", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("${index + 1}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }

        // Weight Input
        TextField(
            value = weightText,
            onValueChange = { 
                weightText = it
                it.toFloatOrNull()?.let { w -> onUpdate(set.copy(weight = w)) }
            },
            modifier = Modifier.weight(1.5f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            singleLine = true
        )

        // Reps Input
        TextField(
            value = repsText,
            onValueChange = { 
                repsText = it
                it.toIntOrNull()?.let { r -> onUpdate(set.copy(reps = r)) }
            },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier.widthIn(min = if (onDelete != null) 96.dp else 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer l'échauffement",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(
                onClick = { onToggle(!set.isCompleted) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (set.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (set.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

fun formatElapsedTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

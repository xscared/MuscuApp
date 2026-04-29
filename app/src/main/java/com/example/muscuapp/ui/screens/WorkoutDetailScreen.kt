package com.example.muscuapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.data.local.ExerciseEntity
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    sessionId: Long,
    onAddExercise: (Long) -> Unit,
    onEditExercise: (Long, Long) -> Unit,
    onLiveClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = null)
    val weightUnit by viewModel.weightUnit.collectAsState()
    
    var exerciseToDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val workout = remember(workouts, sessionId) {
        workouts?.find { it.session.sessionId == sessionId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = workout?.session?.title ?: if (workouts == null) "Chargement..." else "Séance",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (workout != null) {
                        IconButton(onClick = { 
                            if (!workout.session.isLive) viewModel.startLiveWorkout(sessionId)
                            onLiveClick(sessionId) 
                        }) {
                            Icon(
                                if (workout.session.isLive) Icons.Default.PlayArrow else Icons.Default.PlayArrow, 
                                if (workout.session.isLive) "Continuer l'entraînement" else "Démarrer l'entraînement",
                                tint = if (workout.session.isLive) MaterialTheme.colorScheme.error else LocalContentColor.current
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (workout != null) {
                FloatingActionButton(onClick = { onAddExercise(sessionId) }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un exercice")
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = workout,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            label = "workout_content_anim"
        ) { currentWorkout ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (workouts == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (currentWorkout == null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Séance introuvable")
                        Button(onClick = onBack) { Text("Retour") }
                    }
                } else if (currentWorkout.exercises.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aucun exercice dans cette séance.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onAddExercise(sessionId) }) {
                            Text("Ajouter mon premier exercice")
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        val sortedExercises = currentWorkout.exercises.map { it.exercise }.sortedBy { it.order }
                        
                        itemsIndexed(sortedExercises, key = { _, it -> it.id }) { index, exercise ->
                            ExerciseItem(
                                exercise = exercise, 
                                onDelete = { 
                                    exerciseToDelete = exercise
                                    showDeleteConfirm = true
                                },
                                onToggleFavorite = { isFav -> viewModel.toggleFavorite(exercise, isFav) },
                                weightUnit = weightUnit,
                                onMoveUp = if (index > 0) {
                                    {
                                        val newList = sortedExercises.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index - 1, item)
                                        viewModel.reorderExercises(newList)
                                    }
                                } else null,
                                onMoveDown = if (index < sortedExercises.size - 1) {
                                    {
                                        val newList = sortedExercises.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index + 1, item)
                                        viewModel.reorderExercises(newList)
                                    }
                                } else null,
                                onEdit = { onEditExercise(sessionId, exercise.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm && exerciseToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirm = false
                    exerciseToDelete = null
                },
                title = { Text("Supprimer l'exercice ?") },
                text = { Text("Voulez-vous vraiment supprimer '${exerciseToDelete!!.name}' ? Cette action est irréversible.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteExercise(exerciseToDelete!!)
                            showDeleteConfirm = false
                            exerciseToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Supprimer") }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showDeleteConfirm = false
                        exerciseToDelete = null
                    }) { Text("Annuler") }
                }
            )
        }
    }
}

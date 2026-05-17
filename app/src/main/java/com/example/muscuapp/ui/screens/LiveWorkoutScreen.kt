package com.example.muscuapp.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.data.local.ExerciseSetEntity
import com.example.muscuapp.data.local.ExerciseWithSets
import com.example.muscuapp.service.WorkoutTimerService
import com.example.muscuapp.ui.components.MuscuButton
import com.example.muscuapp.ui.components.MuscuCard
import com.example.muscuapp.ui.components.MuscuConfirmDialog
import com.example.muscuapp.ui.components.MuscuSuccessCard
import com.example.muscuapp.ui.components.MuscuTextField
import com.example.muscuapp.ui.components.MuscuTopBar
import com.example.muscuapp.ui.theme.MuscuTheme
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
    val context = LocalContext.current
    
    var timeElapsed by remember { mutableLongStateOf(0L) }
    val showFinishSummary = remember { mutableStateOf(false) }
    var setToDelete by remember { mutableStateOf<ExerciseSetEntity?>(null) }

    val showRestTimeSelector = remember { mutableStateOf(false) }

    LaunchedEffect(workout?.session?.startTime) {
        val startTime = workout?.session?.startTime ?: System.currentTimeMillis()
        while (workout?.session?.isLive == true) {
            timeElapsed = System.currentTimeMillis() - startTime
            delay(1000)
        }
    }

    Scaffold(
        containerColor = MuscuTheme.colors.background,
        topBar = {
             MuscuTopBar(
                title = workout?.session?.title ?: "Live",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    TextButton(
                        onClick = { showFinishSummary.value = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MuscuTheme.colors.error)
                    ) {
                        Text("TERMINER", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (workout == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = MuscuTheme.colors.primary)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = MuscuTheme.spacing.medium,
                        bottom = 120.dp, // Plus d'espace au fond pour éviter les chevauchements
                        start = MuscuTheme.spacing.small,
                        end = MuscuTheme.spacing.small
                    ),
                    verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.small)
                ) {
                    item {
                        Text(
                            text = "DURÉE : ${formatElapsedTime(timeElapsed)}",
                            modifier = Modifier.padding(16.dp),
                            style = MuscuTheme.typography.labelSmall,
                            color = MuscuTheme.colors.primary
                        )
                    }
                    items(
                        items = workout.exercises.sortedBy { it.exercise.order },
                        key = { it.exercise.id }
                    ) { exerciseWithSets ->
                        LiveExerciseCard(
                            exerciseWithSets = exerciseWithSets,
                            modifier = Modifier.padding(horizontal = MuscuTheme.spacing.small),
                            onToggleSet = { set, completed ->
                                viewModel.toggleSetCompletion(set, completed)
                                if (completed) {
                                    showRestTimeSelector.value = true
                                }
                            },
                            onUpdateSet = { set ->
                                viewModel.updateSet(set)
                            },
                            onAddWarmup = {
                                viewModel.addWarmupSet(exerciseWithSets.exercise.id, 10, exerciseWithSets.exercise.weight * 0.4f)
                            },
                            onDeleteWarmup = { set ->
                                setToDelete = set
                            }
                        )
                    }
                    // Spacer final pour assurer que le dernier exo soit bien dégagé
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        MuscuConfirmDialog(
            visible = setToDelete != null,
            title = "Supprimer la série ?",
            message = "Cette action est irréversible.",
            confirmText = "SUPPRIMER",
            dismissText = "ANNULER",
            onConfirm = {
                setToDelete?.let { viewModel.deleteSet(it) }
                setToDelete = null
            },
            onDismiss = {
                setToDelete = null
            }
        )

        if (showRestTimeSelector.value) {
            var customTime by remember { mutableStateOf(TextFieldValue("60", selection = TextRange(0, 2))) }
            var isCustom by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                MuscuCard(
                    borderColor = MuscuTheme.colors.primary,
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Temps de repos",
                            style = MuscuTheme.typography.titleMedium,
                            color = MuscuTheme.colors.primary
                        )

                        if (!isCustom) {
                            MuscuButton(text = "1 MIN", onClick = { startTimer(context, 60, sessionId); showRestTimeSelector.value = false }, modifier = Modifier.fillMaxWidth())
                            MuscuButton(text = "1 MIN 30S", onClick = { startTimer(context, 90, sessionId); showRestTimeSelector.value = false }, modifier = Modifier.fillMaxWidth())
                            MuscuButton(text = "2 MIN", onClick = { startTimer(context, 120, sessionId); showRestTimeSelector.value = false }, modifier = Modifier.fillMaxWidth())
                            TextButton(
                                onClick = { isCustom = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("PERSONNALISÉ", color = MuscuTheme.colors.textSecondary)
                            }
                        } else {
                            MuscuTextField(
                                value = customTime.text,
                                onValueChange = { customTime = customTime.copy(text = it) },
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }

                            MuscuButton(
                                text = "DÉMARRER",
                                onClick = {
                                    val secs = customTime.text.toIntOrNull() ?: 60
                                    startTimer(context, secs, sessionId)
                                    showRestTimeSelector.value = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        TextButton(
                            onClick = { showRestTimeSelector.value = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("PASSER", color = MuscuTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }

        if (showFinishSummary.value && workout != null) {
            val totalVolume = workout.exercises.sumOf { ex ->
                ex.sets.filter { it.isCompleted }.sumOf { (it.weight * it.reps).toDouble() }
            }
            val setsDone = workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                MuscuSuccessCard(
                    title = "SÉANCE TERMINÉE ! 🔥",
                    message = "Félicitations !\nDurée : ${formatElapsedTime(timeElapsed)}\nVolume : ${totalVolume.toInt()} kg\nSéries : $setsDone",
                    onDismiss = {
                        viewModel.endWorkout(workout.session) {
                            onBack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LiveExerciseCard(
    exerciseWithSets: ExerciseWithSets,
    modifier: Modifier = Modifier,
    onToggleSet: (ExerciseSetEntity, Boolean) -> Unit,
    onUpdateSet: (ExerciseSetEntity) -> Unit,
    onAddWarmup: () -> Unit,
    onDeleteWarmup: (ExerciseSetEntity) -> Unit
) {
    MuscuCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // Prend tout l'espace restant sans pousser le bouton
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MuscuTheme.colors.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        exerciseWithSets.exercise.name.uppercase(),
                        style = MuscuTheme.typography.titleMedium,
                        color = MuscuTheme.colors.textPrimary,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                // Bouton d'ajout d'échauffement explicite
                TextButton(
                    onClick = onAddWarmup,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MuscuTheme.colors.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "ÉCHAUFF.",
                        style = MuscuTheme.typography.labelSmall,
                        color = MuscuTheme.colors.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SÉRIE", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MuscuTheme.typography.labelSmall, color = MuscuTheme.colors.textSecondary)
                Text("POIDS (KG)", modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center, style = MuscuTheme.typography.labelSmall, color = MuscuTheme.colors.textSecondary)
                Text("REPS", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MuscuTheme.typography.labelSmall, color = MuscuTheme.colors.textSecondary)
                Spacer(modifier = Modifier.width(48.dp))
            }

            val sortedSets = exerciseWithSets.sets.sortedWith(
                compareBy<ExerciseSetEntity> { it.order }
                    .thenBy { it.timestamp }
                    .thenBy { it.setId }
            )
            
            var regularSetCount = 0
            sortedSets.forEach { set ->
                val displayIndex = if (set.isWarmup) -1 else ++regularSetCount
                key(set.setId) {
                    LiveSetRow(
                        index = displayIndex,
                        set = set,
                        onToggle = { onToggleSet(set, it) },
                        onUpdate = onUpdateSet,
                        onDelete = if (set.isWarmup) { { onDeleteWarmup(set) } } else null
                    )
                }
            }
        }
    }
}

@Composable
fun LiveSetRow(
    index: Int, // -1 pour échauffement, sinon 1, 2, 3...
    set: ExerciseSetEntity,
    onToggle: (Boolean) -> Unit,
    onUpdate: (ExerciseSetEntity) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var weightText by remember(set.setId, set.weight) { mutableStateOf(set.weight.toString()) }
    var repsText by remember(set.setId, set.reps) { mutableStateOf(set.reps.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    set.isCompleted -> MuscuTheme.colors.success.copy(alpha = 0.15f)
                    set.isWarmup -> MuscuTheme.colors.secondary.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (set.isWarmup) {
                Surface(
                    color = MuscuTheme.colors.secondary.copy(alpha = 0.8f),
                    shape = CircleShape
                ) {
                    Text(
                        "E", 
                        color = Color.White, 
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), 
                        style = MuscuTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Text(
                    text = "$index", 
                    style = MuscuTheme.typography.bodyLarge,
                    color = if (set.isCompleted) MuscuTheme.colors.success else MuscuTheme.colors.textPrimary,
                    fontWeight = FontWeight.Black
                )
            }
        }

        MuscuTextField(
            value = weightText,
            onValueChange = { 
                weightText = it
                it.toFloatOrNull()?.let { w -> onUpdate(set.copy(weight = w)) }
            },
            modifier = Modifier.weight(1.5f),
            keyboardType = KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.width(8.dp))

        MuscuTextField(
            value = repsText,
            onValueChange = { 
                repsText = it
                it.toIntOrNull()?.let { r -> onUpdate(set.copy(reps = r)) }
            },
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Number
        )

        Row(
            modifier = Modifier.widthIn(min = if (onDelete != null) 96.dp else 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MuscuTheme.colors.error, modifier = Modifier.size(20.dp))
                }
            }

            IconButton(onClick = { onToggle(!set.isCompleted) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (set.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (set.isCompleted) MuscuTheme.colors.success else MuscuTheme.colors.divider,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

    }
}

private fun startTimer(context: android.content.Context, seconds: Int, sessionId: Long) {
    val intent = Intent(context, WorkoutTimerService::class.java).apply {
        action = "START_TIMER"
        putExtra("DURATION", seconds)
        putExtra("SESSION_ID", sessionId)
    }
    ContextCompat.startForegroundService(context, intent)
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

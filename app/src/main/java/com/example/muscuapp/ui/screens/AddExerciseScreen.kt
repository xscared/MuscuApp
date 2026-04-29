package com.example.muscuapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import com.example.muscuapp.ui.viewmodel.SetInfo

data class SetDraft(
    val id: Long = System.nanoTime(), // Pour la clé Compose
    val reps: String = "10",
    val weight: String = "20",
    val isWarmup: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(
    sessionId: Long,
    exerciseId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = emptyList())
    val existingExerciseWithSets = remember(workouts, exerciseId) {
        if (exerciseId != null) {
            workouts.flatMap { it.exercises }.find { it.exercise.id == exerciseId }
        } else null
    }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Autre") }
    var note by remember { mutableStateOf("") }
    var setDrafts by remember { mutableStateOf(listOf(SetDraft())) }

    LaunchedEffect(existingExerciseWithSets) {
        existingExerciseWithSets?.let {
            name = it.exercise.name
            category = it.exercise.category
            note = it.exercise.note
            setDrafts = it.sets.map { set ->
                SetDraft(
                    id = set.setId,
                    reps = set.reps.toString(),
                    weight = set.weight.toString(),
                    isWarmup = set.isWarmup
                )
            }
        }
    }

    val categories = listOf("Pectoraux", "Dos", "Jambes", "Épaules", "Bras", "Abdos", "Autre")
    val exerciseSuggestions = listOf(
        "Développé couché", "Squat à la barre", "Soulevé de terre", "Tractions", 
        "Pompes", "Fentes", "Curls haltères", "Extension triceps", 
        "Rowing barre", "Développé militaire", "Planche", "Crunchs"
    )
    
    var expanded by remember { mutableStateOf(false) }
    var suggestionExpanded by remember { mutableStateOf(false) }

    val filteredSuggestions = remember(name) {
        if (name.isEmpty()) emptyList() 
        else exerciseSuggestions.filter { it.contains(name, ignoreCase = true) && it != name }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId == null) "Ajouter un exercice" else "Modifier l'exercice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Catégorie
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, "Ouvrir")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false })
                    }
                }
            }

            // Nom de l'exercice avec suggestions
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        suggestionExpanded = filteredSuggestions.isNotEmpty()
                    },
                    label = { Text("Nom de l'exercice") },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = suggestionExpanded,
                    onDismissRequest = { suggestionExpanded = false },
                    properties = PopupProperties(focusable = false)
                ) {
                    filteredSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = { name = suggestion; suggestionExpanded = false }
                        )
                    }
                }
            }

            // Section Séries
            Text("Séries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                setDrafts.forEachIndexed { index, draft ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Label E ou Index
                        IconButton(
                            onClick = {
                                setDrafts = setDrafts.toMutableList().apply {
                                    this[index] = draft.copy(isWarmup = !draft.isWarmup)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Surface(
                                color = if (draft.isWarmup) Color(0xFFFF9800) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ) {
                                Text(
                                    if (draft.isWarmup) "E" else "${index + 1}",
                                    color = if (draft.isWarmup) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = draft.weight,
                            onValueChange = { valText ->
                                setDrafts = setDrafts.toMutableList().apply {
                                    this[index] = draft.copy(weight = valText)
                                }
                            },
                            label = { Text("Poids") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = draft.reps,
                            onValueChange = { valText ->
                                setDrafts = setDrafts.toMutableList().apply {
                                    this[index] = draft.copy(reps = valText)
                                }
                            },
                            label = { Text("Reps") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        IconButton(onClick = {
                            if (setDrafts.size > 1) {
                                setDrafts = setDrafts.toMutableList().apply { removeAt(index) }
                            }
                        }) {
                            Icon(Icons.Default.RemoveCircleOutline, "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Button(
                    onClick = {
                        val lastSet = setDrafts.lastOrNull()
                        setDrafts = setDrafts + SetDraft(
                            weight = lastSet?.weight ?: "20",
                            reps = lastSet?.reps ?: "10",
                            isWarmup = false
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter une série")
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Notes (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (name.isNotBlank() && setDrafts.isNotEmpty()) {
                        val parsedSets = setDrafts.map { draft ->
                            SetInfo(
                                reps = draft.reps.toIntOrNull() ?: 0,
                                weight = draft.weight.toFloatOrNull() ?: 0f,
                                isWarmup = draft.isWarmup
                            )
                        }
                        
                        if (exerciseId == null) {
                            viewModel.addExerciseWithDetailedSets(sessionId, name, category, note, parsedSets)
                        } else {
                            existingExerciseWithSets?.exercise?.let {
                                viewModel.updateExerciseWithDetailedSets(it.copy(name = name, category = category, note = note), parsedSets)
                            }
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (exerciseId == null) "Créer l'exercice" else "Sauvegarder les modifications", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

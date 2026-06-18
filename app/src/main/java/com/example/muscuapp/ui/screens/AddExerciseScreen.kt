package com.example.muscuapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.muscuapp.ui.components.MuscuButton
import com.example.muscuapp.ui.components.MuscuTextField
import com.example.muscuapp.ui.components.MuscuTopBar
import com.example.muscuapp.ui.theme.MuscuTheme
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import com.example.muscuapp.ui.viewmodel.SetInfo

data class SetDraft(
    val id: Long = System.nanoTime(), // Pour la clé Compose
    val reps: String = "10",
    val weight: String = "20",
    val isWarmup: Boolean = false,
    val persistedSetId: Long? = null,
    val persistedOrder: Int? = null
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
            setDrafts = it.sets
                .sortedWith(
                    compareBy<com.example.muscuapp.data.local.ExerciseSetEntity> { set -> set.order }
                        .thenBy { set -> set.timestamp }
                        .thenBy { set -> set.setId }
                )
                .map { set ->
                SetDraft(
                    id = set.setId,
                    reps = set.reps.toString(),
                    weight = set.weight.toString(),
                    isWarmup = set.isWarmup,
                    persistedSetId = set.setId,
                    persistedOrder = set.order
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
            MuscuTopBar(
                title = if (exerciseId == null) "Ajouter un exercice" else "Modifier l'exercice",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = MuscuTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(MuscuTheme.spacing.medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.medium)
        ) {
            // Catégorie
            Column(verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.extraSmall)) {
                Text(
                    "Catégorie",
                    style = MuscuTheme.typography.bodyMedium,
                    color = MuscuTheme.colors.textSecondary
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Ouvrir", tint = MuscuTheme.colors.textPrimary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MuscuTheme.typography.bodyLarge.copy(color = MuscuTheme.colors.textPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MuscuTheme.colors.primary,
                            unfocusedBorderColor = MuscuTheme.colors.divider,
                            unfocusedContainerColor = MuscuTheme.colors.surface,
                            focusedContainerColor = MuscuTheme.colors.surface
                        )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MuscuTheme.colors.surface)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = MuscuTheme.colors.textPrimary) },
                                onClick = { category = cat; expanded = false }
                            )
                        }
                    }
                }
            }

            // Nom de l'exercice avec suggestions
            Column(verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.extraSmall)) {
                Text(
                    "Nom de l'exercice",
                    style = MuscuTheme.typography.bodyMedium,
                    color = MuscuTheme.colors.textSecondary
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            suggestionExpanded = filteredSuggestions.isNotEmpty()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MuscuTheme.typography.bodyLarge.copy(color = MuscuTheme.colors.textPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MuscuTheme.colors.primary,
                            unfocusedBorderColor = MuscuTheme.colors.divider,
                            unfocusedContainerColor = MuscuTheme.colors.surface,
                            focusedContainerColor = MuscuTheme.colors.surface
                        )
                    )
                    DropdownMenu(
                        expanded = suggestionExpanded,
                        onDismissRequest = { suggestionExpanded = false },
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier.background(MuscuTheme.colors.surface)
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion, color = MuscuTheme.colors.textPrimary) },
                                onClick = { name = suggestion; suggestionExpanded = false }
                            )
                        }
                    }
                }
            }

            // Section Séries
            Text(
                "Séries",
                style = MuscuTheme.typography.titleMedium,
                color = MuscuTheme.colors.textPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.small)) {
                setDrafts.forEachIndexed { index, draft ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.small)
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
                                color = if (draft.isWarmup) Color(0xFFFF9800) else MuscuTheme.colors.divider,
                                shape = CircleShape
                            ) {
                                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (draft.isWarmup) "E" else "${index + 1}",
                                        color = if (draft.isWarmup) Color.White else MuscuTheme.colors.textSecondary,
                                        style = MuscuTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        MuscuTextField(
                            value = draft.weight,
                            onValueChange = { valText ->
                                setDrafts = setDrafts.toMutableList().apply {
                                    this[index] = draft.copy(weight = valText)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Decimal
                        )

                        MuscuTextField(
                            value = draft.reps,
                            onValueChange = { valText ->
                                setDrafts = setDrafts.toMutableList().apply {
                                    this[index] = draft.copy(reps = valText)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Number
                        )

                        IconButton(onClick = {
                            if (setDrafts.size > 1) {
                                setDrafts = setDrafts.toMutableList().apply { removeAt(index) }
                            }
                        }) {
                            Icon(Icons.Default.RemoveCircleOutline, "Supprimer", tint = MuscuTheme.colors.error)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MuscuTheme.colors.secondary.copy(alpha = 0.1f),
                        contentColor = MuscuTheme.colors.secondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(MuscuTheme.spacing.small))
                    Text("Ajouter une série", style = MuscuTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.extraSmall)) {
                Text(
                    "Notes (optionnel)",
                    style = MuscuTheme.typography.bodyMedium,
                    color = MuscuTheme.colors.textSecondary
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    textStyle = MuscuTheme.typography.bodyLarge.copy(color = MuscuTheme.colors.textPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MuscuTheme.colors.primary,
                        unfocusedBorderColor = MuscuTheme.colors.divider,
                        unfocusedContainerColor = MuscuTheme.colors.surface,
                        focusedContainerColor = MuscuTheme.colors.surface
                    )
                )
            }

            Spacer(modifier = Modifier.height(MuscuTheme.spacing.medium))

            MuscuButton(
                text = if (exerciseId == null) "Créer l'exercice" else "Sauvegarder",
                onClick = {
                    if (name.isNotBlank() && setDrafts.isNotEmpty()) {
                        val parsedSets = setDrafts.map { draft ->
                            SetInfo(
                                reps = draft.reps.toIntOrNull() ?: 0,
                                weight = draft.weight.toFloatOrNull() ?: 0f,
                                isWarmup = draft.isWarmup,
                                persistedSetId = draft.persistedSetId,
                                persistedOrder = draft.persistedOrder
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


package com.example.muscuapp.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.data.local.WorkoutSessionEntity
import com.example.muscuapp.data.local.TemplateWithExercises
import com.example.muscuapp.data.local.WorkoutWithExercisesAndSets
import com.example.muscuapp.data.prefs.WeightUnit
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onWorkoutClick: (Long) -> Unit,
    onStatsClick: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = emptyList())
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    val streakCount by viewModel.streakCount.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val context = LocalContext.current
    
    var showTimer by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var newWorkoutTitle by remember { mutableStateOf("") }
    
    var selectedWorkoutForAction by remember { mutableStateOf<WorkoutSessionEntity?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    
    var showSettingsMenu by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri) { success ->
                Toast.makeText(context, if (success) "Sauvegarde exportée" else "Erreur d'exportation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.importBackup(uri) { success ->
                Toast.makeText(context, if (success) "Sauvegarde importée" else "Erreur d'importation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MuscuApp")
                        if (streakCount > 0) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                color = Color(0xFFFF5722).copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                                    Text(" $streakCount", color = Color(0xFFFF5722), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats")
                    }
                    IconButton(onClick = { showTimer = !showTimer }) {
                        Icon(
                            imageVector = if (showTimer) Icons.Default.TimerOff else Icons.Default.Timer,
                            contentDescription = "Chronomètre"
                        )
                    }
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unité : ${if (weightUnit == WeightUnit.KG) "KG" else "LBS"}") },
                                onClick = { 
                                    viewModel.toggleWeightUnit()
                                    showSettingsMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Partager en CSV") },
                                onClick = {
                                    showSettingsMenu = false
                                    viewModel.exportCsv { csvData ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_TEXT, csvData)
                                            putExtra(Intent.EXTRA_SUBJECT, "Export MuscuApp")
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Exporter mes données"))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sauvegarder les données (JSON)") },
                                onClick = {
                                    showSettingsMenu = false
                                    exportBackupLauncher.launch("muscuapp_backup.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restaurer une sauvegarde (JSON)") },
                                onClick = {
                                    showSettingsMenu = false
                                    importBackupLauncher.launch("application/json")
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (templates.isNotEmpty()) {
                    SmallFloatingActionButton(onClick = { showTemplateDialog = true }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Démarrer un modèle")
                    }
                }
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvelle Séance")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            AnimatedVisibility(visible = showTimer) { RestTimer() }

            if (workouts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune séance.\nAppuyez sur + pour commencer.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(workouts) { workout ->
                        WorkoutItem(
                            workout = workout,
                            onClick = { onWorkoutClick(workout.session.sessionId) },
                            onLongClick = {
                                selectedWorkoutForAction = workout.session
                                renameText = workout.session.title
                                showActionMenu = true
                            }
                        )
                    }
                }
            }
        }

        if (showActionMenu && selectedWorkoutForAction != null) {
            AlertDialog(
                onDismissRequest = { showActionMenu = false },
                title = { Text(selectedWorkoutForAction!!.title) },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text("Renommer") },
                            leadingContent = { Icon(Icons.Default.Edit, null) },
                            modifier = Modifier.clickable {
                                showActionMenu = false
                                showRenameDialog = true
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                showActionMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showActionMenu = false }) { Text("Fermer") }
                }
            )
        }

        if (showRenameDialog && selectedWorkoutForAction != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Renommer la séance") },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Nouveau nom") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameWorkout(selectedWorkoutForAction!!, renameText)
                            showRenameDialog = false
                            selectedWorkoutForAction = null
                        }
                    }) { Text("Enregistrer") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Annuler") }
                }
            )
        }

        if (showDeleteConfirm && selectedWorkoutForAction != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Supprimer la séance ?") },
                text = { Text("Cette action supprimera définitivement la séance '${selectedWorkoutForAction!!.title}' et tous les exercices associés.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteWorkout(selectedWorkoutForAction!!)
                            showDeleteConfirm = false
                            selectedWorkoutForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Supprimer") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
                }
            )
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Nouvelle Séance") },
                text = {
                    OutlinedTextField(
                        value = newWorkoutTitle,
                        onValueChange = { newWorkoutTitle = it },
                        label = { Text("Nom de la séance (ex: Push)") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newWorkoutTitle.isNotBlank()) {
                            viewModel.createWorkout(newWorkoutTitle) { id ->
                                onWorkoutClick(id)
                            }
                            showAddDialog = false
                            newWorkoutTitle = ""
                        }
                    }) { Text("Créer") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Annuler") }
                }
            )
        }

        if (showTemplateDialog) {
            AlertDialog(
                onDismissRequest = { showTemplateDialog = false },
                title = { Text("Démarrer depuis un modèle") },
                text = {
                    LazyColumn {
                        items(templates) { template ->
                            ListItem(
                                headlineContent = { Text(template.template.name) },
                                modifier = Modifier.clickable {
                                    viewModel.createWorkoutFromTemplate(template)
                                    showTemplateDialog = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTemplateDialog = false }) { Text("Fermer") }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutItem(workout: WorkoutWithExercisesAndSets, onClick: () -> Unit, onLongClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.session.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${workout.exercises.size} exercices • ${dateFormat.format(Date(workout.session.date))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (workout.session.isLive) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("LIVE")
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
fun RestTimer() {
    var timeLeft by remember { mutableStateOf(60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft -= 1
        } else if (timeLeft == 0) {
            isRunning = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Repos", style = MaterialTheme.typography.labelLarge)
            Text(
                text = String.format(Locale.US, "%02d:%02d", timeLeft / 60, timeLeft % 60),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { isRunning = !isRunning }, modifier = Modifier.width(120.dp)) {
                    Text(if (isRunning) "Pause" else "Démarrer")
                }
                OutlinedButton(onClick = { timeLeft = 60; isRunning = false }) { Text("60s") }
                OutlinedButton(onClick = { timeLeft = 90; isRunning = false }) { Text("90s") }
            }
        }
    }
}

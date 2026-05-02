package com.example.muscuapp.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.data.local.WorkoutSessionEntity
import com.example.muscuapp.data.prefs.WeightUnit
import com.example.muscuapp.data.prefs.ThemeMode
import com.example.muscuapp.ui.components.*
import com.example.muscuapp.ui.theme.MuscuTheme
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel

@Composable
fun HomeScreen(
    onWorkoutClick: (Long) -> Unit,
    onStatsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = emptyList())
    val templates by viewModel.templates.collectAsState(initial = emptyList())
    val weightUnit by viewModel.weightUnit.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var newWorkoutTitle by remember { mutableStateOf("") }
    
    var showSettingsSheet by remember { mutableStateOf(false) }

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

    MuscuScreen(
        title = "Mes Séances",
        actions = {
            IconButton(onClick = onCalendarClick) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Calendrier", tint = MuscuTheme.colors.textPrimary)
            }
            IconButton(onClick = onStatsClick) {
                Icon(Icons.Default.BarChart, contentDescription = "Stats", tint = MuscuTheme.colors.textPrimary)
            }
            IconButton(onClick = { showSettingsSheet = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Paramètres", tint = MuscuTheme.colors.textPrimary)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxSize().muscuClickable { showAddDialog = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MuscuTheme.colors.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "NOUVELLE SÉANCE",
                    style = MuscuTheme.typography.labelSmall,
                    color = MuscuTheme.colors.onPrimary
                )
            }
        }
    ) {
        if (workouts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aucune séance.\nAppuyez sur + pour commencer.", 
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MuscuTheme.colors.textSecondary,
                        style = MuscuTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(workouts) { workout ->
                MuscuWorkoutItem(
                    workout = workout,
                    onClick = { onWorkoutClick(workout.session.sessionId) }
                )
            }
        }
    }

    // --- CUSTOM SETTINGS SHEET ---
    MuscuActionSheet(
        visible = showSettingsSheet,
        onDismiss = { showSettingsSheet = false },
        title = "Paramètres"
    ) {
        MuscuActionItem(
            label = "Thème : ${when(themeMode) {
                ThemeMode.LIGHT -> "Clair"
                ThemeMode.DARK -> "Sombre"
                ThemeMode.SYSTEM -> "Système"
            }}",
            icon = Icons.Default.Palette,
            onClick = {
                val nextMode = when(themeMode) {
                    ThemeMode.SYSTEM -> ThemeMode.LIGHT
                    ThemeMode.LIGHT -> ThemeMode.DARK
                    ThemeMode.DARK -> ThemeMode.SYSTEM
                }
                viewModel.setThemeMode(nextMode)
            }
        )
        MuscuActionItem(
            label = "Unité : ${if (weightUnit == WeightUnit.KG) "KG" else "LBS"}",
            icon = Icons.Default.Straighten,
            onClick = { viewModel.toggleWeightUnit() }
        )
        MuscuActionItem(
            label = "Partager en CSV",
            icon = Icons.Default.Share,
            onClick = {
                showSettingsSheet = false
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
        MuscuActionItem(
            label = "Sauvegarder (JSON)",
            icon = Icons.Default.CloudUpload,
            onClick = {
                showSettingsSheet = false
                exportBackupLauncher.launch("muscuapp_backup.json")
            }
        )
        MuscuActionItem(
            label = "Restaurer (JSON)",
            icon = Icons.Default.CloudDownload,
            onClick = {
                showSettingsSheet = false
                importBackupLauncher.launch("application/json")
            }
        )
    }

    // Dialogs remain for input fields, but using our custom interaction logic
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nouvelle Séance", style = MuscuTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = newWorkoutTitle,
                    onValueChange = { newWorkoutTitle = it },
                    label = { Text("Nom de la séance") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MuscuTheme.colors.primary,
                        unfocusedBorderColor = MuscuTheme.colors.divider
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newWorkoutTitle.isNotBlank()) {
                            viewModel.createWorkout(newWorkoutTitle) { id ->
                                onWorkoutClick(id)
                            }
                            showAddDialog = false
                            newWorkoutTitle = ""
                        }
                    }
                ) { Text("CRÉER", color = MuscuTheme.colors.primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("ANNULER", color = MuscuTheme.colors.textSecondary) }
            }
        )
    }

    if (showTemplateDialog) {
        MuscuActionSheet(
            visible = showTemplateDialog,
            onDismiss = { showTemplateDialog = false },
            title = "Démarrer un modèle"
        ) {
            templates.forEach { template ->
                MuscuActionItem(
                    label = template.template.name,
                    icon = Icons.Default.ContentPaste,
                    onClick = {
                        viewModel.createWorkoutFromTemplate(template)
                        showTemplateDialog = false
                    }
                )
            }
        }
    }
}

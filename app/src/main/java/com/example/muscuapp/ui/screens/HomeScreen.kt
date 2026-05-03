package com.example.muscuapp.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.data.local.WorkoutSessionEntity
import com.example.muscuapp.data.local.WorkoutWithExercisesAndSets
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
    
    var showAddSheet by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var newWorkoutTitle by remember { mutableStateOf("") }
    
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    var selectedWorkoutForActions by remember { mutableStateOf<WorkoutWithExercisesAndSets?>(null) }
    var showWorkoutActions by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }

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
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxSize().muscuClickable { showAddSheet = true },
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
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MuscuTheme.spacing.medium)
                    .padding(bottom = MuscuTheme.spacing.large),
                horizontalArrangement = Arrangement.spacedBy(MuscuTheme.spacing.small)
            ) {
                NavDashboardItem(
                    label = "STATS",
                    icon = Icons.Default.BarChart,
                    color = MuscuTheme.colors.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onStatsClick
                )
                NavDashboardItem(
                    label = "DATES",
                    icon = Icons.Default.CalendarMonth,
                    color = MuscuTheme.colors.secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onCalendarClick
                )
                NavDashboardItem(
                    label = "SETUP",
                    icon = Icons.Default.Settings,
                    color = MuscuTheme.colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { showSettingsSheet = true }
                )
            }
        }

        if (workouts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "AUCUNE SÉANCE.\nDÉMARRE MAINTENANT.", 
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MuscuTheme.colors.textSecondary,
                        style = MuscuTheme.typography.titleMedium,
                        letterSpacing = 2.sp
                    )
                }
            }
        } else {
            items(workouts) { workout ->
                MuscuWorkoutItem(
                    workout = workout,
                    onClick = { onWorkoutClick(workout.session.sessionId) },
                    onLongClick = {
                        selectedWorkoutForActions = workout
                        showWorkoutActions = true
                    }
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

    // --- CUSTOM ADD WORKOUT SHEET (Replaces AlertDialog) ---
    MuscuActionSheet(
        visible = showAddSheet,
        onDismiss = { showAddSheet = false },
        title = "Nouvelle Séance"
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            MuscuTextField(
                value = newWorkoutTitle,
                onValueChange = { newWorkoutTitle = it },
                placeholder = "NOM DE LA SÉANCE (EX: PUSH DAY)"
            )
            Spacer(modifier = Modifier.height(24.dp))
            MuscuButton(
                text = "CRÉER LA SÉANCE",
                onClick = {
                    if (newWorkoutTitle.isNotBlank()) {
                        viewModel.createWorkout(newWorkoutTitle) { id ->
                            onWorkoutClick(id)
                        }
                        showAddSheet = false
                        newWorkoutTitle = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
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

    // --- WORKOUT ACTIONS SHEET ---
    MuscuActionSheet(
        visible = showWorkoutActions,
        onDismiss = { showWorkoutActions = false },
        title = selectedWorkoutForActions?.session?.title?.uppercase() ?: "SÉANCE"
    ) {
        MuscuActionItem(
            label = "RENOMMER",
            icon = Icons.Default.Edit,
            onClick = {
                renameValue = selectedWorkoutForActions?.session?.title ?: ""
                showWorkoutActions = false
                showRenameSheet = true
            }
        )
        MuscuActionItem(
            label = "SUPPRIMER",
            icon = Icons.Default.Delete,
            color = MuscuTheme.colors.error,
            onClick = {
                selectedWorkoutForActions?.let { viewModel.deleteWorkout(it.session) }
                showWorkoutActions = false
            }
        )
    }

    // --- RENAME SHEET ---
    MuscuActionSheet(
        visible = showRenameSheet,
        onDismiss = { showRenameSheet = false },
        title = "Renommer la séance"
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            MuscuTextField(
                value = renameValue,
                onValueChange = { renameValue = it },
                placeholder = "NOUVEAU NOM"
            )
            Spacer(modifier = Modifier.height(24.dp))
            MuscuButton(
                text = "VALIDER",
                onClick = {
                    selectedWorkoutForActions?.let { 
                        viewModel.renameWorkout(it.session, renameValue)
                    }
                    showRenameSheet = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun NavDashboardItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(SquircleShape(n = 3.0f))
            .background(MuscuTheme.colors.surfaceVariant)
            .muscuClickable(onClick = onClick)
            .padding(MuscuTheme.spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MuscuTheme.typography.labelSmall,
                color = MuscuTheme.colors.textPrimary,
                letterSpacing = 1.sp
            )
        }
    }
}

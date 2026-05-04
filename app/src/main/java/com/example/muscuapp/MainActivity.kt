package com.example.muscuapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.muscuapp.service.WorkoutTimerService
import com.example.muscuapp.ui.components.MuscuCard
import com.example.muscuapp.ui.screens.*
import com.example.muscuapp.ui.theme.MuscuTheme
import com.example.muscuapp.ui.theme.MuscuAppTheme
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var offsetX by mutableFloatStateOf(0f)
    private var offsetY by mutableFloatStateOf(0f)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Explain to the user that the feature is unavailable
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            val viewModel: ExerciseViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val hapticEnabled by viewModel.hapticEnabled.collectAsState()
            val hapticIntensity by viewModel.hapticIntensity.collectAsState()
            
            MuscuAppTheme(
                themeMode = themeMode,
                hapticEnabled = hapticEnabled,
                hapticIntensity = hapticIntensity
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MuscuTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(
                                    onWorkoutClick = { id -> navController.navigate("detail/$id") },
                                    onLiveClick = { id -> navController.navigate("live/$id") },
                                    onStatsClick = { navController.navigate("stats") },
                                    onCalendarClick = { navController.navigate("calendar") }
                                )
                            }
                            composable("stats") {
                                StatsScreen(onBack = { navController.popBackStack() })
                            }
                            composable("calendar") {
                                CalendarScreen(
                                    onWorkoutClick = { id -> navController.navigate("detail/$id") },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                "detail/{sessionId}",
                                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                                WorkoutDetailScreen(
                                    sessionId = sessionId,
                                    onAddExercise = { id -> navController.navigate("add/$id") },
                                    onEditExercise = { sid, eid -> navController.navigate("edit/$sid/$eid") },
                                    onLiveClick = { id -> navController.navigate("live/$id") },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                "live/{sessionId}",
                                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                                deepLinks = listOf(navDeepLink { uriPattern = "muscuapp://live/{sessionId}" })
                            ) { backStackEntry ->
                                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                                LiveWorkoutScreen(
                                    sessionId = sessionId,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                "add/{sessionId}",
                                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                                AddExerciseScreen(
                                    sessionId = sessionId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                "edit/{sessionId}/{exerciseId}",
                                arguments = listOf(
                                    navArgument("sessionId") { type = NavType.LongType },
                                    navArgument("exerciseId") { type = NavType.LongType }
                                )
                            ) { backStackEntry ->
                                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                                val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L
                                AddExerciseScreen(
                                    sessionId = sessionId,
                                    exerciseId = exerciseId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // --- COMPTEUR GLOBAL DRAGGABLE ---
                        GlobalDraggableTimer(
                            navController = navController,
                            currentOffsetX = offsetX,
                            currentOffsetY = offsetY,
                            onPositionChange = { x, y ->
                                offsetX = x
                                offsetY = y
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalDraggableTimer(
    navController: NavController,
    currentOffsetX: Float,
    currentOffsetY: Float,
    onPositionChange: (Float, Float) -> Unit
) {
    val isTimerRunning = WorkoutTimerService.isTimerRunning.value
    val isAlarmPlaying = WorkoutTimerService.isAlarmPlaying.value
    val timerSeconds = WorkoutTimerService.currentTimerSeconds.intValue
    val currentSessionId = WorkoutTimerService.currentSessionId.longValue
    val context = androidx.compose.ui.platform.LocalContext.current

    // Utilisation de rememberUpdatedState pour que le detectDragGestures (pointerInput(Unit))
    // utilise toujours les dernières valeurs sans redémarrer le pointerInput.
    val updatedX by rememberUpdatedState(currentOffsetX)
    val updatedY by rememberUpdatedState(currentOffsetY)

    AnimatedVisibility(
        visible = isTimerRunning || isAlarmPlaying,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(updatedX.roundToInt(), updatedY.roundToInt()) }
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onPositionChange(updatedX + dragAmount.x, updatedY + dragAmount.y)
                        }
                    }
            ) {
                MuscuCard(
                    borderColor = if (isAlarmPlaying) MuscuTheme.colors.error else Color.Transparent,
                    onClick = {
                        if (currentSessionId != -1L) {
                            navController.navigate("live/$currentSessionId")
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isAlarmPlaying) Icons.Default.NotificationsActive else Icons.Default.Timer, 
                            contentDescription = null, 
                            tint = if (isAlarmPlaying) MuscuTheme.colors.error else MuscuTheme.colors.primary
                        )
                        Column {
                            Text(
                                text = if (isAlarmPlaying) "FINI !" else "Repos", 
                                style = MuscuTheme.typography.labelSmall,
                                color = if (isAlarmPlaying) MuscuTheme.colors.error else MuscuTheme.colors.textSecondary
                            )
                            if (!isAlarmPlaying) {
                                Text(
                                    String.format(Locale.getDefault(), "%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                                    style = MuscuTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MuscuTheme.colors.primary
                                )
                            } else {
                                Text(
                                    "STOP",
                                    style = MuscuTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MuscuTheme.colors.error
                                )
                            }
                        }
                        IconButton(
                            onClick = { 
                                val action = if (isAlarmPlaying) "STOP_ALARM" else "CANCEL_TIMER"
                                context.startService(Intent(context, WorkoutTimerService::class.java).apply { this.action = action })
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = "Arrêter", 
                                modifier = Modifier.size(20.dp),
                                tint = if (isAlarmPlaying) MuscuTheme.colors.error else MuscuTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

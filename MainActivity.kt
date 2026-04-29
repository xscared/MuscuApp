package com.example.muscuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.muscuapp.ui.screens.AddExerciseScreen
import com.example.muscuapp.ui.screens.HomeScreen
import com.example.muscuapp.ui.screens.WorkoutDetailScreen
import com.example.muscuapp.ui.screens.LiveWorkoutScreen
import com.example.muscuapp.ui.theme.MuscuAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MuscuAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(onWorkoutClick = { id -> 
                                // On vérifie si c'est live ou non (on peut naviguer différemment si on veut)
                                navController.navigate("detail/$id") 
                            })
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
                            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
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
                }
            }
        }
    }
}

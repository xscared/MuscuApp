package com.example.muscuapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muscuapp.data.local.WorkoutWithExercisesAndSets
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onWorkoutClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val workouts by viewModel.workouts.collectAsState(initial = emptyList())
    
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    val workoutDays = remember(workouts) {
        workouts.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.session.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSet()
    }

    val workoutsOnSelectedDay = remember(selectedDate, workouts) {
        workouts.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.session.date
            cal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendrier") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newCal = currentMonth.clone() as Calendar
                    newCal.add(Calendar.MONTH, -1)
                    currentMonth = newCal
                }) {
                    Icon(Icons.Default.ChevronLeft, "Précédent")
                }
                Text(
                    text = monthFormatter.format(currentMonth.time).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    val newCal = currentMonth.clone() as Calendar
                    newCal.add(Calendar.MONTH, 1)
                    currentMonth = newCal
                }) {
                    Icon(Icons.Default.ChevronRight, "Suivant")
                }
            }

            // Days of week header
            val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
            Row(Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Calendar Grid
            val days = getDaysInMonth(currentMonth)
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().height(300.dp).padding(8.dp)
            ) {
                items(days) { date ->
                    if (date == null) {
                        Box(Modifier.aspectRatio(1f))
                    } else {
                        val isSelected = isSameDay(date, selectedDate)
                        val hasWorkout = workoutDays.contains(getStartOfDay(date).timeInMillis)
                        val isToday = isSameDay(date, Calendar.getInstance())

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { selectedDate = date },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = date.get(Calendar.DAY_OF_MONTH).toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasWorkout) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Sessions on selected day
            Text(
                text = "Séances du ${SimpleDateFormat("dd MMMM", Locale.getDefault()).format(selectedDate.time)}",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (workoutsOnSelectedDay.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune séance ce jour-là.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(workoutsOnSelectedDay) { workout ->
                        WorkoutItemSimple(
                            workout = workout,
                            onClick = { onWorkoutClick(workout.session.sessionId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutItemSimple(workout: WorkoutWithExercisesAndSets, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workout.session.title, style = MaterialTheme.typography.titleMedium)
                Text("${workout.exercises.size} exercices", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

fun getDaysInMonth(month: Calendar): List<Calendar?> {
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    
    // Adjust to Monday = 1
    var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2
    if (firstDayOfWeek < 0) firstDayOfWeek += 7
    
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalDays = mutableListOf<Calendar?>()
    
    for (i in 0 until firstDayOfWeek) {
        totalDays.add(null)
    }
    
    for (i in 1..daysInMonth) {
        val day = cal.clone() as Calendar
        day.set(Calendar.DAY_OF_MONTH, i)
        totalDays.add(day)
    }
    
    return totalDays
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun getStartOfDay(cal: Calendar): Calendar {
    val res = cal.clone() as Calendar
    res.set(Calendar.HOUR_OF_DAY, 0)
    res.set(Calendar.MINUTE, 0)
    res.set(Calendar.SECOND, 0)
    res.set(Calendar.MILLISECOND, 0)
    return res
}

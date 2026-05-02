package com.example.muscuapp.ui.screens

import androidx.compose.foundation.background
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
import com.example.muscuapp.ui.components.*
import com.example.muscuapp.ui.theme.MuscuTheme
import com.example.muscuapp.ui.viewmodel.ExerciseViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    MuscuScreen(
        title = "Calendrier",
        navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigationClick = onBack
    ) {
        // Sélecteur de mois personnalisé
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = currentMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        currentMonth = newCal
                    },
                    modifier = Modifier.background(MuscuTheme.colors.surfaceVariant, SquircleShape(3f))
                ) {
                    Icon(Icons.Default.ChevronLeft, null, tint = MuscuTheme.colors.textPrimary)
                }
                
                Text(
                    text = monthFormatter.format(currentMonth.time).uppercase(),
                    style = MuscuTheme.typography.titleMedium,
                    color = MuscuTheme.colors.primary,
                    fontWeight = FontWeight.Black
                )
                
                IconButton(
                    onClick = {
                        val newCal = currentMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        currentMonth = newCal
                    },
                    modifier = Modifier.background(MuscuTheme.colors.surfaceVariant, SquircleShape(3f))
                ) {
                    Icon(Icons.Default.ChevronRight, null, tint = MuscuTheme.colors.textPrimary)
                }
            }
        }

        // En-tête des jours
        item {
            val daysOfWeek = listOf("LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM")
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MuscuTheme.typography.labelSmall,
                        color = MuscuTheme.colors.textSecondary
                    )
                }
            }
        }

        // Grille du Calendrier
        item {
            val days = getDaysInMonth(currentMonth)
            Box(modifier = Modifier.height(320.dp).fillMaxWidth().padding(horizontal = 8.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
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
                                    .clip(SquircleShape(n = 2.5f))
                                    .background(
                                        when {
                                            isSelected -> MuscuTheme.colors.primary
                                            isToday -> MuscuTheme.colors.surfaceVariant
                                            else -> Color.Transparent
                                        }
                                    )
                                    .muscuClickable { selectedDate = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = date.get(Calendar.DAY_OF_MONTH).toString(),
                                        style = MuscuTheme.typography.bodyLarge,
                                        fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) MuscuTheme.colors.onPrimary else MuscuTheme.colors.textPrimary
                                    )
                                    if (hasWorkout) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MuscuTheme.colors.onPrimary else MuscuTheme.colors.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(16.dp), color = MuscuTheme.colors.divider)
        }

        // Liste des séances du jour sélectionné
        item {
            Text(
                text = "SÉANCES DU ${SimpleDateFormat("dd MMMM", Locale.getDefault()).format(selectedDate.time).uppercase()}",
                modifier = Modifier.padding(16.dp),
                style = MuscuTheme.typography.labelSmall,
                color = MuscuTheme.colors.textSecondary
            )
        }

        if (workoutsOnSelectedDay.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("REPOS.", style = MuscuTheme.typography.titleMedium, color = MuscuTheme.colors.divider)
                }
            }
        } else {
            items(workoutsOnSelectedDay) { workout ->
                MuscuWorkoutItem(
                    workout = workout,
                    onClick = { onWorkoutClick(workout.session.sessionId) }
                )
            }
        }
    }
}

private fun getDaysInMonth(calendar: Calendar): List<Calendar?> {
    val days = mutableListOf<Calendar?>()
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val padding = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
    
    repeat(padding) {
        days.add(null)
    }
    
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in 1..daysInMonth) {
        val day = cal.clone() as Calendar
        day.set(Calendar.DAY_OF_MONTH, i)
        days.add(day)
    }
    
    return days
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun getStartOfDay(calendar: Calendar): Calendar {
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal
}

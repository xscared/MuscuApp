package com.example.muscuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muscuapp.data.local.WorkoutWithExercisesAndSets
import com.example.muscuapp.ui.theme.MuscuTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MuscuWorkoutItem(
    workout: WorkoutWithExercisesAndSets,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    
    MuscuCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MuscuTheme.spacing.medium, vertical = MuscuTheme.spacing.small),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (workout.session.isLive) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MuscuTheme.colors.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = workout.session.title.uppercase(),
                        style = MuscuTheme.typography.titleMedium,
                        color = MuscuTheme.colors.textPrimary,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "${workout.exercises.size} EXERCICES // ${dateFormat.format(Date(workout.session.date)).uppercase()}",
                    style = MuscuTheme.typography.monoLabel,
                    color = MuscuTheme.colors.primary
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MuscuTheme.colors.divider,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

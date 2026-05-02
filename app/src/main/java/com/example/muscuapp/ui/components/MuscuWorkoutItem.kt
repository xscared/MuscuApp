package com.example.muscuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.muscuapp.data.local.WorkoutWithExercisesAndSets
import com.example.muscuapp.ui.theme.MuscuTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MuscuWorkoutItem(
    workout: WorkoutWithExercisesAndSets,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    
    MuscuCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MuscuTheme.spacing.medium, vertical = MuscuTheme.spacing.small),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(
                        if (workout.session.isLive) MuscuTheme.colors.primary
                        else MuscuTheme.colors.divider,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(MuscuTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.session.title,
                    style = MuscuTheme.typography.titleMedium,
                    color = MuscuTheme.colors.textPrimary
                )
                Text(
                    text = "${workout.exercises.size} exercices • ${dateFormat.format(Date(workout.session.date))}",
                    style = MuscuTheme.typography.bodyMedium,
                    color = MuscuTheme.colors.textSecondary
                )
            }
            
            if (workout.session.isLive) {
                Text(
                    text = "LIVE",
                    style = MuscuTheme.typography.labelSmall,
                    color = MuscuTheme.colors.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MuscuTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

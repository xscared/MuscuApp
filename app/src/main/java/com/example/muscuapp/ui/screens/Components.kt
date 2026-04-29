package com.example.muscuapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muscuapp.data.local.ExerciseEntity
import com.example.muscuapp.data.prefs.WeightUnit
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExerciseItem(
    exercise: ExerciseEntity, 
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (Boolean) -> Unit = {},
    weightUnit: WeightUnit = WeightUnit.KG,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    
    val displayWeight = if (weightUnit == WeightUnit.LBS) {
        exercise.weight * 2.20462f
    } else {
        exercise.weight
    }
    val unitLabel = if (weightUnit == WeightUnit.LBS) "lbs" else "kg"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onEdit?.invoke() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                            Text(exercise.category, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        if (exercise.isPR) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = Color(0xFFFFD700)) {
                                Icon(Icons.Default.TrendingUp, null, Modifier.size(12.dp), Color.Black)
                                Text(" RECORD !", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormat.format(Date(exercise.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = exercise.name, style = MaterialTheme.typography.titleLarge)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${exercise.sets} séries x ${exercise.reps} reps @ ${String.format(Locale.US, "%.1f", displayWeight)} $unitLabel",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (exercise.reps > 1) {
                        val oneRM = displayWeight / (1.0278 - 0.0278 * exercise.reps)
                        Text(
                            text = "1RM estimé: ${String.format(Locale.US, "%.1f", oneRM)} $unitLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    if (exercise.note.isNotBlank()) {
                        Text(
                            text = "Note: ${exercise.note}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onToggleFavorite(!exercise.isFavorite) }) {
                        Icon(
                            if (exercise.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favori",
                            tint = if (exercise.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (onMoveUp != null || onMoveDown != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                ) {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, "Monter", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, "Descendre", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

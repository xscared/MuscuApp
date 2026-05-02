package com.example.muscuapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.muscuapp.ui.theme.MuscuTheme

@Composable
fun MuscuSuccessCard(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    MuscuCard(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        borderColor = MuscuTheme.colors.primary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MuscuTheme.colors.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = title,
                style = MuscuTheme.typography.titleLarge,
                color = MuscuTheme.colors.primary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = message,
                style = MuscuTheme.typography.bodyLarge,
                color = MuscuTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MuscuButton(
                text = "CONTINUER",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

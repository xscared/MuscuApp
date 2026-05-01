package com.example.muscuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muscuapp.ui.theme.MuscuTheme

@Composable
fun MuscuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MuscuTheme.colors.primary,
    contentColor: Color = MuscuTheme.colors.onPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = MuscuTheme.spacing.large, vertical = MuscuTheme.spacing.small),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MuscuTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MuscuOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Version outlined si besoin
}

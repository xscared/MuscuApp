package com.example.muscuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.muscuapp.ui.theme.MuscuTheme

@Composable
fun MuscuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    
    val cardModifier = Modifier
        .shadow(elevation = 8.dp, shape = shape)
        .clip(shape)
        .background(MuscuTheme.colors.surface)
        .then(
            if (borderColor != Color.Transparent) {
                Modifier.border(1.dp, borderColor, shape)
            } else Modifier
        )
        .then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else Modifier
        )

    Box(
        modifier = modifier.then(cardModifier).padding(MuscuTheme.spacing.medium)
    ) {
        content()
    }
}

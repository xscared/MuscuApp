package com.example.muscuapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.muscuapp.ui.theme.MuscuTheme

/**
 * Une barre de progression linéaire fine et néon pour remplacer le CircularProgressIndicator.
 */
@Composable
fun MuscuLoadingBar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val offset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MuscuTheme.colors.divider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .offset(x = (offset * 200).dp) // Animation simple de va-et-vient
                .background(MuscuTheme.colors.primary, CircleShape)
        )
    }
}

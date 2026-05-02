package com.example.muscuapp.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView

/**
 * Un modificateur personnalisé qui remplace le Ripple de Material.
 * - Réduction d'échelle (s'enfonce au clic)
 * - Retour haptique (vibration légère)
 * - Pas de vaguelette visuelle Google
 */
fun Modifier.muscuClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    
    // Animation d'échelle (scale)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 1500f),
        label = "scale"
    )
    
    // Animation de luminosité (alpha/glow)
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        label = "alpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(false)
                    isPressed = true
                    // Petit retour haptique au toucher
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Supprime le Ripple Material
            enabled = enabled,
            onClick = {
                // Retour haptique plus sec au clic final
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        )
}

/**
 * Un bouton spécial qui utilise l'interaction Muscu
 */
@Composable
fun MuscuInteractionWrapper(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.muscuClickable(onClick = onClick)
    ) {
        content()
    }
}

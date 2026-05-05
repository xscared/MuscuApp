package com.example.muscuapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.muscuapp.ui.theme.MuscuTheme

/**
 * Une alternative haut de gamme aux Dialogs et DropdownMenus.
 * S'affiche depuis le bas avec une animation fluide.
 */
@Composable
fun MuscuActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Overlay sombre
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MuscuTheme.colors.surface)
                    .navigationBarsPadding()
                    .padding(MuscuTheme.spacing.large)
            ) {
                // Petit indicateur de drag
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MuscuTheme.colors.divider)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                )

                if (title != null) {
                    Text(
                        text = title.uppercase(),
                        style = MuscuTheme.typography.titleMedium,
                        color = MuscuTheme.colors.textPrimary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    HorizontalDivider(color = MuscuTheme.colors.divider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                content()
            }
        }
    }
}

@Composable
fun MuscuActionItem(
    label: String,
    icon: ImageVector? = null,
    color: Color = MuscuTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .muscuClickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = label,
            style = MuscuTheme.typography.bodyLarge,
            color = color
        )
    }
}

package com.example.muscuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.muscuapp.ui.theme.MuscuTheme

/**
 * Un champ de texte minimaliste et technique, sans les bordures Material.
 */
@Composable
fun MuscuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MuscuTheme.colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        textStyle = MuscuTheme.typography.bodyLarge.copy(
            color = MuscuTheme.colors.textPrimary,
            fontFamily = FontFamily.Monospace
        ),
        cursorBrush = SolidColor(MuscuTheme.colors.primary),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder.uppercase(),
                        style = MuscuTheme.typography.labelSmall,
                        color = MuscuTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        }
    )
}

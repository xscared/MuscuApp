package com.example.muscuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.muscuapp.ui.theme.MuscuTheme

@Composable
fun MuscuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val shape = RoundedCornerShape(8.dp)
    
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .background(MuscuTheme.colors.surface)
            .border(
                width = 1.dp,
                color = if (isFocused) MuscuTheme.colors.primary else MuscuTheme.colors.divider,
                shape = shape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        textStyle = MuscuTheme.typography.bodyLarge.copy(
            color = MuscuTheme.colors.textPrimary
        ),
        cursorBrush = SolidColor(MuscuTheme.colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && !isFocused) {
                    // Placeholder optionnel ici si besoin
                }
                innerTextField()
            }
        }
    )
}

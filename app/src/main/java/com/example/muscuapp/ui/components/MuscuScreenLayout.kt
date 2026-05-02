package com.example.muscuapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muscuapp.ui.theme.MuscuTheme

@Composable
fun MuscuScreen(
    title: String,
    navigationIcon: ImageVector? = null,
    onNavigationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable (BoxScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    val scrollState = rememberLazyListState()
    
    // Header configuration
    val headerHeight = 160.dp
    val minHeaderHeight = 72.dp
    
    val scrollOffset = remember { derivedStateOf { scrollState.firstVisibleItemScrollOffset } }
    val firstItemIndex = remember { derivedStateOf { scrollState.firstVisibleItemIndex } }
    
    // Determine collapse state
    val isCollapsed = remember {
        derivedStateOf {
            firstItemIndex.value > 0 || scrollOffset.value > 120
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MuscuTheme.colors.background)
    ) {
        // Main list
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = headerHeight,
                bottom = if (bottomBar != null) 120.dp else 32.dp
            )
        ) {
            content()
        }

        // Custom Header with transition
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCollapsed.value) minHeaderHeight else headerHeight)
                .background(
                    if (isCollapsed.value) MuscuTheme.colors.background.copy(alpha = 0.98f)
                    else Color.Transparent
                )
                .statusBarsPadding()
                .padding(horizontal = MuscuTheme.spacing.medium)
        ) {
            // Dynamic Title
            Text(
                text = title.uppercase(),
                style = MuscuTheme.typography.titleLarge,
                fontSize = if (isCollapsed.value) 20.sp else 36.sp,
                fontWeight = FontWeight.Black,
                color = MuscuTheme.colors.textPrimary,
                modifier = Modifier
                    .align(if (isCollapsed.value) Alignment.Center else Alignment.BottomStart)
                    .padding(bottom = if (isCollapsed.value) 0.dp else 16.dp)
                    .graphicsLayer {
                        // Subtle scaling or fading could go here
                    }
            )

            // Navigation Icon
            if (navigationIcon != null) {
                IconButton(
                    onClick = onNavigationClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(MuscuTheme.colors.surface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Icon(navigationIcon, null, tint = MuscuTheme.colors.textPrimary)
                }
            }

            // Right Actions
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }

        // Floating Action Pill (Bottom Bar)
        if (bottomBar != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MuscuTheme.colors.primary,
                                MuscuTheme.colors.primary.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MuscuTheme.colors.onPrimary
                ) {
                    bottomBar()
                }
            }
        }
    }
}

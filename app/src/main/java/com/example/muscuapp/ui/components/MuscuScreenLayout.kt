package com.example.muscuapp.ui.components

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
    
    // Configuration du Header (agressif)
    val headerHeight = 180.dp
    val minHeaderHeight = 72.dp
    
    val scrollOffset = remember { derivedStateOf { scrollState.firstVisibleItemScrollOffset } }
    val firstItemIndex = remember { derivedStateOf { scrollState.firstVisibleItemIndex } }
    
    val isCollapsed = remember {
        derivedStateOf {
            firstItemIndex.value > 0 || scrollOffset.value > 100
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MuscuTheme.colors.background)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = headerHeight,
                bottom = if (bottomBar != null) 120.dp else 40.dp
            )
        ) {
            content()
        }

        // Header Custom avec dégradé subtil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCollapsed.value) minHeaderHeight else headerHeight)
                .background(
                    if (isCollapsed.value) MuscuTheme.colors.background.copy(alpha = 0.95f)
                    else Color.Transparent
                )
                .statusBarsPadding()
                .padding(horizontal = MuscuTheme.spacing.medium)
        ) {
            // Titre Display Large (Agressif)
            Text(
                text = title.uppercase(),
                style = if (isCollapsed.value) MuscuTheme.typography.titleLarge else MuscuTheme.typography.displayLarge,
                color = MuscuTheme.colors.textPrimary,
                modifier = Modifier
                    .align(if (isCollapsed.value) Alignment.Center else Alignment.BottomStart)
                    .padding(bottom = if (isCollapsed.value) 0.dp else 24.dp)
                    .graphicsLayer {
                        // On réduit légèrement l'opacité au scroll pour le titre géant
                        if (!isCollapsed.value) {
                            alpha = (1f - (scrollOffset.value / 250f)).coerceIn(0f, 1f)
                        }
                    }
            )

            // Actions & Navigation
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navigationIcon != null) {
                    IconButton(
                        onClick = onNavigationClick,
                        modifier = Modifier.background(MuscuTheme.colors.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(navigationIcon, null, tint = MuscuTheme.colors.textPrimary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }

        // Barre d'action Pill
        if (bottomBar != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(SquircleShape(n = 3.5f))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MuscuTheme.colors.primary,
                                MuscuTheme.colors.primary.copy(alpha = 0.85f)
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

package com.zipper.compose.assetguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.zipper.compose.assetguard.ui.theme.overviewCardGradient
import com.zipper.compose.assetguard.ui.theme.spacing

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val resolvedGradient = gradient ?: overviewCardGradient()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(resolvedGradient)
            .padding(MaterialTheme.spacing.lg),
        content = content
    )
}

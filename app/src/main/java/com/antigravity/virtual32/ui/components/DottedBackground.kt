package com.antigravity.virtual32.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.antigravity.virtual32.ui.theme.CozyBackground
import com.antigravity.virtual32.ui.theme.CozyDotColor

/**
 * Procedural dotted background providing the warm, cozy retro aesthetic from the project spec.
 * Uses drawBehind for zero-overhead canvas rendering without composing child nodes.
 */
fun Modifier.dottedBackground(
    backgroundColor: Color = CozyBackground,
    dotColor: Color = CozyDotColor,
    dotRadius: Dp = 1.4.dp,
    spacing: Dp = 22.dp
): Modifier = this.drawBehind {
    drawRect(color = backgroundColor)

    val spacingPx = spacing.toPx()
    val radiusPx = dotRadius.toPx()

    val columns = (size.width / spacingPx).toInt() + 1
    val rows = (size.height / spacingPx).toInt() + 1

    for (x in 0..columns) {
        for (y in 0..rows) {
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(x * spacingPx, y * spacingPx)
            )
        }
    }
}

@Composable
fun DottedBackgroundBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .dottedBackground(),
        content = content
    )
}

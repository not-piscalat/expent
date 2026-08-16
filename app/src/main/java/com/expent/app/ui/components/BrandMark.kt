package com.expent.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MarkBackgroundTop = Color(0xFF1C0940)
private val MarkBackgroundMid = Color(0xFF2A1060)
private val MarkBackgroundDeep = Color(0xFF160830)
private val MarkGrid = Color(0xFF8B5CF6)
private val MarkBaseline = Color(0xFF6D28D9)
private val MarkTrend = Color(0xFFC4B5FD)
private val MarkDot = Color(0xFFEDE9FE)
private val MarkRing = Color(0xFFDDD6FE)

/**
 * Expent's brand mark: the growth chart from the launcher icon — three
 * ascending violet bars with a dashed trendline and a glow dot on the tallest.
 * Drawn in a Canvas so it renders crisply at any size and in any context.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp
) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        val s = size.minDimension / 108f
        // Deep violet backdrop with a radial glow, like the launcher.
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(MarkBackgroundTop, MarkBackgroundMid, MarkBackgroundDeep),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF6D28D9).copy(alpha = 0.35f),
                    Color(0xFF6D28D9).copy(alpha = 0f)
                ),
                center = Offset(0.62f * size.width, 0.52f * size.height),
                radius = 0.5f * size.minDimension
            ),
            center = Offset(0.62f * size.width, 0.52f * size.height),
            radius = 0.5f * size.minDimension
        )

        // Chart grid: three faint dashed lines behind the bars.
        val dash = PathEffect.dashPathEffect(floatArrayOf(2.3f * s, 3.45f * s))
        listOf(28.7f, 44.8f, 62.05f).forEach { gy ->
            drawLine(
                color = MarkGrid.copy(alpha = 0.12f),
                start = Offset(22.95f * s, gy * s),
                end = Offset(85.05f * s, gy * s),
                strokeWidth = 1f * s,
                pathEffect = dash
            )
        }
        // Baseline the bars stand on.
        drawLine(
            color = MarkBaseline.copy(alpha = 0.35f),
            start = Offset(24.1f * s, 79.3f * s),
            end = Offset(83.9f * s, 79.3f * s),
            strokeWidth = 1.5f * s,
            cap = StrokeCap.Round
        )

        // The three bars, shortest to tallest.
        drawMarkBar(
            left = 25.25f, top = 58.6f, right = 39.05f, bottom = 79.3f, s = s,
            topColor = Color(0xFF7C3AED), bottomColor = Color(0xFF4C1D95)
        )
        drawMarkBar(
            left = 47.1f, top = 44.8f, right = 60.9f, bottom = 79.3f, s = s,
            topColor = Color(0xFFA78BFA), bottomColor = Color(0xFF5B21B6)
        )
        drawMarkBar(
            left = 68.95f, top = 28.7f, right = 82.75f, bottom = 79.3f, s = s,
            topColor = Color(0xFFDDD6FE), bottomColor = Color(0xFF6D28D9)
        )

        // Soft halo behind the glow dot on the tallest bar.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MarkDot.copy(alpha = 0.45f), MarkDot.copy(alpha = 0f)),
                center = Offset(75.85f * s, 28.7f * s),
                radius = 10.35f * s
            ),
            center = Offset(75.85f * s, 28.7f * s),
            radius = 10.35f * s
        )

        // Highlight caps on each bar.
        drawCap(left = 27.55f, top = 60.9f, s = s, alpha = 0.12f)
        drawCap(left = 49.4f, top = 47.1f, s = s, alpha = 0.12f)
        drawCap(left = 71.25f, top = 31f, s = s, alpha = 0.15f)

        // The dashed trendline across the bar tops.
        val trend = Path().apply {
            moveTo(32.15f * s, 58.6f * s)
            lineTo(54f * s, 44.8f * s)
            lineTo(75.85f * s, 28.7f * s)
        }
        drawPath(
            path = trend,
            color = MarkTrend.copy(alpha = 0.3f),
            style = Stroke(
                width = 1.38f * s,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = dash
            )
        )

        // The glow dot: ring, then the bright core.
        drawCircle(
            color = MarkRing.copy(alpha = 0.3f),
            center = Offset(75.85f * s, 28.7f * s),
            radius = 6.325f * s,
            style = Stroke(width = 1.15f * s)
        )
        drawCircle(
            color = MarkDot,
            center = Offset(75.85f * s, 28.7f * s),
            radius = 4.025f * s
        )
    }
}

/** A rounded violet bar spanning the given 108-space box. */
private fun DrawScope.drawMarkBar(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    s: Float,
    topColor: Color,
    bottomColor: Color
) {
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
        topLeft = Offset(left * s, top * s),
        size = Size((right - left) * s, (bottom - top) * s),
        cornerRadius = CornerRadius(4.6f * s)
    )
}

/** The tiny white highlight pill near the top of a bar. */
private fun DrawScope.drawCap(left: Float, top: Float, s: Float, alpha: Float) {
    drawRoundRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(left * s, top * s),
        size = Size(9.2f * s, 3.45f * s),
        cornerRadius = CornerRadius(1.725f * s)
    )
}

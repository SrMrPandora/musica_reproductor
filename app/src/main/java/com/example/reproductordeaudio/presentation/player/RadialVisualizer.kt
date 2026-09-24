package com.example.reproductordeaudio.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadialVisualizer(
    amplitudesProvider: () -> FloatArray,
    barColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 380.dp,
    innerRadiusPx: Float,
    maxBarLengthPx: Float
) {
    Canvas(modifier = modifier.size(size)) {
        val amplitudes = amplitudesProvider()
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val bandCount = amplitudes.size.coerceAtMost(128)
        if (bandCount == 0) return@Canvas

        val angleStep = (2 * Math.PI / bandCount).toFloat()

        for (i in 0 until bandCount) {
            val angle = i * angleStep - (Math.PI / 2).toFloat()
            val amp = amplitudes[i].coerceIn(0.05f, 1f)
            val barLength = maxBarLengthPx * amp

            val startX = center.x + innerRadiusPx * cos(angle)
            val startY = center.y + innerRadiusPx * sin(angle)

            val endX = center.x + (innerRadiusPx + barLength) * cos(angle)
            val endY = center.y + (innerRadiusPx + barLength) * sin(angle)

            drawLine(
                color = barColor.copy(alpha = (0.4f + amp * 0.6f).coerceAtMost(1f)),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

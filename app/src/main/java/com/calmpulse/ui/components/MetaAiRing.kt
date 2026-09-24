package com.calmpulse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.calmpulse.ui.theme.MetaAiRingColors

/**
 * Anel Gradiente Holográfico do Meta AI de altíssima performance.
 * Utiliza graphicsLayer para rotação na GPU sem disparar recomposições contínuas de CPU.
 * Desliga a animação quando inativo para economizar bateria e renderização.
 */
@Composable
fun MetaAiRing(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    strokeWidth: Dp = 3.dp,
    isPulsing: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    val rotationAngle = if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "MetaAiRingRotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotationAngle"
        )
        rotation
    } else {
        45f
    }

    val sweepGradient = remember {
        Brush.sweepGradient(colors = MetaAiRingColors)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    rotationZ = rotationAngle
                }
        ) {
            val strokePx = strokeWidth.toPx()
            drawCircle(
                brush = sweepGradient,
                radius = (size.toPx() - strokePx) / 2f,
                style = Stroke(width = strokePx)
            )
        }

        content?.invoke()
    }
}

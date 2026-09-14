package com.calmpulse.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmpulse.ui.theme.MistBlue
import com.calmpulse.ui.theme.PulseGlow
import com.calmpulse.ui.theme.SageGreen
import com.calmpulse.ui.theme.SageGreenLight
import com.calmpulse.ui.theme.SoftLavender
import com.calmpulse.ui.theme.TextPrimary
import com.calmpulse.ui.theme.TextSecondary
import kotlinx.coroutines.delay

enum class BreathingPhase(
    val title: String,
    val subtitle: String,
    val durationSeconds: Int,
    val targetScale: Float
) {
    INHALE("Inspire", "Pelo nariz, devagar...", 4, 1.45f),
    HOLD("Segure", "Mantenha o ar suavemente...", 7, 1.45f),
    EXHALE("Expire", "Solte todo o ar pela boca...", 8, 1.0f)
}

@Composable
fun BreathingCircle(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    var currentPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var secondsRemaining by remember { mutableStateOf(currentPhase.durationSeconds) }

    // Loop do ciclo 4-7-8 rítmico
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        while (true) {
            // Fase 1: Inspire (4s)
            currentPhase = BreathingPhase.INHALE
            for (sec in currentPhase.durationSeconds downTo 1) {
                secondsRemaining = sec
                delay(1000)
            }

            // Fase 2: Retenha (7s)
            currentPhase = BreathingPhase.HOLD
            for (sec in currentPhase.durationSeconds downTo 1) {
                secondsRemaining = sec
                delay(1000)
            }

            // Fase 3: Expire (8s)
            currentPhase = BreathingPhase.EXHALE
            for (sec in currentPhase.durationSeconds downTo 1) {
                secondsRemaining = sec
                delay(1000)
            }
        }
    }

    // Animação contínua da escala baseada na fase
    val animationDuration = when (currentPhase) {
        BreathingPhase.INHALE -> 4000
        BreathingPhase.HOLD -> 500
        BreathingPhase.EXHALE -> 8000
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) currentPhase.targetScale else 1.0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "BreathingScale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Halo de brilho externo (Glow)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(animatedScale * 1.15f)
                    .clip(CircleShape)
                    .background(PulseGlow)
            )

            // Círculo intermediário com gradiente relaxante
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(animatedScale * 1.05f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SoftLavender.copy(alpha = 0.45f), MistBlue.copy(alpha = 0.6f))
                        )
                    )
            )

            // Círculo principal onde o texto reside
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SageGreenLight, SageGreen)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentPhase.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${secondsRemaining}s",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instrução guiada em texto abaixo do círculo
        Text(
            text = currentPhase.subtitle,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Técnica de Respiração 4-7-8",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary
            ),
            textAlign = TextAlign.Center
        )
    }
}

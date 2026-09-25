package com.calmpulse.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmpulse.ui.theme.DarkBackground
import com.calmpulse.ui.theme.MetaAiBlue
import com.calmpulse.ui.theme.MetaAiCyan
import com.calmpulse.ui.theme.MetaAiPink
import com.calmpulse.ui.theme.MetaAiPurple
import com.calmpulse.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay

enum class BreathingPhase(
    val title: String,
    val subtitle: String,
    val durationSeconds: Int,
    val targetScale: Float
) {
    INHALE("Inspire", "Puxe o ar suavemente pelo nariz", 4, 1.18f),
    HOLD("Segure", "Mantenha o ar nos pulmões com calma", 7, 1.18f),
    EXHALE("Expire", "Solte todo o ar devagar pela boca", 8, 0.88f)
}

/**
 * Círculo de Respiração 4-7-8 com estética etérea inspirada no Meta AI e design moderno de meditação.
 */
@Composable
fun BreathingCircle(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    onBackToChat: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hapticHelper = remember { com.calmpulse.util.HapticFeedbackHelper(context) }
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val prefs = remember { context.getSharedPreferences("calmpulse_prefs", android.content.Context.MODE_PRIVATE) }
    var isVibrationEnabled by remember {
        mutableStateOf(prefs.getBoolean("haptic_feedback_enabled", true))
    }

    var isRunning by remember { mutableStateOf(isActive) }
    var currentPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var secondsRemaining by remember { mutableStateOf(currentPhase.durationSeconds) }
    var cycleCount by remember { mutableStateOf(1) }

    // Ciclo temporal com feedback tátil suave (se ativado)
    LaunchedEffect(isRunning, isVibrationEnabled) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning) {
            // 1. Inspire (4s)
            currentPhase = BreathingPhase.INHALE
            if (isVibrationEnabled) hapticHelper.vibrateInhale()
            for (sec in currentPhase.durationSeconds downTo 1) {
                secondsRemaining = sec
                delay(1000)
            }

            // 2. Segure (7s)
            currentPhase = BreathingPhase.HOLD
            if (isVibrationEnabled) hapticHelper.vibrateHold()
            for (sec in currentPhase.durationSeconds downTo 1) {
                secondsRemaining = sec
                delay(1000)
            }

            // 3. Expire (8s)
            currentPhase = BreathingPhase.EXHALE
            if (isVibrationEnabled) hapticHelper.vibrateExhale()
            for (sec in currentPhase.durationSeconds downTo 1) {
                secondsRemaining = sec
                delay(1000)
            }

            cycleCount++
        }
    }

    val animationDuration = when (currentPhase) {
        BreathingPhase.INHALE -> 4000
        BreathingPhase.HOLD -> 600
        BreathingPhase.EXHALE -> 8000
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isRunning) currentPhase.targetScale else 1.0f,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "BreathingScale"
    )

    // Gradiente holográfico Meta AI com transparências etéreas
    val ringGradient = remember {
        Brush.sweepGradient(
            colors = listOf(
                MetaAiBlue,
                MetaAiPurple,
                MetaAiPink,
                MetaAiCyan,
                WhatsAppGreen,
                MetaAiBlue
            )
        )
    }

    val innerOrbGradient = remember(isDark) {
        if (isDark) {
            Brush.radialGradient(
                colors = listOf(
                    MetaAiCyan.copy(alpha = 0.25f),
                    MetaAiPurple.copy(alpha = 0.15f),
                    Color(0xFF1F2C34).copy(alpha = 0.85f)
                )
            )
        } else {
            Brush.radialGradient(
                colors = listOf(
                    MetaAiCyan.copy(alpha = 0.35f),
                    MetaAiPurple.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.90f)
                )
            )
        }
    }

    val primaryText = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21)
    val secondaryText = if (isDark) Color(0xFF8696A0) else Color(0xFF667781)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Indicador do ciclo atual
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF1F2C34) else Color(0xFFE2E8F0))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Ciclo $cycleCount • Técnica 4-7-8",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = WhatsAppGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contêiner principal com dimensões controladas (evita sobreposição)
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. Halo de Brilho Translúcido Externo
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = animatedScale * 1.05f
                        scaleY = animatedScale * 1.05f
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MetaAiCyan.copy(alpha = if (isDark) 0.20f else 0.18f),
                                MetaAiPurple.copy(alpha = if (isDark) 0.10f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Anel Holográfico Dinâmico do Meta AI
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
            ) {
                drawCircle(
                    brush = ringGradient,
                    radius = size.minDimension / 2f - 4.dp.toPx(),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Esfera Central Translúcida (Glassmorphism Acolhedor)
            Box(
                modifier = Modifier
                    .size(172.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .clip(CircleShape)
                    .background(innerOrbGradient),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Badge da Fase com cor de acento
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = currentPhase.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = WhatsAppGreen,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Número Grande de Segundos
                    Text(
                        text = "$secondsRemaining",
                        style = MaterialTheme.typography.displayLarge.copy(
                            color = primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp
                        )
                    )

                    Text(
                        text = "segundos",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Instrução guiada com tipografia equilibrada
        Text(
            text = currentPhase.subtitle,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = primaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Etapas em pílulas sincronizadas
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreathingStepChip(
                stepText = "Inspire 4s",
                isActive = currentPhase == BreathingPhase.INHALE,
                isDark = isDark
            )
            BreathingStepChip(
                stepText = "Segure 7s",
                isActive = currentPhase == BreathingPhase.HOLD,
                isDark = isDark
            )
            BreathingStepChip(
                stepText = "Expire 8s",
                isActive = currentPhase == BreathingPhase.EXHALE,
                isDark = isDark
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Controles de Ação na base
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão de Pausar / Retomar exercício
            IconButton(
                onClick = { isRunning = !isRunning },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1F2C34) else Color(0xFFE2E8F0))
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pausar" else "Continuar",
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Botão de Alternar Vibração Sensorial (Haptics On/Off)
            IconButton(
                onClick = {
                    val newState = !isVibrationEnabled
                    isVibrationEnabled = newState
                    prefs.edit().putBoolean("haptic_feedback_enabled", newState).apply()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isVibrationEnabled) WhatsAppGreen.copy(alpha = 0.18f)
                        else if (isDark) Color(0xFF1F2C34) else Color(0xFFE2E8F0)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = if (isVibrationEnabled) "Desativar vibração sensorial" else "Ativar vibração sensorial",
                    tint = if (isVibrationEnabled) WhatsAppGreen else if (isDark) Color(0xFF8696A0) else Color(0xFF667781),
                    modifier = Modifier.size(22.dp)
                )
            }

            if (onBackToChat != null) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onBackToChat,
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Forum,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voltar ao Chat",
                        color = WhatsAppGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BreathingStepChip(
    stepText: String,
    isActive: Boolean,
    isDark: Boolean
) {
    val bg = if (isActive) {
        WhatsAppGreen
    } else {
        if (isDark) Color(0xFF1F2C34) else Color(0xFFE2E8F0)
    }
    val textColor = if (isActive) {
        Color.White
    } else {
        if (isDark) Color(0xFF8696A0) else Color(0xFF667781)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stepText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 11.sp
            )
        )
    }
}

package com.calmpulse.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmpulse.data.model.ChatMessage
import com.calmpulse.data.model.MessageSender
import com.calmpulse.ui.theme.DarkBackground
import com.calmpulse.ui.theme.DarkChatBubbleAi
import com.calmpulse.ui.theme.DarkChatBubbleUser
import com.calmpulse.ui.theme.DarkTextPrimary
import com.calmpulse.ui.theme.DarkTextSecondary
import com.calmpulse.ui.theme.TextPrimary
import com.calmpulse.ui.theme.TextSecondary
import com.calmpulse.ui.theme.WhatsAppAiBubbleLight
import com.calmpulse.ui.theme.WhatsAppBlueCheck
import com.calmpulse.ui.theme.WhatsAppUserBubbleLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Balão de mensagem oficial estilo WhatsApp iOS 2025 com suporte ao Meta AI.
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    isSpeakingThisMessage: Boolean = false,
    onSpeakClick: (String) -> Unit = {},
    onStopSpeakClick: () -> Unit = {}
) {
    val isUser = message.sender == MessageSender.USER
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val copyAction = {
        if (message.text.isNotBlank()) {
            clipboardManager.setText(AnnotatedString(message.text))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(context, "Mensagem copiada!", Toast.LENGTH_SHORT).show()
        }
    }

    // Cores de fundo dos balões WhatsApp iOS 2025
    val bubbleColor = if (isUser) {
        if (isDark) DarkChatBubbleUser else WhatsAppUserBubbleLight
    } else {
        if (isDark) DarkChatBubbleAi else WhatsAppAiBubbleLight
    }

    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val timeColor = if (isDark) DarkTextSecondary else TextSecondary

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    // Formato com cantos arredondados clássicos do WhatsApp iOS
    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 3.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 3.dp,
            bottomEnd = 16.dp
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 54.dp else 12.dp,
                end = if (isUser) 12.dp else 54.dp,
                top = 3.dp,
                bottom = 3.dp
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (isDark) 0.5.dp else 1.dp,
                    shape = bubbleShape,
                    clip = false
                )
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                // Cabeçalho discreto da IA com o anel Meta AI
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        MetaAiRing(
                            size = 14.dp,
                            strokeWidth = 2.dp,
                            isPulsing = message.isStreaming
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Meta AI • CalmPulse",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                // Conteúdo da Mensagem ou Animação de Streaming
                if (message.text.isBlank() && message.isStreaming) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        TypingDotsIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "digitando...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = timeColor,
                                fontSize = 13.sp
                            )
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Rodapé do balão: Horário, Ações de Áudio (TTS) e Checks duplos
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão Copiar Texto para respostas da IA
                    if (!isUser && message.text.isNotBlank() && !message.isStreaming) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar mensagem",
                            tint = timeColor.copy(alpha = 0.75f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { copyAction() }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Botão de Áudio (TTS) para respostas da IA
                    if (!isUser && message.text.isNotBlank() && !message.isStreaming) {
                        Icon(
                            imageVector = if (isSpeakingThisMessage) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeakingThisMessage) "Mutar áudio" else "Ouvir áudio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    if (isSpeakingThisMessage) onStopSpeakClick() else onSpeakClick(message.text)
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Carimbo de horário WhatsApp
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = timeColor,
                            fontSize = 11.sp
                        )
                    )

                    // Checks duplos azuis do WhatsApp iOS para mensagens enviadas
                    if (isUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mensagem lida",
                            tint = WhatsAppBlueCheck,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Indicador animado de 3 pontos pulsantes ("digitando..."), estilo WhatsApp.
 */
@Composable
private fun TypingDotsIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "dots")
    val dot1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = dot1))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = dot2))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = dot3))
        )
    }
}

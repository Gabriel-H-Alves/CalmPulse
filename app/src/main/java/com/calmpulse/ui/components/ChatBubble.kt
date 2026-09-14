package com.calmpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmpulse.data.model.ChatMessage
import com.calmpulse.data.model.MessageSender
import com.calmpulse.ui.theme.ChatBubbleAi
import com.calmpulse.ui.theme.ChatBubbleUser
import com.calmpulse.ui.theme.DarkBackground
import com.calmpulse.ui.theme.DarkChatBubbleAi
import com.calmpulse.ui.theme.DarkChatBubbleUser

@Composable
fun ChatBubble(
    message: ChatMessage,
    isSpeakingThisMessage: Boolean = false,
    onSpeakClick: (String) -> Unit = {},
    onStopSpeakClick: () -> Unit = {}
) {
    val isUser = message.sender == MessageSender.USER
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    // Cores dinâmicas de acordo com o tema ativo
    val bubbleColor = if (isUser) {
        if (isDark) DarkChatBubbleUser else ChatBubbleUser
    } else {
        if (isDark) DarkChatBubbleAi else ChatBubbleAi
    }

    // Alinhamento: Usuário à direita, IA à esquerda
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val bubbleShape = if (isUser) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        }

        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(bubbleColor)
                .then(
                    if (!isUser) {
                        Modifier.border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.35f else 0.5f),
                            shape = bubbleShape
                        )
                    } else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(0.80f)
                    ) {
                        Text(
                            text = "CalmPulse",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        )

                        // Botão de Áudio (TTS) no balão da IA
                        if (message.text.isNotBlank() && !message.isStreaming) {
                            Icon(
                                imageVector = if (isSpeakingThisMessage) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Ouvir mensagem",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        if (isSpeakingThisMessage) onStopSpeakClick() else onSpeakClick(message.text)
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Conteúdo da Mensagem (renderizado em tempo real no streaming)
                if (message.text.isBlank() && message.isStreaming) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pensando com calma...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

package com.calmpulse.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmpulse.data.model.UpdateInfo
import com.calmpulse.ui.theme.WhatsAppGreen
import com.calmpulse.ui.theme.WhatsAppGreenDark
import com.calmpulse.ui.theme.WhatsAppGreenLight

/**
 * Estado da UI do dialog de atualização.
 */
sealed class UpdateDialogState {
    /** Mostrando info da nova versão, aguardando ação do usuário */
    data class Available(val updateInfo: UpdateInfo) : UpdateDialogState()
    /** Download em andamento */
    data class Downloading(val progress: Int) : UpdateDialogState()
    /** Download concluído, pronto para instalar */
    object ReadyToInstall : UpdateDialogState()
    /** Erro durante o processo */
    data class Error(val message: String) : UpdateDialogState()
}

/**
 * Dialog de atualização com visual premium estilo CalmPulse.
 */
@Composable
fun UpdateDialog(
    state: UpdateDialogState,
    currentVersionName: String,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            // Só permite fechar se não estiver baixando
            if (state !is UpdateDialogState.Downloading) {
                onDismiss()
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            UpdateIcon(state)
        },
        title = {
            Text(
                text = when (state) {
                    is UpdateDialogState.Available -> "Atualização Disponível!"
                    is UpdateDialogState.Downloading -> "Baixando..."
                    is UpdateDialogState.ReadyToInstall -> "Download Concluído!"
                    is UpdateDialogState.Error -> "Erro na Atualização"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is UpdateDialogState.Available -> {
                        AvailableContent(
                            updateInfo = state.updateInfo,
                            currentVersionName = currentVersionName
                        )
                    }
                    is UpdateDialogState.Downloading -> {
                        DownloadingContent(progress = state.progress)
                    }
                    is UpdateDialogState.ReadyToInstall -> {
                        Text(
                            text = "O download foi concluído com sucesso.\nToque em \"Instalar\" para atualizar o app.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is UpdateDialogState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateDialogState.Available -> {
                    Button(
                        onClick = onUpdate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Atualizar", fontWeight = FontWeight.SemiBold)
                    }
                }
                is UpdateDialogState.Downloading -> {
                    // Sem botão de confirmação durante download
                }
                is UpdateDialogState.ReadyToInstall -> {
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Instalar", fontWeight = FontWeight.SemiBold)
                    }
                }
                is UpdateDialogState.Error -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fechar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        dismissButton = {
            when (state) {
                is UpdateDialogState.Available -> {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Depois")
                    }
                }
                else -> { /* Sem botão dismiss nos outros estados */ }
            }
        }
    )
}

@Composable
private fun UpdateIcon(state: UpdateDialogState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .then(
                if (state is UpdateDialogState.Downloading) {
                    Modifier.scale(pulseScale)
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(WhatsAppGreenLight, WhatsAppGreen, WhatsAppGreenDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (state) {
                is UpdateDialogState.ReadyToInstall -> Icons.Rounded.CheckCircle
                is UpdateDialogState.Error -> Icons.Rounded.ErrorOutline
                else -> Icons.Rounded.SystemUpdate
            },
            contentDescription = "Update",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun AvailableContent(
    updateInfo: UpdateInfo,
    currentVersionName: String
) {
    // Badges de versão
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VersionBadge(
            label = "Atual",
            version = currentVersionName,
            isNew = false
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "→",
            style = MaterialTheme.typography.titleLarge,
            color = WhatsAppGreen
        )

        Spacer(modifier = Modifier.width(12.dp))

        VersionBadge(
            label = "Nova",
            version = updateInfo.versionName,
            isNew = true
        )
    }

    if (updateInfo.releaseNotes.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Novidades:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = updateInfo.releaseNotes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 5
        )
    }
}

@Composable
private fun VersionBadge(
    label: String,
    version: String,
    isNew: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isNew) {
                        Brush.linearGradient(
                            listOf(WhatsAppGreenLight, WhatsAppGreen)
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "v$version",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isNew) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun DownloadingContent(progress: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (progress > 0) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = WhatsAppGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$progress%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WhatsAppGreen
            )
        } else {
            CircularProgressIndicator(
                color = WhatsAppGreen,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Aguarde, baixando a atualização...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package com.calmpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmpulse.ui.theme.DarkBackground
import com.calmpulse.ui.theme.DarkSurfaceCard
import com.calmpulse.ui.theme.DarkTextPrimary
import com.calmpulse.ui.theme.DarkTextSecondary
import com.calmpulse.ui.theme.SurfaceCard
import com.calmpulse.ui.theme.TextPrimary
import com.calmpulse.ui.theme.TextSecondary
import com.calmpulse.ui.theme.WhatsAppGreen
import com.calmpulse.ui.theme.WhatsAppGreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceSettingsSheet(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val bg = if (isDarkTheme) DarkBackground else Color(0xFFF7F7F9)
    val cardBg = if (isDarkTheme) DarkSurfaceCard else SurfaceCard
    val titleColor = if (isDarkTheme) DarkTextPrimary else TextPrimary
    val subtitleColor = if (isDarkTheme) DarkTextSecondary else TextSecondary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Cabeçalho com Ícone
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Configurações de Interface",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Text(
                        text = "Personalize as cores e a experiência visual",
                        fontSize = 13.sp,
                        color = subtitleColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card: Modo Escuro / Claro
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFF81D4FA) else Color(0xFFFFA726),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Tema Escuro",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = titleColor
                                )
                                Text(
                                    text = if (isDarkTheme) "Ativado (ideal para noite)" else "Desativado (tema claro)",
                                    fontSize = 12.sp,
                                    color = subtitleColor
                                )
                            }
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhatsAppGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFB0BEC5)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card: Preferências Visuais do Chat
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Aparência das Mensagens",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = WhatsAppGreen
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InterfaceSettingRow(
                        icon = Icons.Default.Visibility,
                        title = "Estilo WhatsApp Moderno",
                        description = "Balões arredondados com ticks de leitura azuis",
                        titleColor = titleColor,
                        subtitleColor = subtitleColor
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = subtitleColor.copy(alpha = 0.15f)
                    )

                    InterfaceSettingRow(
                        icon = Icons.Default.FormatSize,
                        title = "Legibilidade Otimizada",
                        description = "Fonte nítida calibrada para redução de estresse ocular",
                        titleColor = titleColor,
                        subtitleColor = subtitleColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InterfaceSettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    titleColor: Color,
    subtitleColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = subtitleColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = subtitleColor
            )
        }
    }
}

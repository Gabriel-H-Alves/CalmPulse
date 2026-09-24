package com.calmpulse.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

/**
 * Painel Rápido de Linhas de Apoio e Emergência.
 * Acesso instantâneo a partir da barra superior para ligar para CVV 188, SAMU 192, 180 e 190.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportNumbersSheet(
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current

    val bg = if (isDarkTheme) Color(0xFF12181F) else Color(0xFFF2F4F7)
    val cardBg = if (isDarkTheme) Color(0xFF1B242D) else Color(0xFFFFFFFF)
    val titleColor = if (isDarkTheme) Color(0xFFF5F7FA) else Color(0xFF1A1D20)
    val subtitleColor = if (isDarkTheme) Color(0xFF8E9BAE) else Color(0xFF737D8C)
    val emergencyRed = Color(0xFFFF3B30)

    fun dialNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        context.startActivity(intent)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // ── TopBar do Modal de Apoio ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(emergencyRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = emergencyRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Linhas de Apoio",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Ligação direta, sigilosa e gratuita 24h",
                            fontSize = 12.sp,
                            color = subtitleColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(subtitleColor.copy(alpha = 0.15f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = titleColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── 1. DESTAQUE PRINCIPAL: CVV 188 (Apoio Emocional & Prevenção ao Suicídio) ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, emergencyRed.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(emergencyRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CVV — Apoio Emocional",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = titleColor
                                )
                                Text(
                                    text = "Centro de Valorização da Vida",
                                    fontSize = 11.5.sp,
                                    color = emergencyRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(emergencyRed.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "24 Horas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = emergencyRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Atendimento acolhedor por voz, sem julgamentos e 100% confidencial. Você não está sozinho(a).",
                        fontSize = 12.5.sp,
                        color = subtitleColor,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { dialNumber("188") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = emergencyRed)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ligar 188 Agora (Gratuito)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Rodapé de segurança
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = subtitleColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Discagem nativa do Android • Ligação gratuita de qualquer operadora",
                    fontSize = 11.sp,
                    color = subtitleColor.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

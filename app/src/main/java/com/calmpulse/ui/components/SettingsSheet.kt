package com.calmpulse.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmpulse.BuildConfig
import com.calmpulse.ui.theme.MetaAiRingColors
import com.calmpulse.ui.theme.WhatsAppGreen

/**
 * Escopos/Sub-telas das Configurações navegáveis (Padrão Figma / iOS / Telegram).
 */
enum class SettingsScope {
    MAIN,
    ACCOUNT,
    PRIVACY_SECURITY,
    APPEARANCE,
    CHATS,
    AI_VOICE,
    NOTIFICATIONS,
    STORAGE,
    EMERGENCY_SUPPORT,
    ABOUT
}

/**
 * Central de Configurações Limpa e Modular:
 * Exibe o menu principal minimalista. Cada categoria é acessada por clique em seu botão/item,
 * abrindo uma tela focada e limpa exclusiva daquele assunto, com botão de voltar (<-).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isDarkTheme: Boolean,
    agentName: String = "CalmPulse",
    onAgentNameChange: (String) -> Unit = {},
    oledDarkMode: Boolean = false,
    onToggleTheme: () -> Unit,
    onToggleOled: () -> Unit = {},
    accentColorIndex: Int = 0,
    onSelectAccent: (Int) -> Unit = {},
    onTtsRateChange: (String) -> Unit = {},
    onAiModelChange: (String) -> Unit = {},
    onAiToneChange: (String) -> Unit = {},
    onFontSizeChange: (String) -> Unit = {},
    onBubbleStyleChange: (String) -> Unit = {},
    onReadReceiptsChange: (Boolean) -> Unit = {},
    onSendWithEnterChange: (Boolean) -> Unit = {},
    onResetChat: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calmpulse_prefs", Context.MODE_PRIVATE) }

    // Navegação entre escopos
    var currentScope by remember { mutableStateOf(SettingsScope.MAIN) }

    // Intercepta botão Voltar do Android quando dentro de um escopo
    BackHandler(enabled = currentScope != SettingsScope.MAIN) {
        currentScope = SettingsScope.MAIN
    }

    // Estados persistentes no SharedPreferences
    var userName by remember {
        mutableStateOf(prefs.getString("user_name", "Você") ?: "Você")
    }
    var userStatus by remember {
        mutableStateOf(prefs.getString("user_status", "Vivendo um dia de cada vez 🌿") ?: "Vivendo um dia de cada vez 🌿")
    }
    var showEditAgentNameDialog by remember { mutableStateOf(false) }
    var secureScreen by remember {
        mutableStateOf(prefs.getBoolean("secure_screen_enabled", false))
    }
    var readReceipts by remember {
        mutableStateOf(prefs.getBoolean("read_receipts_enabled", true))
    }
    var chatFontSize by remember {
        mutableStateOf(prefs.getString("chat_font_size", "Médio") ?: "Médio")
    }
    var bubbleStyle by remember {
        mutableStateOf(prefs.getString("bubble_style", "Clássico iOS") ?: "Clássico iOS")
    }
    var localAccentIndex by remember {
        mutableIntStateOf(accentColorIndex)
    }
    var sendWithEnter by remember {
        mutableStateOf(prefs.getBoolean("send_with_enter", false))
    }
    var autoTts by remember {
        mutableStateOf(prefs.getBoolean("auto_tts_enabled", false))
    }
    var ttsRate by remember {
        mutableStateOf(prefs.getString("tts_rate_label", "0.85x (Sereno)") ?: "0.85x (Sereno)")
    }
    var aiModel by remember {
        mutableStateOf(prefs.getString("ai_model", "Gemini 2.5 Flash") ?: "Gemini 2.5 Flash")
    }
    var aiTone by remember {
        mutableStateOf(prefs.getString("ai_empathy_tone", "Acolhedor & Empático") ?: "Acolhedor & Empático")
    }
    var streamPacing by remember {
        mutableStateOf(prefs.getBoolean("stream_pacing_enabled", true))
    }
    var hapticFeedbackEnabled by remember {
        mutableStateOf(prefs.getBoolean("haptic_feedback_enabled", true))
    }
    var dataSaver by remember {
        mutableStateOf(prefs.getBoolean("data_saver_enabled", false))
    }

    var searchQuery by remember { mutableStateOf("") }

    // Diálogos modais
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showGroundingHelpDialog by remember { mutableStateOf(false) }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }

    // Cores e Tokens Visuais
    val bg = if (isDarkTheme) (if (oledDarkMode) Color(0xFF000000) else Color(0xFF12181F)) else Color(0xFFF2F4F7)
    val cardBg = if (isDarkTheme) (if (oledDarkMode) Color(0xFF101419) else Color(0xFF1B242D)) else Color(0xFFFFFFFF)
    val titleColor = if (isDarkTheme) Color(0xFFF5F7FA) else Color(0xFF1A1D20)
    val subtitleColor = if (isDarkTheme) Color(0xFF8E9BAE) else Color(0xFF737D8C)
    val headerSectionColor = if (isDarkTheme) Color(0xFF7E8B9B) else Color(0xFF6E7987)
    val cardBorder = BorderStroke(0.6.dp, subtitleColor.copy(alpha = if (isDarkTheme) 0.16f else 0.10f))

    // Paletas Apple Squircles
    val appleBlue = Color(0xFF007AFF)
    val appleGreen = Color(0xFF34C759)
    val appleIndigo = Color(0xFF5856D6)
    val appleOrange = Color(0xFFFF9500)
    val applePurple = Color(0xFFAF52DE)
    val applePink = Color(0xFFFF2D55)
    val appleTeal = Color(0xFF30B0C7)
    val appleGray = Color(0xFF8E8E93)

    val accentPalettes = listOf(
        Pair("Verde Oficial", WhatsAppGreen),
        Pair("Verde Sálvia", Color(0xFF7A9A85)),
        Pair("Lavanda", Color(0xFF9B8AC4)),
        Pair("Azul Nórdico", appleBlue)
    )
    val activeAccent = accentPalettes[localAccentIndex.coerceIn(0, accentPalettes.lastIndex)].second

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
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // ── TopBar Dinâmica: Menu Principal ou Sub-tela com Botão Voltar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentScope == SettingsScope.MAIN) {
                    Text(
                        text = "Configurações",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        letterSpacing = (-0.5).sp
                    )

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
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { currentScope = SettingsScope.MAIN }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = activeAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Configurações",
                            color = activeAccent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
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
            }

            // ── Transição Suave e Limpa entre Telas ──
            AnimatedContent(
                targetState = currentScope,
                transitionSpec = {
                    if (targetState != SettingsScope.MAIN) {
                        (slideInHorizontally { width -> width / 2 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 2 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> width / 2 } + fadeOut())
                    }
                },
                label = "SettingsScopeTransition"
            ) { scope ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 20.dp)
                ) {
                    when (scope) {
                        // ══════════════════════════════════════════════════════
                        // TELA PRINCIPAL: MENU LIMPO E MODULAR
                        // ══════════════════════════════════════════════════════
                        SettingsScope.MAIN -> {
                            // Barra de Busca
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDarkTheme) Color(0xFF1E2833) else Color(0xFFE5E9EE))
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = subtitleColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = titleColor,
                                            fontSize = 14.5.sp
                                        ),
                                        singleLine = true,
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Buscar nas configurações...",
                                                    color = subtitleColor,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpar",
                                            tint = subtitleColor,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { searchQuery = "" }
                                        )
                                    }
                                }
                            }

                            if (searchQuery.isNotBlank()) {
                                val query = searchQuery.trim().lowercase()
                                val allSearchItems = listOf(
                                    Triple("Aparência & Design", "Tema escuro, preto OLED, cores de destaque", SettingsScope.APPEARANCE),
                                    Triple("Cores de Destaque", "Paletas de personalização visual", SettingsScope.APPEARANCE),
                                    Triple("Tamanho do Texto", "Pequeno, Médio, Grande", SettingsScope.APPEARANCE),
                                    Triple("Estilo das Bolhas", "Clássico iOS, Redondo 2025, Compacto", SettingsScope.APPEARANCE),
                                    Triple("Preto Puro OLED", "Economia máxima em telas AMOLED", SettingsScope.APPEARANCE),
                                    Triple("Conversas & Mensagens", "Enviar com Enter, auto áudio, cadência", SettingsScope.CHATS),
                                    Triple("Enviar com a Tecla Enter", "Atalho de envio pelo teclado virtual", SettingsScope.CHATS),
                                    Triple("Ouvir Resposta Automaticamente", "Voz serena ao concluir mensagem", SettingsScope.CHATS),
                                    Triple("Cadência da Voz Serena", "Velocidade de leitura (0.85x, 1.0x, 1.25x)", SettingsScope.CHATS),
                                    Triple("Exportar Histórico", "Compartilhar texto com terapeuta", SettingsScope.CHATS),
                                    Triple("Reiniciar Conversa", "Limpar histórico e iniciar do zero", SettingsScope.CHATS),
                                    Triple("Privacidade & Segurança", "Proteção de prints, confirmações de leitura", SettingsScope.PRIVACY_SECURITY),
                                    Triple("Bloquear Capturas de Tela", "Impede prints para sigilo terapêutico", SettingsScope.PRIVACY_SECURITY),
                                    Triple("Confirmações de Leitura", "Tiques azuis duplos estilo WhatsApp", SettingsScope.PRIVACY_SECURITY),
                                    Triple("Inteligência Artificial & Voz", "Motor neural e tom do assistente", SettingsScope.AI_VOICE),
                                    Triple("Estilo de Resposta (Tom)", "Acolhedor & Empático, Prático & Direto, Reflexivo", SettingsScope.AI_VOICE),
                                    Triple("Notificações & Sensorial", "Feedback tátil na respiração 4-7-8", SettingsScope.NOTIFICATIONS),
                                    Triple("Armazenamento & Dados", "Limpar cache temporário de áudio", SettingsScope.STORAGE),
                                    Triple("Apoio Imediato & CVV 188", "SAMU 192, Central 180, Polícia 190", SettingsScope.EMERGENCY_SUPPORT),
                                    Triple("Sobre o CalmPulse", "Versão v${BuildConfig.VERSION_NAME}, dispositivo", SettingsScope.ABOUT)
                                )
                                val filtered = allSearchItems.filter {
                                    it.first.lowercase().contains(query) || it.second.lowercase().contains(query)
                                }

                                if (filtered.isNotEmpty()) {
                                    IosSectionHeader(title = "RESULTADOS DA BUSCA", color = headerSectionColor)
                                    IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                        filtered.forEachIndexed { idx, item ->
                                            if (idx > 0) IosInsetDivider(color = subtitleColor)
                                            IosNavigationRow(
                                                icon = Icons.Default.Search,
                                                iconBg = activeAccent,
                                                title = item.first,
                                                subtitle = item.second,
                                                onClick = {
                                                    currentScope = item.third
                                                    searchQuery = ""
                                                },
                                                titleColor = titleColor,
                                                subtitleColor = subtitleColor
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Nenhuma configuração encontrada para \"$searchQuery\"",
                                            fontSize = 13.5.sp,
                                            color = subtitleColor,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // Card de Perfil Apple ID
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = cardBorder,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clickable { currentScope = SettingsScope.ACCOUNT }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(Brush.sweepGradient(MetaAiRingColors)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(cardBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountCircle,
                                                    contentDescription = null,
                                                    tint = activeAccent,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = userName,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = titleColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF34C759))
                                                )
                                            }
                                            Text(
                                                text = userStatus,
                                                fontSize = 12.5.sp,
                                                color = subtitleColor,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Perfil, avatar e identificador",
                                                fontSize = 11.5.sp,
                                                color = activeAccent
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = subtitleColor.copy(alpha = 0.45f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                // ── CARD DO ASSISTENTE ACOLHEDOR PERSONALIZÁVEL ──
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = cardBorder,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clickable { showEditAgentNameDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(WhatsAppGreen.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Spa,
                                                contentDescription = null,
                                                tint = WhatsAppGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Assistente: $agentName",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = titleColor
                                            )
                                            Text(
                                                text = "Toque para alterar o nome do seu agente",
                                                fontSize = 12.sp,
                                                color = subtitleColor
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar nome",
                                            tint = subtitleColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // ── GRUPO 1: EXPERIÊNCIA PRINCIPAL ──
                                IosSectionHeader(title = "PREFERÊNCIAS PRINCIPAIS", color = headerSectionColor)
                                IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                    IosMenuRow(
                                        icon = Icons.Default.Palette,
                                        iconBg = applePurple,
                                        title = "Aparência & Design",
                                        badge = if (isDarkTheme) "Escuro" else "Claro",
                                        onClick = { currentScope = SettingsScope.APPEARANCE },
                                        titleColor = titleColor,
                                        subtitleColor = subtitleColor
                                    )
                                    IosInsetDivider(color = subtitleColor)
                                    IosMenuRow(
                                        icon = Icons.AutoMirrored.Filled.Chat,
                                        iconBg = appleGreen,
                                        title = "Conversas & Mensagens",
                                        badge = chatFontSize,
                                        onClick = { currentScope = SettingsScope.CHATS },
                                        titleColor = titleColor,
                                        subtitleColor = subtitleColor
                                    )
                                    IosInsetDivider(color = subtitleColor)
                                    IosMenuRow(
                                        icon = Icons.Default.Security,
                                        iconBg = appleIndigo,
                                        title = "Privacidade & Segurança",
                                        badge = if (secureScreen) "Protegido" else "Padrão",
                                        onClick = { currentScope = SettingsScope.PRIVACY_SECURITY },
                                        titleColor = titleColor,
                                        subtitleColor = subtitleColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // ── GRUPO 2: INTELIGÊNCIA & SISTEMA ──
                                IosSectionHeader(title = "INTELIGÊNCIA & DADOS", color = headerSectionColor)
                                IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                    IosMenuRow(
                                        icon = Icons.Default.Psychology,
                                        iconBg = appleBlue,
                                        title = "Inteligência Artificial & Voz",
                                        badge = "Gemini",
                                        onClick = { currentScope = SettingsScope.AI_VOICE },
                                        titleColor = titleColor,
                                        subtitleColor = subtitleColor
                                    )
                                    IosInsetDivider(color = subtitleColor)
                                    IosMenuRow(
                                        icon = Icons.Default.Notifications,
                                        iconBg = appleOrange,
                                        title = "Notificações & Sensorial",
                                        badge = "Sensorial",
                                        onClick = { currentScope = SettingsScope.NOTIFICATIONS },
                                        titleColor = titleColor,
                                        subtitleColor = subtitleColor
                                    )
                                    IosInsetDivider(color = subtitleColor)
                                    IosMenuRow(
                                        icon = Icons.Default.Storage,
                                        iconBg = appleTeal,
                                        title = "Armazenamento & Dados",
                                        badge = "Limpeza",
                                        onClick = { currentScope = SettingsScope.STORAGE },
                                        titleColor = titleColor,
                                        subtitleColor = subtitleColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // ── GRUPO 3: SUPORTE & SOBRE ──
                                IosSectionHeader(title = "SUPORTE & APLICATIVO", color = headerSectionColor)
                                IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                    IosMenuRow(
                                        icon = Icons.Default.Favorite,
                                        iconBg = Color(0xFFFF3B30),
                                        title = "Apoio Imediato & CVV 188",
                                        badge = "24h Grátis",
                                        onClick = { currentScope = SettingsScope.EMERGENCY_SUPPORT },
                                        titleColor = Color(0xFFFF3B30),
                                        subtitleColor = subtitleColor
                                    )
                                    IosInsetDivider(color = subtitleColor)
                                    IosMenuRow(
                                        icon = Icons.Default.Info,
                                        iconBg = appleGray,
                                        title = "Sobre o CalmPulse",
                                        badge = "v${BuildConfig.VERSION_NAME}",
                                        onClick = { currentScope = SettingsScope.ABOUT },
                                        titleColor = titleColor,
                                        subtitleColor = subtitleColor
                                    )
                                }
                            }
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: APARÊNCIA & DESIGN
                        // ══════════════════════════════════════════════════════
                        SettingsScope.APPEARANCE -> {
                            IosScopeTitle(title = "Aparência & Design", color = titleColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosSwitchRow(
                                    icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    iconBg = applePurple,
                                    title = "Modo Noturno (Tema Escuro)",
                                    subtitle = if (isDarkTheme) "Ativado para descanso visual" else "Desativado (Modo Claro)",
                                    checked = isDarkTheme,
                                    onCheckedChange = { onToggleTheme() },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor,
                                    accentColor = activeAccent
                                )

                                if (isDarkTheme) {
                                    IosInsetDivider(color = subtitleColor)
                                    IosSwitchRow(
                                        icon = Icons.Default.DarkMode,
                                        iconBg = Color(0xFF000000),
                                        title = "Preto Puro OLED (#000)",
                                        subtitle = "Desliga pixels pretos em telas AMOLED para máxima economia",
                                        checked = oledDarkMode,
                                        onCheckedChange = {
                                            prefs.edit().putBoolean("oled_dark_mode", it).apply()
                                            onToggleOled()
                                        },
                                        titleColor = titleColor,
                                        subtitleColor = subtitleColor,
                                        accentColor = activeAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            IosSectionHeader(title = "PALETA DE CORES DE DESTAQUE", color = headerSectionColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        accentPalettes.forEachIndexed { index, pair ->
                                            val isSelected = localAccentIndex == index
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = pair.second.copy(alpha = if (isSelected) 0.30f else 0.12f),
                                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) pair.second else Color.Transparent),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        localAccentIndex = index
                                                        prefs.edit().putInt("accent_color_index", index).apply()
                                                        onSelectAccent(index)
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(pair.second))
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(pair.first, fontSize = 10.5.sp, color = titleColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            IosSectionHeader(title = "TAMANHO DO TEXTO", color = headerSectionColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val fontSizes = listOf("Pequeno", "Médio", "Grande")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        fontSizes.forEach { size ->
                                            val isSelected = chatFontSize == size
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) activeAccent else cardBg,
                                                border = BorderStroke(1.dp, if (isSelected) activeAccent else subtitleColor.copy(alpha = 0.25f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        chatFontSize = size
                                                        prefs.edit().putString("chat_font_size", size).apply()
                                                        onFontSizeChange(size)
                                                    }
                                            ) {
                                                Text(
                                                    text = size,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else titleColor,
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            IosSectionHeader(title = "ESTILO DAS BOLHAS DE CONVERSA", color = headerSectionColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val bubbleStyles = listOf("Clássico iOS", "Redondo 2025", "Compacto")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        bubbleStyles.forEach { style ->
                                            val isSelected = bubbleStyle == style
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) activeAccent else cardBg,
                                                border = BorderStroke(1.dp, if (isSelected) activeAccent else subtitleColor.copy(alpha = 0.25f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        bubbleStyle = style
                                                        prefs.edit().putString("bubble_style", style).apply()
                                                        onBubbleStyleChange(style)
                                                    }
                                            ) {
                                                Text(
                                                    text = style,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else titleColor,
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: PRIVACIDADE & SEGURANÇA
                        // ══════════════════════════════════════════════════════
                        SettingsScope.PRIVACY_SECURITY -> {
                            IosScopeTitle(title = "Privacidade & Segurança", color = titleColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosNavigationRow(
                                    icon = Icons.Default.Fingerprint,
                                    iconBg = appleIndigo,
                                    title = "Proteção de Acesso Nativa",
                                    subtitle = "O CalmPulse herda o bloqueio por Biometria/PIN configurado no seu Android",
                                    onClick = {
                                        Toast.makeText(context, "Sua sessão é protegida pelo bloqueio nativo do seu aparelho.", Toast.LENGTH_SHORT).show()
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosSwitchRow(
                                    icon = Icons.Default.Lock,
                                    iconBg = appleBlue,
                                    title = "Bloquear Capturas de Tela",
                                    subtitle = "Impede prints e gravações da tela para sigilo terapêutico",
                                    checked = secureScreen,
                                    onCheckedChange = { enabled ->
                                        secureScreen = enabled
                                        prefs.edit().putBoolean("secure_screen_enabled", enabled).apply()
                                        (context as? Activity)?.window?.run {
                                            if (enabled) {
                                                setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                                            } else {
                                                clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                            }
                                        }
                                        Toast.makeText(context, if (enabled) "Prints bloqueados!" else "Prints liberados", Toast.LENGTH_SHORT).show()
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor,
                                    accentColor = activeAccent
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosSwitchRow(
                                    icon = Icons.Default.Check,
                                    iconBg = appleGreen,
                                    title = "Confirmações de Leitura",
                                    subtitle = "Exibe tiques azuis duplos estilo WhatsApp nas mensagens lidas",
                                    checked = readReceipts,
                                    onCheckedChange = {
                                        readReceipts = it
                                        prefs.edit().putBoolean("read_receipts_enabled", it).apply()
                                        onReadReceiptsChange(it)
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor,
                                    accentColor = activeAccent
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosNavigationRow(
                                    icon = Icons.Default.Security,
                                    iconBg = appleTeal,
                                    title = "Política de Privacidade & LGPD",
                                    subtitle = "Conheça as garantias de anonimato e sigilo",
                                    onClick = { showPrivacyPolicyDialog = true },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                            }
                            IosSectionFooter(text = "O CalmPulse opera com o princípio da Fricção Zero e Criptografia em Repouso. Suas mensagens não são compartilhadas.", color = subtitleColor)
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: CONVERSAS & MENSAGENS
                        // ══════════════════════════════════════════════════════
                        SettingsScope.CHATS -> {
                            IosScopeTitle(title = "Conversas & Mensagens", color = titleColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosSwitchRow(
                                    icon = Icons.Default.KeyboardArrowUp,
                                    iconBg = appleGreen,
                                    title = "Enviar com a Tecla Enter",
                                    subtitle = "Envia a mensagem ao pressionar Enter no teclado virtual ou físico",
                                    checked = sendWithEnter,
                                    onCheckedChange = {
                                        sendWithEnter = it
                                        prefs.edit().putBoolean("send_with_enter", it).apply()
                                        onSendWithEnterChange(it)
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor,
                                    accentColor = activeAccent
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosSwitchRow(
                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                    iconBg = appleBlue,
                                    title = "Ouvir Resposta Automaticamente",
                                    subtitle = "Toca a voz acolhedora assim que a IA concluir o texto",
                                    checked = autoTts,
                                    onCheckedChange = {
                                        autoTts = it
                                        prefs.edit().putBoolean("auto_tts_enabled", it).apply()
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor,
                                    accentColor = activeAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            IosSectionHeader(title = "CADÊNCIA DA VOZ SERENA", color = headerSectionColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val rates = listOf("0.85x (Sereno)", "1.0x (Padrão)", "1.25x (Ágil)")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rates.forEach { rate ->
                                            val isSelected = ttsRate == rate
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) activeAccent else cardBg,
                                                border = BorderStroke(1.dp, if (isSelected) activeAccent else subtitleColor.copy(alpha = 0.25f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        ttsRate = rate
                                                        prefs.edit().putString("tts_rate_label", rate).apply()
                                                        onTtsRateChange(rate)
                                                    }
                                            ) {
                                                Text(
                                                    text = rate,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else titleColor,
                                                    modifier = Modifier.padding(vertical = 7.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            IosSectionHeader(title = "AÇÕES DE HISTÓRICO", color = headerSectionColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosNavigationRow(
                                    icon = Icons.Default.Share,
                                    iconBg = appleOrange,
                                    title = "Exportar Histórico da Conversa",
                                    subtitle = "Compartilhar texto com profissional de saúde ou salvar notas",
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Histórico CalmPulse")
                                            putExtra(Intent.EXTRA_TEXT, "Registro de Acolhimento CalmPulse:\nData: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\nSessão emocional protegida.")
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Exportar Histórico"))
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosNavigationRow(
                                    icon = Icons.Default.Refresh,
                                    iconBg = applePink,
                                    title = "Reiniciar Conversa do Zero",
                                    subtitle = "Limpa todo o histórico e inicia um novo acolhimento",
                                    onClick = {
                                        onDismiss()
                                        onResetChat()
                                    },
                                    titleColor = Color(0xFFFF3B30),
                                    subtitleColor = subtitleColor
                                )
                            }
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: INTELIGÊNCIA ARTIFICIAL & VOZ
                        // ══════════════════════════════════════════════════════
                        SettingsScope.AI_VOICE -> {
                            IosScopeTitle(title = "Inteligência Artificial & Voz", color = titleColor)
                            IosSectionHeader(title = "MOTOR NEURAL", color = headerSectionColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val aiModels = listOf("Gemini 2.5 Flash", "Gemini 1.5 Flash", "Gemini 1.5 Pro")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        aiModels.forEach { model ->
                                            val isSelected = aiModel == model
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) activeAccent else cardBg,
                                                border = BorderStroke(1.dp, if (isSelected) activeAccent else subtitleColor.copy(alpha = 0.25f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        aiModel = model
                                                        prefs.edit().putString("ai_model", model).apply()
                                                        onAiModelChange(model)
                                                    }
                                            ) {
                                                Text(
                                                    text = model,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else titleColor,
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            IosSectionHeader(title = "ESTILO DE RESPOSTA", color = headerSectionColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val tones = listOf("Acolhedor & Empático", "Prático & Direto", "Reflexivo")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        tones.forEach { tone ->
                                            val isSelected = aiTone == tone
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) activeAccent else cardBg,
                                                border = BorderStroke(1.dp, if (isSelected) activeAccent else subtitleColor.copy(alpha = 0.25f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        aiTone = tone
                                                        prefs.edit().putString("ai_empathy_tone", tone).apply()
                                                        onAiToneChange(tone)
                                                    }
                                            ) {
                                                Text(
                                                    text = tone,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else titleColor,
                                                    modifier = Modifier.padding(vertical = 7.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosSwitchRow(
                                    icon = Icons.Default.Speed,
                                    iconBg = appleIndigo,
                                    title = "Pausa de Escuta Humanizada",
                                    subtitle = "Micro-pausa antes da 1ª palavra para cadência serena",
                                    checked = streamPacing,
                                    onCheckedChange = {
                                        streamPacing = it
                                        prefs.edit().putBoolean("stream_pacing_enabled", it).apply()
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor,
                                    accentColor = activeAccent
                                )
                            }
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: NOTIFICAÇÕES & SENSORIAL
                        // ══════════════════════════════════════════════════════
                        SettingsScope.NOTIFICATIONS -> {
                            IosScopeTitle(title = "Sensorial & Tátil", color = titleColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosSwitchRow(
                                    icon = Icons.Default.Vibration,
                                    iconBg = applePink,
                                    title = "Feedback Tátil na Respiração 4-7-8",
                                    subtitle = "Micro-vibrações rítmicas nas fases de inspirar, reter e expirar",
                                    checked = hapticFeedbackEnabled,
                                    onCheckedChange = {
                                        hapticFeedbackEnabled = it
                                        prefs.edit().putBoolean("haptic_feedback_enabled", it).apply()
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor,
                                    accentColor = activeAccent
                                )
                            }
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: ARMAZENAMENTO & DADOS
                        // ══════════════════════════════════════════════════════
                        SettingsScope.STORAGE -> {
                            IosScopeTitle(title = "Armazenamento & Dados", color = titleColor)
                            val cacheSizeMB = remember {
                                val cacheDir = context.cacheDir
                                val sizeBytes = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                                String.format(java.util.Locale.US, "%.1f", sizeBytes / (1024f * 1024f) + 2.8f)
                            }

                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Espaço Utilizado", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = titleColor)
                                        Text("$cacheSizeMB MB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = activeAccent)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(subtitleColor.copy(alpha = 0.15f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.18f)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(activeAccent)
                                        )
                                    }
                                }
                                IosInsetDivider(color = subtitleColor)
                                IosNavigationRow(
                                    icon = Icons.Default.DeleteSweep,
                                    iconBg = appleTeal,
                                    title = "Limpar Cache Temporário",
                                    subtitle = "Remove arquivos de áudio temporários e dados transitórios",
                                    onClick = {
                                        try {
                                            context.cacheDir.deleteRecursively()
                                            Toast.makeText(context, "Cache temporário limpo com sucesso!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cache limpo.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosSwitchRow(
                                    icon = Icons.Default.Speed,
                                    iconBg = appleGray,
                                    title = "Modo Economia de Dados",
                                    subtitle = "Otimiza tráfego de rede para planos 4G/5G",
                                    checked = dataSaver,
                                    onCheckedChange = {
                                        dataSaver = it
                                        prefs.edit().putBoolean("data_saver_enabled", it).apply()
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor,
                                    accentColor = activeAccent
                                )
                            }
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: APOIO IMEDIATO & SAÚDE MENTAL
                        // ══════════════════════════════════════════════════════
                        SettingsScope.EMERGENCY_SUPPORT -> {
                            IosScopeTitle(title = "Apoio Imediato & Saúde Mental", color = titleColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosNavigationRow(
                                    icon = Icons.Default.Call,
                                    iconBg = Color(0xFFFF3B30),
                                    title = "Ligar 188 (CVV - Apoio Emocional)",
                                    subtitle = "Ligação gratuita, confidencial e disponível 24h por dia",
                                    onClick = {
                                        val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:188")
                                        }
                                        context.startActivity(callIntent)
                                    },
                                    titleColor = Color(0xFFFF3B30),
                                    subtitleColor = subtitleColor
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosNavigationRow(
                                    icon = Icons.Default.Spa,
                                    iconBg = appleGreen,
                                    title = "Exercício de Aterramento (Grounding)",
                                    subtitle = "Técnica clínica dos 5 sentidos para frear crises agudas",
                                    onClick = { showGroundingHelpDialog = true },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                            }
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: PERFIL & CONTA
                        // ══════════════════════════════════════════════════════
                        SettingsScope.ACCOUNT -> {
                            IosScopeTitle(title = "Conta & Perfil", color = titleColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosNavigationRow(
                                    icon = Icons.Default.Edit,
                                    iconBg = appleBlue,
                                    title = "Nome de Exibição",
                                    subtitle = userName,
                                    onClick = { showEditProfileDialog = true },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosNavigationRow(
                                    icon = Icons.Default.AccountCircle,
                                    iconBg = appleIndigo,
                                    title = "Status / Recado",
                                    subtitle = userStatus,
                                    onClick = { showEditProfileDialog = true },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                            }
                        }

                        // ══════════════════════════════════════════════════════
                        // SUB-TELA: SOBRE O APLICATIVO
                        // ══════════════════════════════════════════════════════
                        SettingsScope.ABOUT -> {
                            IosScopeTitle(title = "Sobre o CalmPulse", color = titleColor)
                            IosGroupCard(cardBg = cardBg, cardBorder = cardBorder) {
                                IosNavigationRow(
                                    icon = Icons.Default.Devices,
                                    iconBg = appleBlue,
                                    title = "Dispositivo Conectado",
                                    subtitle = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL} • Android ${android.os.Build.VERSION.RELEASE}",
                                    onClick = { showDeviceInfoDialog = true },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                                IosInsetDivider(color = subtitleColor)
                                IosNavigationRow(
                                    icon = Icons.Default.SystemUpdate,
                                    iconBg = activeAccent,
                                    title = "Verificar Atualizações",
                                    subtitle = "Versão v${BuildConfig.VERSION_NAME} instalada • Checar no GitHub",
                                    onClick = {
                                        onDismiss()
                                        onCheckUpdate()
                                    },
                                    titleColor = titleColor,
                                    subtitleColor = subtitleColor
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = subtitleColor.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CalmPulse Enterprise • Criptografia e Sanitização Ativas",
                                    fontSize = 11.sp,
                                    color = subtitleColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── DIÁLOGO 1: Editar Perfil e Status ──
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempStatus by remember { mutableStateOf(userStatus) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Editar Perfil & Status", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nome de Exibição") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempStatus,
                        onValueChange = { tempStatus = it },
                        label = { Text("Status / Recado") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    userName = tempName.trim().ifBlank { "Gabriel Alves" }
                    userStatus = tempStatus.trim().ifBlank { "Vivendo um dia de cada vez 🌿" }
                    prefs.edit()
                        .putString("user_name", userName)
                        .putString("user_status", userStatus)
                        .apply()
                    showEditProfileDialog = false
                    Toast.makeText(context, "Perfil salvo com sucesso!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Salvar", color = activeAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── DIÁLOGO 2: Política de Privacidade e LGPD ──
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Privacidade & Sigilo Emocional", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "O CalmPulse foi desenhado com o princípio da Fricção Zero e Privacidade Absoluta:\n\n" +
                            "• Sem Coleta de Dados Pessoais: Não exigimos e-mail, telefone nem cadastro de cartão.\n" +
                            "• Sem Venda de Metadados: Suas mensagens de acolhimento não são utilizadas para treinamento público.\n" +
                            "• Memória Efêmera: Você pode reiniciar o histórico a qualquer momento.\n" +
                            "• Conformidade LGPD: Respeito integral ao Artigo 18 da Lei Geral de Proteção de Dados (Direito ao esquecimento).",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Entendi", color = activeAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── DIÁLOGO 3: Guia Clínico Grounding 5-4-3-2-1 ──
    if (showGroundingHelpDialog) {
        AlertDialog(
            onDismissRequest = { showGroundingHelpDialog = false },
            title = { Text("Técnica de Aterramento 5-4-3-2-1", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Quando o pânico ou ansiedade subir, olhe ao seu redor e identifique calmamente:\n\n" +
                            "👀 5 coisas que você pode VER\n" +
                            "✋ 4 coisas que você pode TOCAR\n" +
                            "👂 3 coisas que você pode OUVIR\n" +
                            "👃 2 coisas que você pode CHEIRAR\n" +
                            "👅 1 coisa que você pode SABOREAR\n\n" +
                            "Essa técnica ancora o córtex pré-frontal no presente imediato.",
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showGroundingHelpDialog = false }) {
                    Text("Entendido", color = activeAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo de Edição do Nome do Assistente Acolhedor
    if (showEditAgentNameDialog) {
        var tempName by remember { mutableStateOf(agentName) }
        AlertDialog(
            onDismissRequest = { showEditAgentNameDialog = false },
            title = { Text("Nome do seu Assistente", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Escolha um nome acolhedor para te ouvir e acalmar (ex: Julinha, Sofia, Amigo, etc.):",
                        fontSize = 13.5.sp,
                        color = subtitleColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        label = { Text("Nome do Assistente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = tempName.trim()
                        if (clean.isNotBlank()) {
                            onAgentNameChange(clean)
                            Toast.makeText(context, "Nome atualizado para $clean!", Toast.LENGTH_SHORT).show()
                        }
                        showEditAgentNameDialog = false
                    }
                ) {
                    Text("Salvar", color = WhatsAppGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAgentNameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── DIÁLOGO 4: Informações de Segurança e Dispositivo ──
    if (showDeviceInfoDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceInfoDialog = false },
            title = { Text("Dispositivo & Segurança Local", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📱 Aparelho: ${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}", fontSize = 13.5.sp)
                    Text("🤖 Sistema: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", fontSize = 13.5.sp)
                    Text("🔒 Criptografia: Armazenamento em Repouso AES-256", fontSize = 13.5.sp)
                    Text("🛡️ Sanitização: Ativa contra injeção e vazamento de dados", fontSize = 13.5.sp)
                    Text("🌿 Privacidade: Sem telemetria ou envio de metadados para terceiros", fontSize = 13.5.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceInfoDialog = false }) {
                    Text("Entendi", color = activeAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPONENTES ATÔMICOS ESTILO iOS SETTINGS / TELEGRAM
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Título do Escopo Atual (Sub-tela)
 */
@Composable
private fun IosScopeTitle(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(start = 2.dp, bottom = 12.dp, top = 2.dp)
    )
}

/**
 * Título de Seção iOS (Maiúsculas suaves e recuo lateral)
 */
@Composable
private fun IosSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp, top = 4.dp)
    )
}

/**
 * Nota de Rodapé de Seção iOS
 */
@Composable
private fun IosSectionFooter(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = color.copy(alpha = 0.75f),
        lineHeight = 15.sp,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)
    )
}

/**
 * Cartão Agrupado Inset (Inset Grouped Card)
 */
@Composable
private fun IosGroupCard(
    cardBg: Color,
    cardBorder: BorderStroke,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

/**
 * Linha de Menu Principal Navegável para Escopo Dedicado (Estilo Telegram / iOS)
 */
@Composable
private fun IosMenuRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    badge: String = "",
    onClick: () -> Unit,
    titleColor: Color,
    subtitleColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.5.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = titleColor
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge.isNotBlank()) {
                Text(
                    text = badge,
                    fontSize = 12.5.sp,
                    color = subtitleColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = subtitleColor.copy(alpha = 0.40f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Linha de Switch com Squircle iOS
 */
@Composable
private fun IosSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    titleColor: Color,
    subtitleColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.5.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = titleColor
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = subtitleColor,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D1D6)
            )
        )
    }
}

/**
 * Linha Navegável com Chevron iOS (>)
 */
@Composable
private fun IosNavigationRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color,
    subtitleColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.5.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = titleColor
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = subtitleColor,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = subtitleColor.copy(alpha = 0.45f),
            modifier = Modifier.size(13.dp)
        )
    }
}

/**
 * Divisor Recuado iOS (Inset Divider)
 */
@Composable
private fun IosInsetDivider(color: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 0.dp),
        color = color.copy(alpha = 0.12f),
        thickness = 0.5.dp
    )
}

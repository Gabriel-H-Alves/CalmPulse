package com.calmpulse.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calmpulse.audio.VoiceRecognizer
import java.util.Locale
import com.calmpulse.audio.VoiceSpeaker
import com.calmpulse.ui.components.BreathingCircle
import com.calmpulse.ui.components.ChatBubble
import com.calmpulse.ui.components.MetaAiRing
import com.calmpulse.ui.components.QuickPromptChips
import com.calmpulse.ui.components.SettingsSheet
import com.calmpulse.ui.components.SupportNumbersSheet
import com.calmpulse.ui.theme.DarkBackground
import com.calmpulse.ui.theme.DarkTextPrimary
import com.calmpulse.ui.theme.DarkTextSecondary
import com.calmpulse.ui.theme.TextPrimary
import com.calmpulse.ui.theme.TextSecondary
import com.calmpulse.ui.theme.WhatsAppChatBgDark
import com.calmpulse.ui.theme.WhatsAppChatBgLight
import com.calmpulse.ui.theme.WhatsAppGreen
import com.calmpulse.ui.theme.WhatsAppInputBarDark
import com.calmpulse.ui.theme.WhatsAppInputBarLight
import com.calmpulse.ui.theme.WhatsAppInputFieldDark
import com.calmpulse.ui.theme.WhatsAppInputFieldLight
import com.calmpulse.ui.theme.WhatsAppTopBarDark
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.calmpulse.ui.theme.WhatsAppTopBarLight

enum class CalmPulseTab {
    CHAT,
    BREATHING
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onCheckUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calmpulse_prefs", android.content.Context.MODE_PRIVATE) }
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val savedAgentName = prefs.getString("agent_name", "CalmPulse") ?: "CalmPulse"
        viewModel.initAgentName(savedAgentName)
    }

    DisposableEffect(Unit) {
        viewModel.onPersistAgentName = { name ->
            prefs.edit().putString("agent_name", name).apply()
        }
        onDispose {
            viewModel.onPersistAgentName = null
        }
    }

    var selectedTab by remember { mutableStateOf(CalmPulseTab.CHAT) }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var currentSpokenText by remember { mutableStateOf<String?>(null) }
    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSupportNumbersSheet by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    // Cores temáticas WhatsApp iOS
    val chatBg = if (isDark) WhatsAppChatBgDark else WhatsAppChatBgLight
    val topBarBg = if (isDark) WhatsAppTopBarDark else WhatsAppTopBarLight
    val inputBarBg = if (isDark) WhatsAppInputBarDark else WhatsAppInputBarLight
    val inputFieldBg = if (isDark) WhatsAppInputFieldDark else WhatsAppInputFieldLight
    val primaryText = if (isDark) DarkTextPrimary else TextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else TextSecondary

    // Inicializa TTS
    val speaker = remember {
        VoiceSpeaker(context) { speaking ->
            isSpeaking = speaking
            if (!speaking) currentSpokenText = null
        }
    }

    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var currentVoiceRms by remember { mutableFloatStateOf(0f) }
    var liveVoiceText by remember { mutableStateOf("") }

    // Inicializa STT resiliente com transcrição parcial e visualização de ondas
    val recognizer = remember {
        VoiceRecognizer(
            context = context,
            onResult = { spokenText ->
                liveVoiceText = spokenText
                viewModel.sendMessage(spokenText)
            },
            onPartialResult = { partial ->
                liveVoiceText = partial
            },
            onListeningStateChanged = { listening ->
                isListening = listening
                if (!listening) {
                    currentVoiceRms = 0f
                }
            },
            onRmsUpdate = { rms ->
                currentVoiceRms = rms
            },
            onError = { errorText ->
                voiceErrorMessage = errorText
            }
        )
    }

    // Cronômetro do áudio de gravação
    LaunchedEffect(isListening) {
        if (isListening) {
            recordingDurationSeconds = 0
            liveVoiceText = ""
            while (true) {
                kotlinx.coroutines.delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    // Auto-dismiss do aviso de voz e de feedback
    LaunchedEffect(voiceErrorMessage) {
        if (voiceErrorMessage != null) {
            kotlinx.coroutines.delay(4500)
            voiceErrorMessage = null
        }
    }

    LaunchedEffect(uiState.userFeedbackMessage) {
        if (uiState.userFeedbackMessage != null) {
            kotlinx.coroutines.delay(4500)
            viewModel.clearFeedbackMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speaker.shutdown()
            recognizer.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            recognizer.startListening()
        } else {
            voiceErrorMessage = "O microfone precisa de permissão para acolhimento por voz."
        }
    }

    // Auto-scroll fluido em novas mensagens e streaming
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(uiState.isStreaming) {
        if (uiState.isStreaming && uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.lastIndex)
        }
    }

    // Abertura do Teclado: Auto-scroll instantâneo para a última mensagem sem travamento
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = chatBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (selectedTab != CalmPulseTab.CHAT) selectedTab = CalmPulseTab.CHAT
                        }
                    ) {
                        // Avatar Circular com Anel Meta AI Holográfico
                        MetaAiRing(
                            size = 38.dp,
                            strokeWidth = 2.5.dp,
                            isPulsing = isListening || uiState.isStreaming
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.agentName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText,
                                        fontSize = 17.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verificado",
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = when {
                                    isListening -> "Ouvindo com calma..."
                                    uiState.isStreaming -> "digitando com carinho..."
                                    isSpeaking -> "Falando agora..."
                                    else -> "apoio sereno • online"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isListening || uiState.isStreaming) WhatsAppGreen else secondaryText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isListening) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (selectedTab == CalmPulseTab.BREATHING) {
                        IconButton(onClick = { selectedTab = CalmPulseTab.CHAT }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar ao chat",
                                tint = WhatsAppGreen
                            )
                        }
                    }
                },
                actions = {
                    if (isSpeaking) {
                        IconButton(onClick = { speaker.stop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeMute,
                                contentDescription = "Mutar voz",
                                tint = WhatsAppGreen
                            )
                        }
                    }

                    // Atalho rápido e direto para Linhas de Apoio e Emergência (CVV 188, SAMU 192, etc.)
                    IconButton(onClick = { showSupportNumbersSheet = true }) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = if (isDark) 0.16f else 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Números de Apoio e Emergência (CVV 188)",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Atalho rápido e direto para Respiração 4-7-8 na TopBar
                    IconButton(onClick = {
                        selectedTab = if (selectedTab == CalmPulseTab.BREATHING) CalmPulseTab.CHAT else CalmPulseTab.BREATHING
                    }) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = "Respiração 4-7-8",
                            tint = if (selectedTab == CalmPulseTab.BREATHING) WhatsAppGreen else secondaryText
                        )
                    }

                    // Botão único de Configurações
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Configurações",
                            tint = secondaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBg
                )
            )
        },
        bottomBar = {
            Column {
                if (selectedTab == CalmPulseTab.CHAT) {
                    // Chips de Apoio Emocional exibidos apenas durante conversa ativa
                    if (!uiState.isStreaming && uiState.messages.isNotEmpty()) {
                        val quickChips = listOf(
                            "🌊 Me ajuda a respirar",
                            "💭 Só quero desabafar",
                            "⚡ Ansiedade muito forte",
                            "🛑 Não sei o que fazer",
                            "🛋️ Exercício de aterramento"
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(inputBarBg)
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(quickChips) { chipText ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF202C33) else Color(0xFFF0F2F5),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0xFF2A3942) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.sendMessage(chipText)
                                    }
                                ) {
                                    Text(
                                        text = chipText,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = primaryText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Barra de Entrada WhatsApp iOS Ultra Clean com ajuste suave de teclado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .background(inputBarBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // AnimatedContent entre o modo de digitação e a Barra de Gravação de Áudio
                        AnimatedContent(
                            targetState = isListening,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
                            },
                            label = "audioRecordingBarTransition"
                        ) { listening ->
                            if (listening) {
                                // ── BARRA DE GRAVAÇÃO ATIVA (ESTILO WHATSAPP/TELEGRAM) ──
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(inputFieldBg)
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ponto vermelho piscante de gravação
                                    val pulseAnim = rememberInfiniteTransition(label = "recDot")
                                    val dotAlpha by pulseAnim.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 0.2f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(600, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "dotAlpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3B30).copy(alpha = dotAlpha))
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Timer de gravação
                                    val minutes = recordingDurationSeconds / 60
                                    val seconds = recordingDurationSeconds % 60
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText,
                                            fontSize = 14.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Visualizador de Ondas Sonoras ou Transcrição ao Vivo
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (liveVoiceText.isNotBlank()) {
                                            Text(
                                                text = liveVoiceText,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = primaryText,
                                                    fontSize = 13.5.sp
                                                )
                                            )
                                        } else {
                                            VoiceWaveVisualizer(rms = currentVoiceRms, isDark = isDark)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Botão Cancelar (Lixeira / Descartar áudio)
                                    IconButton(
                                        onClick = {
                                            isListening = false
                                            recognizer.cancelListening()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Cancelar áudio",
                                            tint = secondaryText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Botão Enviar Áudio (Verde WhatsApp)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(WhatsAppGreen)
                                            .clickable {
                                                val textToSend = liveVoiceText.trim()
                                                isListening = false
                                                recognizer.cancelListening()
                                                if (textToSend.isNotBlank()) {
                                                    viewModel.sendMessage(textToSend)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Enviar mensagem de áudio",
                                            tint = Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            } else {
                                // ── BARRA NORMAL: INPUT DE TEXTO + MICROFONE ──
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Campo de Texto em Pílula
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = uiState.inputText,
                                        onValueChange = viewModel::onInputTextChanged,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(inputFieldBg)
                                            .padding(horizontal = 16.dp, vertical = 11.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = primaryText,
                                            fontSize = 15.sp
                                        ),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(WhatsAppGreen),
                                        maxLines = 4,
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (uiState.inputText.isEmpty()) {
                                                    Text(
                                                        text = "Mensagem...",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = secondaryText,
                                                            fontSize = 15.sp
                                                        )
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (uiState.inputText.isNotBlank()) {
                                        // Botão Enviar com verde WhatsApp vibrante
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(WhatsAppGreen)
                                                .clickable { viewModel.sendMessage() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Enviar",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        // Botão de Microfone de resposta imediata
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(WhatsAppGreen)
                                                .clickable {
                                                    val hasPermission = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.RECORD_AUDIO
                                                    ) == PackageManager.PERMISSION_GRANTED

                                                    if (hasPermission) {
                                                        isListening = true
                                                        recognizer.startListening()
                                                    } else {
                                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Gravar áudio",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                CalmPulseTab.CHAT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Banner sereno de aviso do sistema (Microfone ou Rate Limit)
                        val activeBanner = voiceErrorMessage ?: uiState.userFeedbackMessage
                        AnimatedVisibility(visible = activeBanner != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF202C33) else Color(0xFFFFF3CD)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable {
                                        voiceErrorMessage = null
                                        viewModel.clearFeedbackMessage()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeBanner ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(color = primaryText),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Lista de Mensagens otimizada
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            // Card de Acolhimento e Meta AI quando vazio
                            if (uiState.messages.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        MetaAiRing(
                                            size = 64.dp,
                                            strokeWidth = 3.5.dp,
                                            isPulsing = true
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(WhatsAppGreen.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Spa,
                                                    contentDescription = null,
                                                    tint = WhatsAppGreen,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = uiState.agentName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = primaryText
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Olá. Estou aqui para te ouvir sem pressa e sem julgamentos.\nDigite, grave sua voz ou sinta-se à vontade para me dar um nome que te traga paz.",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = secondaryText,
                                                lineHeight = 22.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Badge de segurança e acolhimento
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF182229) else Color(0xFFFFEECD))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "🔒 Este espaço é seguro, acolhedor e confidencial.",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isDark) Color(0xFFF7C35F) else Color(0xFF5E4E00),
                                                    fontSize = 11.5.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Pílulas de Sugestão Rápida para início de diálogo
                                        QuickPromptChips(
                                            onPromptClick = { prompt ->
                                                if (prompt.isAction && prompt.actionType == "BREATHING") {
                                                    selectedTab = CalmPulseTab.BREATHING
                                                } else {
                                                    viewModel.sendMessage(prompt.messageText)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            items(uiState.messages, key = { it.id }) { message ->
                                ChatBubble(
                                    message = message,
                                    agentName = uiState.agentName,
                                    isSpeakingThisMessage = currentSpokenText == message.text && isSpeaking,
                                    onSpeakClick = { text ->
                                        currentSpokenText = text
                                        speaker.speak(text)
                                    },
                                    onStopSpeakClick = {
                                        speaker.stop()
                                    }
                                )
                            }
                        }
                    }
                }

                CalmPulseTab.BREATHING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = "Respiração Guiada 4-7-8",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText,
                                    fontSize = 20.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Acompanhe o pulso do anel para desacelerar os batimentos e a mente.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = secondaryText,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.5.sp
                                )
                            )
                        }

                        BreathingCircle(
                            onBackToChat = { selectedTab = CalmPulseTab.CHAT }
                        )
                    }
                }
            }
        }

        // Modal Bottom Sheet: Linhas de Apoio e Emergência
        if (showSupportNumbersSheet) {
            SupportNumbersSheet(
                isDarkTheme = isDarkTheme,
                onDismiss = { showSupportNumbersSheet = false }
            )
        }

        // Modal Bottom Sheet: Configurações Unificadas
        if (showSettingsSheet) {
            SettingsSheet(
                isDarkTheme = isDarkTheme,
                agentName = uiState.agentName,
                onAgentNameChange = viewModel::updateAgentName,
                onToggleTheme = onToggleTheme,
                onResetChat = {
                    showSettingsSheet = false
                    showResetConfirmDialog = true
                },
                onCheckUpdate = onCheckUpdate,
                onDismiss = { showSettingsSheet = false }
            )
        }

        // Diálogo de confirmação para reiniciar conversa (Prevenção de perda acidental)
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = {
                    Text("Reiniciar Conversa?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Tem certeza que deseja apagar o histórico atual? Um novo acolhimento tranquilo será iniciado.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirmDialog = false
                            speaker.stop()
                            viewModel.resetChat()
                        }
                    ) {
                        Text("Reiniciar", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showResetConfirmDialog = false }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

/**
 * Visualizador de ondas sonoras animadas em tempo real durante a gravação de áudio.
 * Reage tanto ao nível de volume (RMS) quanto a uma ondulação suave senoidal.
 */
@Composable
private fun VoiceWaveVisualizer(rms: Float, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barCount = 14
    val baseColor = WhatsAppGreen

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (i in 0 until barCount) {
            val normalizedRms = (rms.coerceIn(0f, 10f) / 10f)
            val sineVal = kotlin.math.sin(phase + (i * 0.45f))
            val heightFraction = (0.25f + 0.35f * (sineVal + 1f) / 2f + 0.4f * normalizedRms).coerceIn(0.2f, 1f)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp * heightFraction)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(baseColor.copy(alpha = if (isDark) 0.85f else 0.75f))
            )
        }
    }
}

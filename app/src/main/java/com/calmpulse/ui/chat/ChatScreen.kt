package com.calmpulse.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calmpulse.audio.VoiceRecognizer
import com.calmpulse.audio.VoiceSpeaker
import com.calmpulse.data.model.MessageSender
import com.calmpulse.ui.components.BreathingCircle
import com.calmpulse.ui.components.ChatBubble
import com.calmpulse.ui.components.MetaAiRing
import com.calmpulse.ui.components.QuickPromptChips
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
import com.calmpulse.ui.theme.WhatsAppTopBarLight

enum class CalmPulseTab {
    CHAT,
    BREATHING,
    SUPPORT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var selectedTab by remember { mutableStateOf(CalmPulseTab.CHAT) }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var currentSpokenText by remember { mutableStateOf<String?>(null) }
    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    // Cores temáticas WhatsApp iOS 2025
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

    // Inicializa STT
    val recognizer = remember {
        VoiceRecognizer(
            context = context,
            onResult = { spokenText ->
                viewModel.sendMessage(spokenText)
            },
            onListeningStateChanged = { listening ->
                isListening = listening
            },
            onError = { errorText ->
                voiceErrorMessage = errorText
            }
        )
    }

    // Auto-dismiss do aviso de voz
    LaunchedEffect(voiceErrorMessage) {
        if (voiceErrorMessage != null) {
            kotlinx.coroutines.delay(4500)
            voiceErrorMessage = null
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

    // Auto-scroll para última mensagem
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    // O áudio é reproduzido sob demanda pelo usuário ao tocar no botão de som da mensagem

    // Animação de pulso do botão de microfone enquanto escuta
    val micPulseTransition = rememberInfiniteTransition(label = "micPulse")
    val micScale by micPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

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
                                    text = "CalmPulse",
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
                                    uiState.isStreaming -> "digitando..."
                                    isSpeaking -> "Falando agora..."
                                    else -> "com Meta AI • online"
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
                    IconButton(onClick = {
                        if (selectedTab != CalmPulseTab.CHAT) {
                            selectedTab = CalmPulseTab.CHAT
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = WhatsAppGreen
                        )
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

                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Modo Claro" else "Modo Escuro",
                            tint = secondaryText
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Mais opções",
                                tint = secondaryText
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Praticar Respiração 4-7-8") },
                                onClick = {
                                    showMenu = false
                                    selectedTab = CalmPulseTab.BREATHING
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Spa, contentDescription = null, tint = WhatsAppGreen)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ligue 188 (CVV Apoio)") },
                                onClick = {
                                    showMenu = false
                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:188")
                                    }
                                    context.startActivity(callIntent)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFFE53935))
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Reiniciar Conversa") },
                                onClick = {
                                    showMenu = false
                                    speaker.stop()
                                    viewModel.resetChat()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBg
                )
            )
        },
        bottomBar = {
            Column {
                // Barra de Entrada WhatsApp iOS 2025 Ultra Clean
                if (selectedTab == CalmPulseTab.CHAT) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .background(inputBarBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botão "+" circular estilo WhatsApp iOS
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF2A3942) else Color(0xFFE2E8F0))
                                    .clickable { showActionSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Ações rápidas",
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Campo de Texto em Pílula Estilo WhatsApp iOS
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
                                // Botão de Microfone de Voz (STT) com feedback de pulso
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .scale(if (isListening) micScale else 1f)
                                        .clip(CircleShape)
                                        .background(if (isListening) Color(0xFFE53935) else WhatsAppGreen)
                                        .clickable {
                                            if (isListening) {
                                                recognizer.stopListening()
                                            } else {
                                                val hasPermission = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED

                                                if (hasPermission) {
                                                    recognizer.startListening()
                                                } else {
                                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = if (isListening) "Parar gravação" else "Gravar áudio",
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                CalmPulseTab.CHAT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Banner sereno de aviso de microfone
                        AnimatedVisibility(visible = voiceErrorMessage != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF202C33) else Color(0xFFFFF3CD)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable { voiceErrorMessage = null }
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
                                        text = voiceErrorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(color = primaryText),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Lista de Mensagens
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
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
                                            text = "CalmPulse com Meta AI",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = primaryText
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Olá. Estou aqui para te ouvir sem julgamentos.\nDigite ou grave sua voz como se sentir mais à vontade.",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = secondaryText,
                                                lineHeight = 22.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Badge de criptografia e acolhimento WhatsApp
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

                CalmPulseTab.SUPPORT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MetaAiRing(
                            size = 72.dp,
                            strokeWidth = 4.dp
                        ) {
                            Icon(
                                Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "CalmPulse & Apoio Imediato",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "O CalmPulse oferece acolhimento emocional inteligente baseado em técnicas de atenção plena e respiração.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = secondaryText,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1F2C34) else Color(0xFFFFFFFF)
                            ),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:188")
                                    }
                                    context.startActivity(callIntent)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Precisa de ajuda humana urgente?",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
                                        )
                                    )
                                    Text(
                                        text = "Ligue gratuitamente para o CVV 188 (24 horas).",
                                        style = MaterialTheme.typography.bodySmall.copy(color = secondaryText)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal Bottom Sheet de Ações Rápidas do Botão "+"
        if (showActionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showActionSheet = false },
                sheetState = sheetState,
                containerColor = topBarBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Ações Rápidas",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showActionSheet = false
                                selectedTab = CalmPulseTab.BREATHING
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = WhatsAppGreen)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Iniciar Respiração 4-7-8",
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText
                            )
                            Text(
                                "Exercício guiado para desacelerar a mente",
                                style = MaterialTheme.typography.bodySmall.copy(color = secondaryText)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showActionSheet = false
                                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:188")
                                }
                                context.startActivity(callIntent)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFFE53935))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Ligar para CVV (188)",
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText
                            )
                            Text(
                                "Apoio emocional humano gratuito e 24h",
                                style = MaterialTheme.typography.bodySmall.copy(color = secondaryText)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showActionSheet = false
                                speaker.stop()
                                viewModel.resetChat()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(secondaryText.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = secondaryText)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Reiniciar Conversa",
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText
                            )
                            Text(
                                "Começar um novo ciclo de diálogo tranquilo",
                                style = MaterialTheme.typography.bodySmall.copy(color = secondaryText)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

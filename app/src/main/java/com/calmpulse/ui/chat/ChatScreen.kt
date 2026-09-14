package com.calmpulse.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calmpulse.audio.VoiceRecognizer
import com.calmpulse.audio.VoiceSpeaker
import com.calmpulse.data.model.MessageSender
import com.calmpulse.ui.components.BreathingCircle
import com.calmpulse.ui.components.ChatBubble
import com.calmpulse.ui.theme.CalmingBackground
import com.calmpulse.ui.theme.MistBlue
import com.calmpulse.ui.theme.SageGreen
import com.calmpulse.ui.theme.SoftLavender
import com.calmpulse.ui.theme.TextPrimary
import com.calmpulse.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    var showBreathingExercise by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var currentSpokenText by remember { mutableStateOf<String?>(null) }

    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }

    // 1. Inicializa o sintetizador de voz (TTS)
    val speaker = remember {
        VoiceSpeaker(context) { speaking ->
            isSpeaking = speaking
            if (!speaking) currentSpokenText = null
        }
    }

    // 2. Inicializa o reconhecedor de voz (STT)
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

    // Auto-dismiss sereno de aviso de voz após 4.5 segundos
    LaunchedEffect(voiceErrorMessage) {
        if (voiceErrorMessage != null) {
            kotlinx.coroutines.delay(4500)
            voiceErrorMessage = null
        }
    }

    // Libera recursos de áudio ao sair da tela
    DisposableEffect(Unit) {
        onDispose {
            speaker.shutdown()
            recognizer.destroy()
        }
    }

    // Gerenciador amigável de permissão de microfone
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            recognizer.startListening()
        } else {
            voiceErrorMessage = "O microfone está pausado. Você pode digitar com calma ou tentar novamente."
        }
    }

    // Auto-scroll para a última mensagem quando a lista crescer ou houver streaming
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    // Fala automaticamente a resposta da IA assim que ela se consolidar
    LaunchedEffect(uiState.isStreaming) {
        if (!uiState.isStreaming && uiState.messages.isNotEmpty()) {
            val lastMessage = uiState.messages.last()
            if (lastMessage.sender == MessageSender.AI && lastMessage.text.isNotBlank()) {
                currentSpokenText = lastMessage.text
                speaker.speak(lastMessage.text)
            }
        }
    }

    Scaffold(
        containerColor = CalmingBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CalmPulse",
                            style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                        )
                        Text(
                            text = if (isListening) "Ouvindo com calma..." else "Acolhimento imediato",
                            style = MaterialTheme.typography.labelSmall.copy(color = SageGreen)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CalmingBackground),
                actions = {
                    // Botão para mutar a voz a qualquer momento
                    if (isSpeaking) {
                        IconButton(onClick = { speaker.stop() }) {
                            Icon(Icons.Default.VolumeMute, contentDescription = "Mutar voz", tint = SageGreen)
                        }
                    }

                    // Botão para alternar visualização do Círculo de Respiração 4-7-8
                    IconButton(onClick = { showBreathingExercise = !showBreathingExercise }) {
                        Icon(
                            Icons.Default.Spa,
                            contentDescription = "Exercício de Respiração",
                            tint = if (showBreathingExercise) SageGreen else TextSecondary
                        )
                    }

                    // Botão para reiniciar o acolhimento (Novo Ciclo de Diálogo)
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = {
                            speaker.stop()
                            viewModel.resetChat()
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reiniciar acolhimento",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Barra de entrada híbrida (Texto + Voz)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .background(CalmingBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = uiState.inputText,
                        onValueChange = viewModel::onInputTextChanged,
                        placeholder = {
                            Text(
                                "Fale ou digite como você está...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (uiState.inputText.isNotBlank()) {
                        // Botão de Enviar Texto
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SageGreen)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
                        }
                    } else {
                        // Botão Flutuante de Voz (STT)
                        FloatingActionButton(
                            onClick = {
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
                            containerColor = if (isListening) SoftLavender else SageGreen,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Gravar voz")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Área de Respiração Retrátil / Alternável
            AnimatedVisibility(visible = showBreathingExercise) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BreathingCircle()
                }
            }

            // Banner sereno de aviso de voz / microfone (Check-in de acessibilidade)
            AnimatedVisibility(visible = voiceErrorMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftLavender.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { voiceErrorMessage = null }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = SageGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = voiceErrorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Lista de Mensagens do Chat
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Mensagem inicial de acolhimento automático se o chat estiver vazio
                if (uiState.messages.isEmpty() && !showBreathingExercise) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Olá. Estou aqui com você.\nRespire devagar e me conte o que está sentindo, no seu tempo.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextSecondary,
                                    lineHeight = 24.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
}

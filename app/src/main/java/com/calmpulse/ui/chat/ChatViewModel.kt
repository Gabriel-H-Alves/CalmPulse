package com.calmpulse.ui.chat

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calmpulse.data.model.ChatMessage
import com.calmpulse.data.model.MessageSender
import com.calmpulse.data.repository.GeminiChatRepository
import com.calmpulse.domain.repository.ChatRepository
import com.calmpulse.security.RateLimitException
import com.calmpulse.util.AgentNameDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val inputText: String = "",
    val userFeedbackMessage: String? = null,
    val agentName: String = "CalmPulse"
)

class ChatViewModel(
    application: Application,
    private val repository: ChatRepository = GeminiChatRepository()
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("calmpulse_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        ChatUiState(
            agentName = prefs.getString("agent_name", "CalmPulse") ?: "CalmPulse"
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        repository.setAgentName(_uiState.value.agentName)
    }

    fun updateAgentName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString("agent_name", trimmed).apply()
            repository.setAgentName(trimmed)
            _uiState.update { it.copy(agentName = trimmed) }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }

    fun sendMessage(userText: String = _uiState.value.inputText) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _uiState.value.isStreaming) return

        // Verifica se o usuário expressou o desejo de nomear ou renomear o agente
        val detectedName = AgentNameDetector.detectName(trimmed)
        val promptForAi = if (detectedName != null) {
            updateAgentName(detectedName)
            "$trimmed\n[Contexto: O usuário acabou de te batizar com o nome \"$detectedName\". Acolha esse gesto com muito carinho, valide o novo nome e confirme que adorou ser chamado(a) de $detectedName.]"
        } else {
            trimmed
        }

        // 1. Cria a mensagem do usuário com o texto original
        val userMessage = ChatMessage(
            text = trimmed,
            sender = MessageSender.USER
        )

        // 2. Cria a mensagem inicial vazia da IA em modo streaming
        val aiMessage = ChatMessage(
            text = "",
            sender = MessageSender.AI,
            isStreaming = true
        )

        // 3. Adiciona as duas mensagens na lista da tela e limpa o campo de texto
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + aiMessage,
                inputText = "",
                isStreaming = true,
                userFeedbackMessage = null
            )
        }

        // 4. Inicia a coleta do streaming na Coroutine do ViewModel
        viewModelScope.launch {
            try {
                repository.sendMessageStream(promptForAi)
                    .catch { error ->
                        if (error is RateLimitException) {
                            // Remove a mensagem de IA vazia e mostra o feedback suave
                            _uiState.update { state ->
                                val listWithoutEmptyAi = state.messages.filterNot { it.id == aiMessage.id }
                                state.copy(
                                    messages = listWithoutEmptyAi,
                                    isStreaming = false,
                                    userFeedbackMessage = error.reason
                                )
                            }
                        } else {
                            // Se for outro erro, exibe acolhimento seguro
                            _uiState.update { state ->
                                val updatedMessages = state.messages.toMutableList()
                                val lastIndex = updatedMessages.lastIndex
                                if (lastIndex >= 0 && updatedMessages[lastIndex].sender == MessageSender.AI) {
                                    updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                                        text = "Estou aqui com você. Respire fundo devagar... já vamos continuar.",
                                        isStreaming = false
                                    )
                                }
                                state.copy(messages = updatedMessages, isStreaming = false)
                            }
                        }
                    }
                    .collect { token ->
                        _uiState.update { state ->
                            val updatedMessages = state.messages.toMutableList()
                            val lastIndex = updatedMessages.lastIndex
                            if (lastIndex >= 0 && updatedMessages[lastIndex].sender == MessageSender.AI) {
                                val currentAiMessage = updatedMessages[lastIndex]
                                updatedMessages[lastIndex] = currentAiMessage.copy(
                                    text = currentAiMessage.text + token
                                )
                            }
                            state.copy(messages = updatedMessages)
                        }
                    }
            } finally {
                // 5. Garante que o streaming seja finalizado
                _uiState.update { state ->
                    val updatedMessages = state.messages.toMutableList()
                    val lastIndex = updatedMessages.lastIndex
                    if (lastIndex >= 0 && updatedMessages[lastIndex].id == aiMessage.id) {
                        updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(isStreaming = false)
                    }
                    state.copy(messages = updatedMessages, isStreaming = false)
                }
            }
        }
    }

    fun resetChat() {
        repository.resetChat()
        _uiState.update { ChatUiState() }
    }
}

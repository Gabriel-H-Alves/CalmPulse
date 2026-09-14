package com.calmpulse.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmpulse.data.model.ChatMessage
import com.calmpulse.data.model.MessageSender
import com.calmpulse.data.repository.GeminiChatRepository
import com.calmpulse.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val inputText: String = ""
)

class ChatViewModel(
    private val repository: ChatRepository = GeminiChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage(userText: String = _uiState.value.inputText) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _uiState.value.isStreaming) return

        // 1. Cria a mensagem do usuário
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
                isStreaming = true
            )
        }

        // 4. Inicia a coleta do streaming na Coroutine do ViewModel
        viewModelScope.launch {
            repository.sendMessageStream(trimmed).collect { token ->
                // A cada token emitido pela IA, atualizamos o texto da última mensagem!
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

            // 5. Quando o streaming termina, marcamos isStreaming = false
            _uiState.update { state ->
                val updatedMessages = state.messages.toMutableList()
                val lastIndex = updatedMessages.lastIndex
                if (lastIndex >= 0) {
                    updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(isStreaming = false)
                }
                state.copy(messages = updatedMessages, isStreaming = false)
            }
        }
    }

    fun resetChat() {
        repository.resetChat()
        _uiState.update { ChatUiState() }
    }
}

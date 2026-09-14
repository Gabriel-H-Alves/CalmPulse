package com.calmpulse.data.repository

import com.calmpulse.BuildConfig
import com.calmpulse.domain.prompt.SystemPrompt
import com.calmpulse.domain.repository.ChatRepository
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

class GeminiChatRepository : ChatRepository {

    // 1. Inicializamos o modelo do Gemini com o System Prompt com as regras de acolhimento
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-3.6-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = com.google.ai.client.generativeai.type.content {
                text(SystemPrompt.CALM_PULSE_INSTRUCTION)
            }
        )
    }

    // 2. Mantém a sessão de diálogo multi-turn na memória (para conduzir o 5-4-3-2-1 passo a passo)
    private var chatSession: Chat? = null

    @Synchronized
    private fun getOrCreateChat(): Chat {
        return chatSession ?: generativeModel.startChat().also { chatSession = it }
    }

    // 3. Envia mensagem mantendo o histórico de turnos anteriores
    override fun sendMessageStream(userPrompt: String): Flow<String> =
        getOrCreateChat()
            .sendMessageStream(userPrompt)
            .mapNotNull { it.text }
            .catch { _ ->
                // Regra de Ouro do Desafio: Tratamento Gracioso de Erros
                // Nunca mostrar telas vermelhas ou erros técnicos como "SocketTimeoutException"
                emit("Estou aqui com você. Respire fundo devagar... já vamos continuar.")
            }
            .flowOn(Dispatchers.IO) // Execução em thread de rede (IO)

    override fun resetChat() {
        chatSession = null
    }
}

package com.calmpulse.data.repository

import com.calmpulse.BuildConfig
import com.calmpulse.domain.prompt.SystemPrompt
import com.calmpulse.domain.repository.ChatRepository
import com.calmpulse.security.ClientRateLimiter
import com.calmpulse.security.InputSanitizer
import com.calmpulse.security.RateLimitResult
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

class GeminiChatRepository(
    private val rateLimiter: ClientRateLimiter = ClientRateLimiter()
) : ChatRepository {

    // 1. Inicializamos o modelo do Gemini oficial com o System Prompt
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = com.google.ai.client.generativeai.type.content {
                text(SystemPrompt.CALM_PULSE_INSTRUCTION)
            }
        )
    }

    // 2. Mantém a sessão de diálogo multi-turn na memória
    private var chatSession: Chat? = null

    @Synchronized
    private fun getOrCreateChat(): Chat {
        return chatSession ?: generativeModel.startChat().also { chatSession = it }
    }

    // 3. Envia mensagem aplicando sanitização, rate limiting e resiliência
    override fun sendMessageStream(userPrompt: String): Flow<String> {
        // A. Sanitização e validação de tamanho / caracteres de controle
        val sanitized = InputSanitizer.sanitize(userPrompt)
        if (sanitized.isBlank()) {
            return flow { emit("Estou aqui com você. Pode falar com calma no seu tempo.") }
        }

        // B. Verificação de segurança (Anti-Prompt Injection / Sequestro)
        if (!InputSanitizer.isSafe(sanitized)) {
            return flow {
                emit("Estou focado em cuidar de você e te ajudar a se acalmar agora. Vamos respirar fundo juntos?")
            }
        }

        // C. Mitigação de Flooding e Exaustão de Cota (Rate Limiting)
        when (val rateCheck = rateLimiter.tryAcquire()) {
            is RateLimitResult.Denied -> {
                return flow { emit(rateCheck.reason) }
            }
            is RateLimitResult.Allowed -> { /* Prossegue com a chamada */ }
        }

        // D. Verificação de disponibilidade da Chave de API
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return flow {
                emit("Estou aqui com você. Respire fundo devagar... já vamos continuar.")
            }
        }

        // E. Execução protegida do streaming de tokens
        return getOrCreateChat()
            .sendMessageStream(sanitized)
            .mapNotNull { it.text }
            .catch { _ ->
                // Tratamento gracioso sem expor detalhes técnicos ou falhas de infraestrutura
                emit("Estou aqui com você. Respire fundo devagar... já vamos continuar.")
            }
            .flowOn(Dispatchers.IO)
    }

    override fun resetChat() {
        chatSession = null
        rateLimiter.reset()
    }
}

package com.calmpulse.data.repository

import android.util.Log
import com.calmpulse.BuildConfig
import com.calmpulse.domain.prompt.SystemPrompt
import com.calmpulse.domain.repository.ChatRepository
import com.calmpulse.security.ClientRateLimiter
import com.calmpulse.security.InputSanitizer
import com.calmpulse.security.RateLimitException
import com.calmpulse.security.RateLimitResult
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

/**
 * Repositório do Gemini com cascata de modelos resiliente (Model Cascade)
 * e política de retentativa inteligente com backoff exponencial.
 */
class GeminiChatRepository(
    private val rateLimiter: ClientRateLimiter = ClientRateLimiter()
) : ChatRepository {

    companion object {
        private const val TAG = "GeminiChatRepo"

        // Cascata de modelos em ordem de prioridade
        val CANDIDATE_MODELS = listOf(
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-flash-latest"
        )
        private const val MAX_RETRIES_PER_MODEL = 2
    }

    private var activeModelIndex = 0
    private var chatSession: Chat? = null

    private fun createGenerativeModel(modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = com.google.ai.client.generativeai.type.content {
                text(SystemPrompt.CALM_PULSE_INSTRUCTION)
            }
        )
    }

    @Synchronized
    private fun getOrCreateChat(): Pair<Chat, String> {
        val modelName = CANDIDATE_MODELS[activeModelIndex.coerceIn(0, CANDIDATE_MODELS.lastIndex)]
        val session = chatSession ?: createGenerativeModel(modelName)
            .startChat()
            .also { chatSession = it }
        return Pair(session, modelName)
    }

    override fun sendMessageStream(userPrompt: String): Flow<String> = flow {
        val sanitized = InputSanitizer.sanitize(userPrompt)
        if (sanitized.isBlank()) {
            emit("Estou aqui com você. Pode falar com calma no seu próprio tempo.")
            return@flow
        }

        if (!InputSanitizer.isSafe(sanitized)) {
            emit("Estou focado em cuidar de você e te ajudar a se acalmar agora. Vamos respirar fundo juntos?")
            return@flow
        }

        // Validação de taxa (Rate Limit)
        when (val rateCheck = rateLimiter.tryAcquire()) {
            is RateLimitResult.Denied -> {
                throw RateLimitException(rateCheck.reason, rateCheck.retryAfterMillis)
            }
            is RateLimitResult.Allowed -> { /* Continua */ }
        }

        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            Log.e(TAG, "GEMINI_API_KEY está vazia no BuildConfig!")
            emit("Chave de inteligência artificial não configurada. Verifique as credenciais do CalmPulse.")
            return@flow
        }

        // Tentativa de execução com cascata de modelos e retentativa
        var lastException: Exception? = null
        var streamedAnyToken = false

        for (modelIdx in activeModelIndex until CANDIDATE_MODELS.size) {
            val candidate = CANDIDATE_MODELS[modelIdx]

            for (attempt in 1..MAX_RETRIES_PER_MODEL) {
                try {
                    val model = createGenerativeModel(candidate)
                    val chat = if (modelIdx == activeModelIndex && chatSession != null) {
                        chatSession!!
                    } else {
                        model.startChat().also {
                            chatSession = it
                            activeModelIndex = modelIdx
                        }
                    }

                    chat.sendMessageStream(sanitized)
                        .mapNotNull { it.text }
                        .collect { token ->
                            streamedAnyToken = true
                            emit(token)
                        }

                    // Sucesso total no streaming
                    return@flow

                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Falha no modelo $candidate (tentativa $attempt/$MAX_RETRIES_PER_MODEL): ${e.javaClass.simpleName}")

                    // Se já começou a emitir tokens para o usuário, não devemos trocar no meio
                    if (streamedAnyToken) {
                        emit("\n\n(A conexão oscilou, mas você pode me dizer se quer continuar daqui.)")
                        return@flow
                    }

                    // Se for 503 (indisponível) ou erro transitório, espera com backoff exponencial
                    if (attempt < MAX_RETRIES_PER_MODEL) {
                        delay(attempt * 400L)
                    }
                }
            }

            // Alternar para o próximo modelo na cascata
            Log.i(TAG, "Alternando para o próximo modelo da lista...")
            chatSession = null
        }

        // Se todos os modelos falharam
        Log.e(TAG, "Todos os modelos da cascata falharam. Tipo de erro: ${lastException?.javaClass?.simpleName}")
        emit("Estou aqui com você. Houve uma oscilação momentânea de conexão, mas sigo ao seu lado. Respire fundo e me diga como posso te ajudar agora.")

    }.flowOn(Dispatchers.IO)

    override fun resetChat() {
        chatSession = null
        activeModelIndex = 0
        rateLimiter.reset()
    }
}

package com.calmpulse.data.repository

import android.util.Log
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

    companion object {
        private const val TAG = "GeminiChatRepo"
        private const val PRIMARY_MODEL = "gemini-3.6-flash"
        private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"
    }

    private fun createGenerativeModel(modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = com.google.ai.client.generativeai.type.content {
                text(SystemPrompt.CALM_PULSE_INSTRUCTION)
            }
        )
    }

    private val primaryModel by lazy { createGenerativeModel(PRIMARY_MODEL) }
    private val fallbackModel by lazy { createGenerativeModel(FALLBACK_MODEL) }

    private var chatSession: Chat? = null
    private var usingFallback = false

    @Synchronized
    private fun getOrCreateChat(): Chat {
        return chatSession ?: (if (usingFallback) fallbackModel else primaryModel)
            .startChat()
            .also { chatSession = it }
    }

    override fun sendMessageStream(userPrompt: String): Flow<String> {
        val sanitized = InputSanitizer.sanitize(userPrompt)
        if (sanitized.isBlank()) {
            return flow { emit("Estou aqui com você. Pode falar com calma no seu tempo.") }
        }

        if (!InputSanitizer.isSafe(sanitized)) {
            return flow {
                emit("Estou focado em cuidar de você e te ajudar a se acalmar agora. Vamos respirar fundo juntos?")
            }
        }

        when (val rateCheck = rateLimiter.tryAcquire()) {
            is RateLimitResult.Denied -> {
                return flow { emit(rateCheck.reason) }
            }
            is RateLimitResult.Allowed -> { /* Prossegue com a requisição */ }
        }

        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            Log.e(TAG, "GEMINI_API_KEY está vazia no BuildConfig!")
            return flow {
                emit("Estou aqui com você. Respire fundo devagar... já vamos continuar.")
            }
        }

        return getOrCreateChat()
            .sendMessageStream(sanitized)
            .mapNotNull { it.text }
            .catch { error ->
                Log.e(TAG, "Erro na resposta do modelo primário: ${error.message}", error)
                // Se falhou no modelo primário, tenta alternar para o fallback
                if (!usingFallback) {
                    usingFallback = true
                    chatSession = null
                    try {
                        getOrCreateChat()
                            .sendMessageStream(sanitized)
                            .mapNotNull { it.text }
                            .collect { token -> emit(token) }
                        return@catch
                    } catch (fallbackError: Exception) {
                        Log.e(TAG, "Erro também no fallback: ${fallbackError.message}", fallbackError)
                    }
                }
                emit("Estou aqui com você. Respire fundo devagar... já vamos continuar.")
            }
            .flowOn(Dispatchers.IO)
    }

    override fun resetChat() {
        chatSession = null
        usingFallback = false
        rateLimiter.reset()
    }
}

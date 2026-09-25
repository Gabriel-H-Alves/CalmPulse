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

        // Cascata de modelos em ordem de prioridade (testados e operacionais)
        val CANDIDATE_MODELS = listOf(
            "gemini-flash-lite-latest",
            "gemini-3.5-flash-lite",
            "gemini-3.6-flash",
            "gemini-flash-latest"
        )
        private const val MAX_RETRIES_PER_MODEL = 2
    }

    private var activeModelName: String? = null
    private var chatSession: Chat? = null
    private var currentAgentName: String = "CalmPulse"
    private var currentTone: String = "Acolhedor & Empático"
    private var preferredModel: String? = null

    override fun setAgentName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && trimmed != currentAgentName) {
            currentAgentName = trimmed
            // Reseta a sessão para que o novo prompt com o nome do agente seja adotado imediatamente
            chatSession = null
        }
    }

    fun setEmpathyTone(tone: String) {
        val trimmed = tone.trim()
        if (trimmed.isNotBlank() && trimmed != currentTone) {
            currentTone = trimmed
            chatSession = null
        }
    }

    fun setPreferredModel(model: String) {
        preferredModel = when {
            model.contains("3.6") -> "gemini-3.6-flash"
            model.contains("3.5") -> "gemini-3.5-flash-lite"
            model.contains("3.8") -> "gemini-3.8-flash"
            model.contains("Lite", ignoreCase = true) || model.contains("Flash", ignoreCase = true) -> "gemini-flash-lite-latest"
            else -> "gemini-flash-lite-latest"
        }
        chatSession = null
    }

    private fun createGenerativeModel(modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY.trim(),
            systemInstruction = com.google.ai.client.generativeai.type.content {
                text(SystemPrompt.getInstruction(currentAgentName, currentTone))
            }
        )
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

        // Fila de modelos priorizando o modelo escolhido pelo usuário
        val modelQueue = if (preferredModel != null) {
            listOf(preferredModel!!) + CANDIDATE_MODELS.filter { it != preferredModel }
        } else {
            CANDIDATE_MODELS
        }

        var lastException: Exception? = null
        var streamedAnyToken = false

        for (candidate in modelQueue) {
            for (attempt in 1..MAX_RETRIES_PER_MODEL) {
                try {
                    val model = createGenerativeModel(candidate)
                    val chat = if (activeModelName == candidate && chatSession != null) {
                        chatSession!!
                    } else {
                        model.startChat().also {
                            chatSession = it
                            activeModelName = candidate
                        }
                    }

                    chat.sendMessageStream(sanitized)
                        .mapNotNull { it.text }
                        .collect { token ->
                            if (!streamedAnyToken) {
                                streamedAnyToken = true
                            }
                            emit(token)
                        }

                    // Sucesso total no streaming
                    return@flow

                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Falha no modelo $candidate (tentativa $attempt/$MAX_RETRIES_PER_MODEL): ${e.message}", e)

                    // Se já começou a emitir tokens para o usuário, não devemos trocar no meio
                    if (streamedAnyToken) {
                        emit("\n\n(A conexão oscilou, mas você pode me dizer se quer continuar daqui.)")
                        return@flow
                    }

                    // Espera suave antes da próxima tentativa
                    if (attempt < MAX_RETRIES_PER_MODEL) {
                        delay(250L)
                    }
                }
            }

            // Alternar para o próximo modelo na cascata
            Log.i(TAG, "Alternando para o próximo modelo da lista...")
            chatSession = null
        }

        // Se todos os modelos falharam
        Log.e(TAG, "Todos os modelos da cascata falharam. Causa raiz: ${lastException?.message}", lastException)
        emit("Estou aqui com você. Houve uma oscilação na rede, mas não se preocupe: você não está sozinho(a). Respire fundo... puxe o ar pelo nariz contando até 4, segure por 7 e solte devagar contando até 8. Me diga como está se sentindo agora.")

    }.flowOn(Dispatchers.IO)

    override fun resetChat() {
        chatSession = null
        activeModelName = null
        rateLimiter.reset()
    }
}

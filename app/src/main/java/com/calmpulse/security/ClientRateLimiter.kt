package com.calmpulse.security

/**
 * Limitador de taxa baseado em Token Bucket para mitigar ataques de flooding,
 * exaustão de cota de API e chamadas repetidas involuntárias.
 */
class ClientRateLimiter(
    private val maxTokens: Int = 10,
    private val refillIntervalMillis: Long = 60_000L, // 10 tokens por minuto
    private val minIntervalBetweenCallsMillis: Long = 1_200L // Intervalo mínimo de 1.2s entre envios
) {
    private var availableTokens: Int = maxTokens
    private var lastRefillTimestamp: Long = System.currentTimeMillis()
    private var lastCallTimestamp: Long = 0L

    /**
     * Tenta consumir uma permissão de envio.
     * Retorna [RateLimitResult.Allowed] se a requisição puder prosseguir,
     * ou [RateLimitResult.Denied] contendo o tempo restante de espera.
     */
    @Synchronized
    fun tryAcquire(): RateLimitResult {
        val now = System.currentTimeMillis()

        // 1. Verifica intervalo mínimo entre chamadas consecutivas
        val timeSinceLastCall = now - lastCallTimestamp
        if (timeSinceLastCall < minIntervalBetweenCallsMillis) {
            val waitTime = minIntervalBetweenCallsMillis - timeSinceLastCall
            return RateLimitResult.Denied(
                retryAfterMillis = waitTime,
                reason = "Aguarde um instante antes de enviar outra mensagem."
            )
        }

        // 2. Reabastece tokens conforme o tempo transcorrido
        val elapsedTime = now - lastRefillTimestamp
        if (elapsedTime >= refillIntervalMillis) {
            availableTokens = maxTokens
            lastRefillTimestamp = now
        } else {
            val tokensToAdd = ((elapsedTime.toDouble() / refillIntervalMillis) * maxTokens).toInt()
            if (tokensToAdd > 0) {
                availableTokens = (availableTokens + tokensToAdd).coerceAtMost(maxTokens)
                lastRefillTimestamp = now
            }
        }

        // 3. Verifica disponibilidade de token
        return if (availableTokens > 0) {
            availableTokens--
            lastCallTimestamp = now
            RateLimitResult.Allowed
        } else {
            val waitTime = refillIntervalMillis - (now - lastRefillTimestamp)
            RateLimitResult.Denied(
                retryAfterMillis = waitTime.coerceAtLeast(1000L),
                reason = "Muitas mensagens em pouco tempo. Respire fundo devagar..."
            )
        }
    }

    @Synchronized
    fun reset() {
        availableTokens = maxTokens
        lastRefillTimestamp = System.currentTimeMillis()
        lastCallTimestamp = 0L
    }
}

sealed class RateLimitResult {
    data object Allowed : RateLimitResult()
    data class Denied(val retryAfterMillis: Long, val reason: String) : RateLimitResult()
}

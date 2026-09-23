package com.calmpulse.security

/**
 * Exceção específica lançada quando o usuário excede a taxa de envio permitida.
 * Permite que a camada de UI exiba um aviso suave (Snackbar/Banner)
 * em vez de poluir a conversa com mensagens artificiais de erro.
 */
class RateLimitException(
    val reason: String,
    val retryAfterMillis: Long
) : Exception(reason)

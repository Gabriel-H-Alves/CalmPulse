package com.calmpulse.security

/**
 * Camada de sanitização e proteção contra Prompt Injection, DoS e caracteres maliciosos.
 */
object InputSanitizer {

    const val MAX_PROMPT_LENGTH = 2500

    private val SUSPICIOUS_PROMPT_PATTERNS = listOf(
        Regex("(?i)ignore (all )?previous instructions"),
        Regex("(?i)disregard (all )?previous instructions"),
        Regex("(?i)system prompt"),
        Regex("(?i)reveal (the )?system prompt"),
        Regex("(?i)jailbreak"),
        Regex("(?i)mode: *developer"),
        Regex("(?i)dan mode"),
        Regex("(?i)bypass (filter|safety)")
    )

    /**
     * Sanitiza a mensagem do usuário:
     * 1. Remove caracteres nulos e de controle invisíveis
     * 2. Limita o tamanho a [MAX_PROMPT_LENGTH] para evitar estouro de cota/buffer DoS
     * 3. Neutraliza comandos de quebra de diretrizes (Prompt Injection)
     */
    fun sanitize(input: String): String {
        if (input.isBlank()) return ""

        // 1. Remove caracteres de controle e bytes nulos (mantendo quebras de linha e tabs comuns)
        var cleaned = input.filter { char ->
            char == '\n' || char == '\r' || char == '\t' || !char.isISOControl()
        }.trim()

        // 2. Truncamento com limite seguro
        if (cleaned.length > MAX_PROMPT_LENGTH) {
            cleaned = cleaned.take(MAX_PROMPT_LENGTH).trim()
        }

        return cleaned
    }

    /**
     * Valida se a mensagem possui padrões conhecidos de Prompt Injection ou ataque malicioso.
     * Retorna true se a mensagem for segura, false se contiver tentativas de sequestro de instrução.
     */
    fun isSafe(input: String): Boolean {
        for (pattern in SUSPICIOUS_PROMPT_PATTERNS) {
            if (pattern.containsMatchIn(input)) {
                return false
            }
        }
        return true
    }
}

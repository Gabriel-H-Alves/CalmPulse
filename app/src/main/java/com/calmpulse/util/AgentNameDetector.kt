package com.calmpulse.util

/**
 * Utilitário de detecção de intenção do usuário para nomear o agente acolhedor.
 * Permite que o usuário estabeleça um vínculo humano e afetuoso personalizando o nome
 * (ex: "Eu desejo que seu nome seja Julinha", "Quero te chamar de Julinha", etc.).
 */
object AgentNameDetector {

    private val NAME_PATTERNS = listOf(
        // "Eu desejo que seu nome seja X" / "Quero que seu nome seja X" / "Gostaria que seu nome fosse X"
        Regex("(?i)(?:eu\\s+)?(?:desejo|quero|gostaria|espero|prefiro)\\s+(?:que\\s+)?(?:o\\s+)?seu\\s+nome\\s+(?:seja|fosse|vire)\\s+([\\p{L}0-9_\\-\\s]{2,25})"),
        
        // "Quero te chamar de X" / "Vou te chamar de X" / "Gostaria de te chamar de X" / "Posso te chamar de X?"
        Regex("(?i)(?:eu\\s+)?(?:quero|vou|gostaria\\s+de|posso|prefiro|desejo)\\s+te\\s+chamar\\s+de\\s+([\\p{L}0-9_\\-\\s]{2,25})"),
        
        // "Chamar você de X"
        Regex("(?i)(?:quero|vou|gostaria\\s+de|posso)\\s+chamar\\s+voc[eê]\\s+de\\s+([\\p{L}0-9_\\-\\s]{2,25})"),

        // "Seu nome agora é X" / "Seu nome a partir de agora é X" / "Seu nome vai ser X" / "Seu nome será X"
        Regex("(?i)(?:a\\s+partir\\s+de\\s+agora\\s+|de\\s+agora\\s+em\\s+diante\\s+)?seu\\s+(?:novo\\s+)?nome\\s+(?:agora\\s+)?(?:[eé]|vai\\s+ser|ser[aá])\\s+([\\p{L}0-9_\\-\\s]{2,25})"),

        // "A partir de agora você se chama X" / "Você agora se chama X"
        Regex("(?i)(?:a\\s+partir\\s+de\\s+agora\\s+|de\\s+agora\\s+em\\s+diante\\s+)?voc[eê]\\s+(?:agora\\s+)?se\\s+chama\\s+([\\p{L}0-9_\\-\\s]{2,25})"),

        // "Mude seu nome para X" / "Troque seu nome para X" / "Altere seu nome para X"
        Regex("(?i)(?:mude|troque|altere)\\s+(?:o\\s+)?(?:seu\\s+)?nome\\s+para\\s+([\\p{L}0-9_\\-\\s]{2,25})"),

        // "Te batizo de X"
        Regex("(?i)te\\s+batizo\\s+de\\s+([\\p{L}0-9_\\-\\s]{2,25})")
    )

    /**
     * Tenta extrair o nome desejado a partir do texto do usuário.
     * Retorna o nome formatado (Title Case, sem pontuações) se detectado, ou null se não houver intenção de nomeação.
     */
    fun detectName(text: String): String? {
        val sanitized = text.trim()
            .replace(Regex("[.,!?;:\"']+$"), "")
            .trim()

        for (pattern in NAME_PATTERNS) {
            val match = pattern.find(sanitized)
            if (match != null && match.groupValues.size > 1) {
                val rawCandidate = match.groupValues[1].trim()
                val words = rawCandidate
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .take(3) // No máximo 3 palavras para o nome

                if (words.isNotEmpty()) {
                    val formatted = words.joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase() else char.toString()
                        }
                    }
                    if (formatted.length in 2..30) {
                        return formatted
                    }
                }
            }
        }
        return null
    }
}

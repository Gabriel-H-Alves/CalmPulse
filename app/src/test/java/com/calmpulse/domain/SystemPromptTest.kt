package com.calmpulse.domain

import com.calmpulse.domain.prompt.SystemPrompt
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para garantir a integridade dos mandatos éticos e clínicos do System Prompt.
 */
class SystemPromptTest {

    @Test
    fun `prompt should contain essential anti-crisis rules and protocols`() {
        val prompt = SystemPrompt.CALM_PULSE_INSTRUCTION

        // Regra 1: Respostas curtas
        assertTrue("Prompt deve exigir respostas curtas", prompt.contains("Respostas Curtas") || prompt.contains("2 a 3 frases"))

        // Regra 2: Não diagnosticar
        assertTrue("Prompt deve proibir diagnóstico médico", prompt.contains("Não Diagnostique"))

        // Regra 3: Passo a passo sequencial
        assertTrue("Prompt deve orientar passo a passo", prompt.contains("Passo a Passo"))

        // Regra 4: Protocolo 5-4-3-2-1
        assertTrue("Prompt deve conter o protocolo 5-4-3-2-1", prompt.contains("5-4-3-2-1"))

        // Regra 5: Respiração 4-7-8
        assertTrue("Prompt deve conter a respiração 4-7-8", prompt.contains("4-7-8"))

        // Regra 6: Acolhimento antes de prescrição
        assertTrue("Prompt deve exigir acolhimento antes de exercícios", prompt.contains("Acolhimento Antes da Prescrição"))

        // Regra 7: Detecção de perfis psicológicos
        assertTrue("Prompt deve contemplar adaptação de perfis", prompt.contains("Detecção e Adaptação de Perfil"))

        // Regra 8: Regras anti-robô (proibição de bullet points e exclamações)
        assertTrue("Prompt deve proibir bullet points mecânicos", prompt.contains("bullet points"))
    }
}

package com.calmpulse.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de comunicação com o Agente de IA.
 * Suporta conversação Multi-Turn (histórico encadeado) e streaming de tokens.
 */
interface ChatRepository {
    /**
     * Envia a mensagem do usuário dentro da sessão com memória de contexto
     * e emite os pedaços (tokens) da resposta em tempo real.
     */
    fun sendMessageStream(userPrompt: String): Flow<String>

    /**
     * Reinicia o contexto da sessão para um novo ciclo de acolhimento.
     */
    fun resetChat()

    /**
     * Atualiza o nome do agente para personalização afetiva do usuário.
     */
    fun setAgentName(name: String) {}
}
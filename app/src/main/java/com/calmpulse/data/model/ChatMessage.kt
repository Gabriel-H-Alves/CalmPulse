package com.calmpulse.data.model

import java.util.UUID

/**
 * Representa uma mensagem dentro do chat de acolhimento.
 *
 * @property id identicador único para a lista do Compose (LazyColumn) rederizar com performance.
 * @property texte conteúdo da mensagem (vai sendo atualizado caractere por caractere no streaming).
 * @property sender Quem enviou (USER ou IA).
 * @property isStreaming Indica se a IA ainda está gerando essa resposta em tempo real.
 * @property timestamp Momento em que a mensagem foi criada.
 */


 enum class MessageSender {
      USER,
      AI
 }
 data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()

 )
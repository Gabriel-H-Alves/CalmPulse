package com.calmpulse.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Gerenciador de Text-to-Speech com calibração serena para crises de ansiedade.
 */
class VoiceSpeaker(
    context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Tenta configurar o idioma em Português do Brasil
                val result = tts?.setLanguage(Locale("pt", "BR"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                    
                    // REQUISITO DO DESAFIO: Tom sereno e desacelerado
                    tts?.setSpeechRate(0.85f) // Redução de velocidade para ritmo calmante
                    tts?.setPitch(0.90f)      // Tom ligeiramente mais aveludado/grave
                }
            }
        }

        // Listener para saber quando a voz começa e quando ela termina de falar
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingStateChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingStateChanged(false)
            }

            override fun onError(utteranceId: String?) {
                onSpeakingStateChanged(false)
            }
        })
    }

    /**
     * Fala o texto fornecido. Interrompe falas anteriores se houver.
     */
    fun speak(text: String) {
        if (!isInitialized || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CalmPulseUtterance")
    }

    /**
     * Permite ao usuário pausar/mutar a voz com um único clique.
     */
    fun stop() {
        tts?.stop()
        onSpeakingStateChanged(false)
    }

    /**
     * Libera recursos de áudio da memória do Android quando o app fechar.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

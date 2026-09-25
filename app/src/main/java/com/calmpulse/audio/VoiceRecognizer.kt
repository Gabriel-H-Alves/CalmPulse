package com.calmpulse.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Gerenciador reativo e resiliente do SpeechRecognizer do Android.
 * Suporta streaming de transcrição parcial em tempo real, monitoramento de RMS (volume da voz),
 * recriação automática pós-erro (prevenção de travamentos do binder) e fallback gracioso.
 */
class VoiceRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onRmsUpdate: (Float) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "VoiceRecognizer"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var latestPartialText: String = ""
    private var isCurrentlyListening = false

    /**
     * Inicia a escuta da voz do usuário imediatamente com feedback visual sem latência.
     */
    fun startListening() {
        mainHandler.post {
            try {
                // Limpa instância anterior para evitar ERROR_RECOGNIZER_BUSY
                cleanUpRecognizer()

                latestPartialText = ""
                isCurrentlyListening = true
                this@VoiceRecognizer.onListeningStateChanged(true)

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "Reconhecimento de fala não disponível neste dispositivo/emulador.")
                    isCurrentlyListening = false
                    this@VoiceRecognizer.onListeningStateChanged(false)
                    this@VoiceRecognizer.onError("Reconhecimento de voz indisponível no dispositivo. Digite sua mensagem.")
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(TAG, "onReadyForSpeech: microfone pronto para captar")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "onBeginningOfSpeech: usuário começou a falar")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            if (isCurrentlyListening) {
                                this@VoiceRecognizer.onRmsUpdate(rmsdB)
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d(TAG, "onEndOfSpeech: término da fala detectado")
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim()
                            if (!text.isNullOrBlank()) {
                                latestPartialText = text
                                Log.d(TAG, "onPartialResults: $text")
                                this@VoiceRecognizer.onPartialResult(text)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val finalText = matches?.firstOrNull()?.trim() ?: latestPartialText
                            Log.d(TAG, "onResults: $finalText")
                            
                            isCurrentlyListening = false
                            this@VoiceRecognizer.onListeningStateChanged(false)
                            cleanUpRecognizer()

                            if (finalText.isNotBlank()) {
                                this@VoiceRecognizer.onResult(finalText)
                            } else {
                                this@VoiceRecognizer.onError("Não ouvimos sua mensagem. Fale novamente ou digite.")
                            }
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "SpeechRecognizer onError código: $error (latest: '$latestPartialText')")
                            isCurrentlyListening = false
                            this@VoiceRecognizer.onListeningStateChanged(false)
                            val textToSend = latestPartialText.trim()
                            cleanUpRecognizer()

                            // Preserva a transcrição já capturada para envio, se houver
                            if (textToSend.isNotBlank()) {
                                this@VoiceRecognizer.onResult(textToSend)
                            } else {
                                val friendlyMsg = when (error) {
                                    SpeechRecognizer.ERROR_NO_MATCH -> "Não consegui te ouvir com clareza. Fale novamente ou digite."
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Pausa longa detectada. Fale quando estiver à vontade."
                                    SpeechRecognizer.ERROR_AUDIO -> "Problema momentâneo no áudio. Tente novamente."
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone necessária para acolhimento por voz."
                                    else -> "Gravação finalizada. Fale de novo ou digite sua mensagem."
                                }
                                this@VoiceRecognizer.onError(friendlyMsg)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR"))
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)

            } catch (e: Exception) {
                Log.e(TAG, "Exceção ao iniciar escuta: ${e.message}", e)
                isCurrentlyListening = false
                onListeningStateChanged(false)
                cleanUpRecognizer()
                onError("Erro ao iniciar gravação: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Finaliza a escuta e processa o que foi capturado até o momento.
     */
    fun stopListening() {
        mainHandler.post {
            try {
                if (isCurrentlyListening) {
                    speechRecognizer?.stopListening()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao parar escuta: ${e.message}")
            }
        }
    }

    /**
     * Cancela e descarta qualquer gravação em andamento.
     */
    fun cancelListening() {
        mainHandler.post {
            latestPartialText = ""
            isCurrentlyListening = false
            onListeningStateChanged(false)
            cleanUpRecognizer()
        }
    }

    private fun cleanUpRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao destruir speechRecognizer: ${e.message}")
        } finally {
            speechRecognizer = null
        }
    }

    fun destroy() {
        cancelListening()
    }
}

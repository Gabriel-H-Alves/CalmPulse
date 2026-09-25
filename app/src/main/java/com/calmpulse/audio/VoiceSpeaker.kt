package com.calmpulse.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import com.calmpulse.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Gerenciador Híbrido de Voz Terapêutica do CalmPulse:
 * - Motor 1: Gemini Neural Audio (Estúdio • Humana e Aveludada) via gemini-3.8-flash-tts
 * - Motor 2: TTS Nativo do Sistema (com seleção inteligente de vozes de alta fidelidade)
 */
class VoiceSpeaker(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit = {}
) {
    companion object {
        private const val TAG = "VoiceSpeaker"
        const val ENGINE_GEMINI_NEURAL = "GEMINI_NEURAL"
        const val ENGINE_SYSTEM_TTS = "SYSTEM_TTS"

        val NEURAL_VOICES = listOf(
            Pair("Aoede", "Aoede (Serena • Feminina)"),
            Pair("Kore", "Kore (Calorosa • Feminina)"),
            Pair("Charon", "Charon (Profundo • Masculino)"),
            Pair("Fenrir", "Fenrir (Tranquilo • Masculino)")
        )
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var speakJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val prefs = context.getSharedPreferences("calmpulse_prefs", Context.MODE_PRIVATE)

    var currentEngine: String = prefs.getString("voice_engine", ENGINE_GEMINI_NEURAL) ?: ENGINE_GEMINI_NEURAL
        private set

    var currentVoiceName: String = prefs.getString("neural_voice_name", "Aoede") ?: "Aoede"
        private set

    var currentRate: Float = parseRate(prefs.getString("tts_rate_label", "0.85x (Sereno)") ?: "0.85x (Sereno)")
        private set

    init {
        initMediaPlayer()
        initSystemTts()
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                onSpeakingStateChanged(false)
            }
            setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra")
                onSpeakingStateChanged(false)
                true
            }
        }
    }

    private fun initSystemTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("pt", "BR"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true
                    selectBestSystemVoice()
                    tts?.setSpeechRate(currentRate)
                    tts?.setPitch(0.92f)
                }
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingStateChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingStateChanged(false)
            }

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                onSpeakingStateChanged(false)
            }
        })
    }

    private fun selectBestSystemVoice() {
        try {
            val voices = tts?.voices ?: return
            val ptBrVoices = voices.filter { it.locale.language == "pt" && it.locale.country == "BR" }
            val highQualityVoice = ptBrVoices.firstOrNull { voice ->
                voice.quality >= Voice.QUALITY_HIGH && !voice.isNetworkConnectionRequired
            } ?: ptBrVoices.firstOrNull()

            if (highQualityVoice != null) {
                tts?.voice = highQualityVoice
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível selecionar voz específica do sistema: ${e.message}")
        }
    }

    fun setVoiceEngine(engine: String) {
        currentEngine = engine
        prefs.edit().putString("voice_engine", engine).apply()
    }

    fun setNeuralVoice(voiceName: String) {
        currentVoiceName = voiceName
        prefs.edit().putString("neural_voice_name", voiceName).apply()
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate
        tts?.setSpeechRate(rate)
    }

    fun updateRateFromLabel(label: String) {
        setSpeechRate(parseRate(label))
    }

    private fun parseRate(label: String): Float = when {
        label.contains("1.25") -> 1.25f
        label.contains("1.0") -> 1.0f
        else -> 0.85f
    }

    /**
     * Fala o texto. Se o motor for Gemini Neural, sintetiza em HD.
     * Caso haja indisponibilidade de rede, faz fallback suave para o TTS nativo.
     */
    fun speak(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        stop()

        if (currentEngine == ENGINE_GEMINI_NEURAL && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            speakWithGeminiNeural(trimmed, currentVoiceName)
        } else {
            speakWithSystemTts(trimmed)
        }
    }

    /**
     * Pré-visualização rápida de voz nas configurações
     */
    fun speakPreview(voiceName: String) {
        stop()
        val previewText = "Olá, estou aqui com você para te acolher com muita calma."
        speakWithGeminiNeural(previewText, voiceName)
    }

    private fun speakWithGeminiNeural(text: String, voiceName: String) {
        speakJob?.cancel()
        speakJob = scope.launch {
            onSpeakingStateChanged(true)
            val audioFile = withContext(Dispatchers.IO) {
                synthesizeGeminiAudio(text, voiceName)
            }

            if (audioFile != null && audioFile.exists()) {
                playAudioFile(audioFile)
            } else {
                Log.w(TAG, "Fallback para TTS do sistema devido a falha no Gemini Audio")
                speakWithSystemTts(text)
            }
        }
    }

    private fun synthesizeGeminiAudio(text: String, voiceName: String): File? {
        return try {
            val key = BuildConfig.GEMINI_API_KEY.trim()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash-tts:generateContent?key=$key"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", text)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini Audio HTTP erro: ${response.code}")
                return null
            }

            val responseBody = response.body?.string() ?: return null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val firstPart = parts.optJSONObject(0) ?: return null
            val inlineData = firstPart.optJSONObject("inlineData") ?: return null
            val base64Data = inlineData.optString("data", "")

            if (base64Data.isBlank()) return null

            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val cacheFile = File(context.cacheDir, "gemini_voice_stream.wav")
            FileOutputStream(cacheFile).use { it.write(audioBytes) }
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Erro na síntese neural Gemini: ${e.message}")
            null
        }
    }

    private fun playAudioFile(file: File) {
        try {
            if (mediaPlayer == null) initMediaPlayer()
            mediaPlayer?.run {
                reset()
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    try {
                        mp.playbackParams = mp.playbackParams.setSpeed(currentRate)
                    } catch (e: Exception) {
                        Log.d(TAG, "Velocidade padrão mantida")
                    }
                    mp.start()
                    onSpeakingStateChanged(true)
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao reproduzir arquivo de áudio: ${e.message}")
            onSpeakingStateChanged(false)
        }
    }

    private fun speakWithSystemTts(text: String) {
        if (!isTtsInitialized || text.isBlank()) {
            onSpeakingStateChanged(false)
            return
        }
        tts?.setSpeechRate(currentRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "CalmPulseUtterance")
    }

    fun stop() {
        speakJob?.cancel()
        speakJob = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
            }
        } catch (e: Exception) {
            // Ignora se não estiver em estado de reprodução
        }
        tts?.stop()
        onSpeakingStateChanged(false)
    }

    fun shutdown() {
        stop()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignora
        }
        tts?.shutdown()
        tts = null
    }
}

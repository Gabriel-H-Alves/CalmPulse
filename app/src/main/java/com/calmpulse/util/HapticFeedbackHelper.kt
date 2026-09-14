package com.calmpulse.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Gerencia micro-vibrações táteis suaves e calmantes para guiar a respiração mesmo com os olhos fechados.
 */
class HapticFeedbackHelper(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Pulso suave ao iniciar a Inspiração (4s)
     */
    fun vibrateInhale() {
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Vibração curta de 100ms com amplitude suave (80 de 255)
            val effect = VibrationEffect.createOneShot(100, 80)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    /**
     * Dois toques sutis (tap-tap) ao iniciar a Retenção (7s)
     */
    fun vibrateHold() {
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Padrão: espera 0ms, vibra 50ms, pausa 70ms, vibra 50ms
            val timings = longArrayOf(0, 50, 70, 50)
            val amplitudes = intArrayOf(0, 70, 0, 70)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 70, 50), -1)
        }
    }

    /**
     * Vibração prolongada e suave na Expiração (8s)
     */
    fun vibrateExhale() {
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Vibração de 200ms com amplitude bem leve (50 de 255)
            val effect = VibrationEffect.createOneShot(200, 50)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
}

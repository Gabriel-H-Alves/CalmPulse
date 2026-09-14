package com.calmpulse.domain

import com.calmpulse.ui.components.BreathingPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para validar as constantes e proporções da técnica 4-7-8.
 */
class BreathingPhaseTest {

    @Test
    fun `breathing cycle phases should follow exact 4-7-8 timing`() {
        assertEquals(4, BreathingPhase.INHALE.durationSeconds)
        assertEquals(7, BreathingPhase.HOLD.durationSeconds)
        assertEquals(8, BreathingPhase.EXHALE.durationSeconds)

        val totalCycleSeconds = BreathingPhase.values().sumOf { it.durationSeconds }
        assertEquals(19, totalCycleSeconds)
    }

    @Test
    fun `target scales should expand on inhale and return to baseline on exhale`() {
        assertTrue(BreathingPhase.INHALE.targetScale > 1.0f)
        assertEquals(BreathingPhase.INHALE.targetScale, BreathingPhase.HOLD.targetScale)
        assertEquals(1.0f, BreathingPhase.EXHALE.targetScale, 0.001f)
    }

    @Test
    fun `phases should have clear and comforting user instructions`() {
        assertTrue(BreathingPhase.INHALE.title.isNotBlank())
        assertTrue(BreathingPhase.HOLD.title.isNotBlank())
        assertTrue(BreathingPhase.EXHALE.title.isNotBlank())
        assertTrue(BreathingPhase.INHALE.subtitle.isNotBlank())
        assertTrue(BreathingPhase.HOLD.subtitle.isNotBlank())
        assertTrue(BreathingPhase.EXHALE.subtitle.isNotBlank())
    }
}

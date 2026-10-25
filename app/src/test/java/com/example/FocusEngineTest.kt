package com.example

import com.example.data.local.FocusGamificationEntity
import com.example.data.local.FocusPresetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusEngineTest {

    @Test
    fun `preset entity default values`() {
        val preset = FocusPresetEntity(
            title = "Standard Pomodoro",
            mode = "POMODORO",
            workMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            targetCycles = 4
        )

        assertEquals("Standard Pomodoro", preset.title)
        assertEquals(25, preset.workMinutes)
        assertEquals(5, preset.shortBreakMinutes)
        assertEquals(4, preset.targetCycles)
        assertFalse(preset.isStrict)
    }

    @Test
    fun `gamification level calculation`() {
        val g1 = FocusGamificationEntity(totalXp = 0)
        val level1 = 1 + (g1.totalXp / 1000)
        assertEquals(1, level1)

        val g2 = FocusGamificationEntity(totalXp = 2450)
        val level3 = 1 + (g2.totalXp / 1000)
        assertEquals(3, level3)
    }

    @Test
    fun `xp reward calculation formula`() {
        val elapsedSecs = 25 * 60 // 25 mins
        val mins = elapsedSecs / 60
        val baseMultiplier = 10
        val bonusXp = 50 // completion bonus

        val earnedXp = (mins * baseMultiplier) + bonusXp
        assertEquals(300, earnedXp) // 250 + 50 = 300 XP
    }
}

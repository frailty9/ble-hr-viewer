package io.github.frailty9.blehr.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {
    @Test
    fun missingOrUnknownPreferenceFollowsSystem() {
        assertEquals(AppTheme.SYSTEM, AppTheme.fromKey(null))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromKey("unknown"))
        AppTheme.entries.forEach { assertEquals(it, AppTheme.fromKey(it.key)) }
    }

    @Test
    fun systemThemeTracksSystemWhileExplicitChoicesOverrideIt() {
        assertFalse(AppTheme.SYSTEM.isDark(false))
        assertTrue(AppTheme.SYSTEM.isDark(true))
        for (systemDark in listOf(false, true)) {
            assertFalse(AppTheme.LIGHT.isDark(systemDark))
            assertTrue(AppTheme.DARK.isDark(systemDark))
        }
    }
}

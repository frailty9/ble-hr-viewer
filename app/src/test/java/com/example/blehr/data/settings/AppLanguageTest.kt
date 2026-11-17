package com.example.blehr.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun emptyOrUnknownLanguageUsesSystemDefault() {
        listOf(null, "", "fr", "invalid").forEach { tag ->
            assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(tag))
        }
    }

    @Test
    fun regionalAndScriptLocalesSelectTheSupportedLanguage() {
        listOf("zh", "zh-CN", "zh-Hans-CN", "zh-Hant-TW").forEach { tag ->
            assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag(tag))
        }
        listOf("en", "en-US", "en-GB").forEach { tag ->
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(tag))
        }
    }

    @Test
    fun savedLanguageTagsRoundTripIncludingFollowSystem() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromTag(language.tag))
        }
        assertEquals("", AppLanguage.SYSTEM.tag)
    }
}

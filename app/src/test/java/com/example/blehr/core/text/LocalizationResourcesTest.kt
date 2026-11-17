package com.example.blehr.core.text

import com.example.blehr.ble.DiagnosticCode
import com.example.blehr.ble.HeartRateDiagnostic
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourcesTest {
    private val resources = File("src/main/res")
    private val placeholders = Regex("%(\\d+)\\$[-\\d.]*([a-zA-Z])")

    private fun entries(directory: String, tag: String): Map<String, Element> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resources, "$directory/strings.xml"))
        val nodes = document.getElementsByTagName(tag)
        return (0 until nodes.length).associate { index ->
            val element = nodes.item(index) as Element
            element.getAttribute("name") to element
        }
    }

    private fun arguments(text: String): Map<Int, Char> = placeholders.findAll(text).associate {
        it.groupValues[1].toInt() to it.groupValues[2].single()
    }

    private fun assertFormattable(text: String, locale: Locale) {
        val arguments = arguments(text)
        val values = (1..(arguments.keys.maxOrNull() ?: 0)).map { index ->
            when (arguments[index]) {
                'd', 'x' -> 123
                's' -> "Sample device"
                else -> error("Unexpected placeholder at index $index: $text")
            }
        }
        val formatted = String.format(locale, text, *values.toTypedArray())
        assertFalse("Unresolved placeholder: $formatted", placeholders.containsMatchIn(formatted))
    }

    @Test
    fun chineseTranslationsCoverEveryTranslatableStringWithMatchingArguments() {
        val defaults = entries("values", "string")
        val chinese = entries("values-zh", "string")
        val translatable = defaults.filterValues { it.getAttribute("translatable") != "false" }
        assertEquals(translatable.keys, chinese.keys)
        translatable.forEach { (key, english) ->
            val translated = chinese.getValue(key).textContent
            assertTrue("Empty translation: $key", translated.isNotBlank())
            assertEquals("Different arguments: $key", arguments(english.textContent), arguments(translated))
            assertFormattable(english.textContent, Locale.ENGLISH)
            assertFormattable(translated, Locale.CHINESE)
        }
        assertFalse("App name must stay the same in both languages", chinese.containsKey("app_name"))
        assertEquals("BLE HR Viewer", defaults.getValue("app_name").textContent)
    }

    @Test
    fun notificationCountsHaveEnglishSingularAndPluralAndChineseFallback() {
        val english = entries("values", "plurals")
        val chinese = entries("values-zh", "plurals")
        assertEquals(english.keys, chinese.keys)
        for (key in english.keys) {
            fun quantities(element: Element): Map<String, String> {
                val items = element.getElementsByTagName("item")
                return (0 until items.length).associate { index ->
                    val item = items.item(index) as Element
                    item.getAttribute("quantity") to item.textContent
                }
            }
            val en = quantities(english.getValue(key))
            val zh = quantities(chinese.getValue(key))
            assertTrue(en.containsKey("one"))
            assertTrue(en.containsKey("other"))
            assertTrue(zh.containsKey("other"))
            (en.values + zh.values).forEach { text ->
                assertEquals(mapOf(1 to 'd'), arguments(text))
                assertFormattable(text, Locale.ENGLISH)
            }
        }
    }

    @Test
    fun everyStructuredParserDiagnosticHasAResourceMappingAndKeepsItsArguments() {
        val mapped = DiagnosticCode.entries.map { code ->
            val arguments = listOf<Any>(123, 2)
            val text = HeartRateDiagnostic(code, arguments).asUiText()
            assertEquals(arguments, text.arguments)
            text.resourceId
        }
        assertEquals(DiagnosticCode.entries.size, mapped.toSet().size)
    }

    @Test
    fun systemAppLanguagePickerListsEnglishAndChinese() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resources, "xml/locales_config.xml"))
        val nodes = document.getElementsByTagName("locale")
        val languages = (0 until nodes.length).map { index ->
            (nodes.item(index) as Element).getAttributeNS("http://schemas.android.com/apk/res/android", "name")
                .ifBlank { (nodes.item(index) as Element).getAttribute("android:name") }
        }
        assertEquals(listOf("en", "zh"), languages)
    }
}

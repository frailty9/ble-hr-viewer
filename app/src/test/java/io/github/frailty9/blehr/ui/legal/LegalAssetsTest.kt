package io.github.frailty9.blehr.ui.legal

import io.github.frailty9.blehr.BuildConfig
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalAssetsTest {
    private val bundledLicenses = File("src/main/assets/legal")
    private val generatedAssets = File(checkNotNull(System.getProperty("blehr.legalAssets")))

    @Test
    fun bundlesBilingualPoliciesAgreementsAndLicenses() {
        val documents = listOf("privacy_zh.txt", "privacy_en.txt", "agreement_zh.txt", "agreement_en.txt")
        val licenses = listOf("notices.txt", "Apache-2.0.txt", "Lucide.txt", "MIT.txt")
        (documents.map(generatedAssets::resolve) + licenses.map(bundledLicenses::resolve)).forEach { file ->
            assertTrue("Missing or empty legal asset: ${file.name}", file.isFile && file.length() > 0)
        }
    }

    @Test
    fun policiesDiscloseContactSensitiveDataAndMedicalDisclaimer() {
        listOf("privacy_zh.txt", "privacy_en.txt").forEach { name ->
            val text = generatedAssets.resolve(name).readText()
            assertTrue("Missing contact in $name", text.contains(LegalDocuments.CONTACT_EMAIL))
            assertTrue("Missing package name in $name", text.contains(BuildConfig.APPLICATION_ID))
        }
        assertTrue(generatedAssets.resolve("privacy_zh.txt").readText().contains("敏感个人信息"))
        assertTrue(generatedAssets.resolve("privacy_en.txt").readText().contains("sensitive personal information"))
        assertTrue(generatedAssets.resolve("privacy_zh.txt").readText().contains("不是医疗器械"))
        assertTrue(generatedAssets.resolve("privacy_en.txt").readText().contains("not a medical device"))
        assertTrue(generatedAssets.resolve("privacy_zh.txt").readText().contains("系统云备份与设备迁移备份"))
        assertTrue(generatedAssets.resolve("privacy_en.txt").readText().contains("device-to-device transfer"))
    }
}

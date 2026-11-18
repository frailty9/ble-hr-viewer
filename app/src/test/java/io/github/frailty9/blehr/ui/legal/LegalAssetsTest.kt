package io.github.frailty9.blehr.ui.legal

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalAssetsTest {
    private val assets = File("src/main/assets/legal")

    @Test
    fun bundlesBilingualPoliciesAgreementsAndLicenses() {
        listOf(
            "privacy_zh.txt",
            "privacy_en.txt",
            "agreement_zh.txt",
            "agreement_en.txt",
            "notices.txt",
            "Apache-2.0.txt",
            "Lucide.txt",
            "MIT.txt",
        ).forEach { name ->
            assertTrue("Missing legal asset: $name", File(assets, name).isFile)
        }
    }

    @Test
    fun policiesDiscloseContactSensitiveDataAndMedicalDisclaimer() {
        listOf("privacy_zh.txt", "privacy_en.txt").forEach { name ->
            val text = File(assets, name).readText()
            assertTrue("Missing contact in $name", text.contains(LegalDocuments.CONTACT_EMAIL))
            assertTrue("Missing package name in $name", text.contains("io.github.frailty9.blehr"))
        }
        assertTrue(File(assets, "privacy_zh.txt").readText().contains("敏感个人信息"))
        assertTrue(File(assets, "privacy_en.txt").readText().contains("sensitive personal information"))
        assertTrue(File(assets, "privacy_zh.txt").readText().contains("不是医疗器械"))
        assertTrue(File(assets, "privacy_en.txt").readText().contains("not a medical device"))
    }
}

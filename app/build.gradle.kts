import com.android.build.api.artifact.SingleArtifact
import java.util.Properties
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

val appFilingNumber = providers.gradleProperty("app.filingNumber").orElse("").get()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val releaseSigningProperties = providers
    .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
    .asText.orNull?.let { contents ->
        Properties().apply { load(contents.removePrefix("\uFEFF").reader()) }
    }

val privacyPolicy = rootProject.layout.projectDirectory.file("PRIVACY.md")
val privacyConsentVersion = providers.fileContents(privacyPolicy).asText.map { text ->
    val declarations = text.lineSequence().map(String::trim)
        .filter { it.startsWith("<!-- consent-version:") }.toList()
    val pattern = Regex("<!-- consent-version: ([1-9][0-9]*) -->")
    val version = declarations.singleOrNull()?.let(pattern::matchEntire)
        ?.groupValues?.get(1)?.toIntOrNull()
    version ?: throw GradleException(
        "PRIVACY.md must contain exactly one <!-- consent-version: N --> with a positive integer version.",
    )
}

android {
    namespace = "io.github.frailty9.blehr"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.frailty9.blehr"
        minSdk = 26
        targetSdk = 35
        versionCode = 22
        versionName = "1.0.0"
        buildConfigField("String", "APP_FILING_NUMBER", "\"$appFilingNumber\"")
        buildConfigField("int", "PRIVACY_CONSENT_VERSION", privacyConsentVersion.get().toString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseSigning = releaseSigningProperties?.let { properties ->
        fun required(name: String, default: String?): String = properties.getProperty(name)
            ?.takeIf { it.isNotBlank() }
            ?: default
            ?: throw GradleException("Missing '$name' in keystore.properties.")

        fun required(name: String): String = required(name, null)

        signingConfigs.create("release") {
            storeFile = rootProject.file(required("storeFile"))
            storePassword = required("storePassword")
            keyAlias = required("keyAlias")
            keyPassword = required("keyPassword", required("storePassword"))
        }
    }

    buildTypes {
        release {
            signingConfig = releaseSigning
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// These two documents use a bilingual title and one section for each locale.
abstract class GenerateLegalAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val documents: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val assets = outputDirectory.dir("legal").get().asFile.apply { mkdirs() }
        for (document in documents.files) {
            val lines = document.readLines(Charsets.UTF_8)
            val title = lines.firstOrNull()?.takeIf { it.startsWith("# ") }
                ?.removePrefix("# ")?.split(" / ").orEmpty()
            val chinese = lines.indexOf("## 中文")
            val english = lines.indexOf("## English")
            require(title.size == 2 && title.none(String::isBlank) && chinese > 0 && english > chinese &&
                lines.count { it == "## 中文" || it == "## English" } == 2
            ) { "${document.name} must have a bilingual title followed by ‘## 中文’ and ‘## English’ sections." }
            val name = document.nameWithoutExtension.lowercase()
            writeAsset(assets.resolve("${name}_zh.txt"), title[0], lines.subList(chinese + 1, english))
            writeAsset(assets.resolve("${name}_en.txt"), title[1], lines.drop(english + 1))
        }
    }

    private fun writeAsset(file: File, title: String, section: List<String>) {
        val body = section.mapNotNull { line ->
            when {
                line == "---" -> null
                line.startsWith("|") -> {
                    val cells = line.trim('|').split('|').map(String::trim)
                    if (cells.all { it.matches(TABLE_RULE) }) null else cells.joinToString(" · ")
                }
                else -> line.replace(HEADING, "")
            }?.replace(LINK, "$1")?.replace(BOLD, "$1")?.replace(INLINE_CODE, "$1")
        }.joinToString("\n").trim()
        require(body.isNotEmpty()) { "Missing legal document text for ${file.name}." }
        file.writeText("$title\n\n$body\n", Charsets.UTF_8)
    }

    private companion object {
        val TABLE_RULE = Regex(":?-{3,}:?")
        val HEADING = Regex("^#{1,6}\\s+")
        val LINK = Regex("\\[([^]]+)]\\([^)]+\\)")
        val BOLD = Regex("\\*\\*([^*]+)\\*\\*")
        val INLINE_CODE = Regex("`([^`]+)`")
    }
}

abstract class LegalAssetTestArguments : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assetDirectory: DirectoryProperty

    override fun asArguments(): Iterable<String> =
        listOf("-Dblehr.legalAssets=${assetDirectory.dir("legal").get().asFile.absolutePath}")
}

val generateLegalAssets = tasks.register<GenerateLegalAssets>("generateLegalAssets") {
    documents.from(privacyPolicy, rootProject.layout.projectDirectory.file("AGREEMENT.md"))
}

val legalAssetTestArguments = objects.newInstance<LegalAssetTestArguments>().apply {
    assetDirectory.set(generateLegalAssets.flatMap { it.outputDirectory })
}
tasks.withType<Test>().configureEach {
    jvmArgumentProviders.add(legalAssetTestArguments)
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(generateLegalAssets, GenerateLegalAssets::outputDirectory)
        val variantTitle = variant.name.replaceFirstChar { it.uppercaseChar() }
        val versionName = variant.outputs.single().versionName.get()
        val buildSuffix = if (variant.buildType == "release") "" else "-${variant.name}"
        val exportApk = tasks.register<Copy>("export${variantTitle}Apk") {
            group = "distribution"
            description = "Export the ${variant.name} APK with the product name and version."
            from(variant.artifacts.get(SingleArtifact.APK)) {
                include("*.apk")
                rename { fileName ->
                    val signingSuffix = if (fileName.endsWith("-unsigned.apk")) "-unsigned" else ""
                    "ble-hr-viewer-$versionName$buildSuffix$signingSuffix.apk"
                }
            }
            into(rootProject.layout.projectDirectory.dir("dist"))
        }
        tasks.matching { it.name == "assemble$variantTitle" }.configureEach {
            finalizedBy(exportApk)
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.9")
    // Compose's transitive Espresso version reflects InputManager.getInstance, removed on API 37.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}

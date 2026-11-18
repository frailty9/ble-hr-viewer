import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

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

android {
    namespace = "com.example.blehr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.blehr"
        minSdk = 26
        targetSdk = 35
        versionCode = 19
        versionName = "0.4.4-SNAPSHOT"
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

androidComponents {
    onVariants { variant ->
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

import com.android.build.api.artifact.SingleArtifact

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.blehr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.blehr"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.4.0-SNAPSHOT"
    }

    buildTypes {
        release {
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
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

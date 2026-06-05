import java.util.Properties
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    // AGP 9 hat Built-in-Kotlin (kein kotlin.android-Plugin); kotlin.compose
    // aktiviert den Compose-Compiler (Version muss zur Built-in-Kotlin-Version passen).
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // KSP statt kapt (kapt ist mit AGP-9-Built-in-Kotlin inkompatibel); Hilt nutzt KSP.
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

val signingProps = Properties().apply {
    val f = rootProject.file("signing.properties")
    if (f.exists()) load(f.inputStream())
}
val hasReleaseSigning = signingProps.getProperty("storeFile") != null

// --- Auto-Versioning --------------------------------------------------------
// Kein manuelles Bumpen mehr. Die Version wird in CI aus Git berechnet und per
// Gradle-Property übergeben (siehe .github/workflows/build-debug.yml):
//   -PlogbotVersionCode = Commit-Anzahl (monoton steigend)
//   -PlogbotVersionName = Datum des letzten Commits (JAHR.MONAT.TAG.STD.MIN.SEK)
//                         + "-alpha" + Kurz-SHA
// Lokale Builds ohne diese Properties nutzen einen Datums-Fallback ("-alpha-dev").
// Bewusst kein Git-Aufruf in Gradle (Configuration-Cache-sicher, keine Prozess-Exec).
val buildVersionCode = (project.findProperty("logbotVersionCode") as String?)?.toIntOrNull()?.coerceAtLeast(5)
    ?: 5
val buildVersionName = (project.findProperty("logbotVersionName") as String?)?.takeIf { it.isNotBlank() }
    ?: (ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss")) + "-alpha-dev")

android {
    namespace = "de.phytech.logbot"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "de.phytech.logbot"
        minSdk = 24
        targetSdk = 36
        versionCode = buildVersionCode
        versionName = buildVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile     = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias      = signingProps.getProperty("keyAlias")
                keyPassword   = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Kotlin-jvmTarget richtet sich bei Built-in-Kotlin automatisch nach targetCompatibility (17).
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Material Components – wird für das XML-App-Theme (Theme.Material3.*) benötigt
    implementation(libs.material)
    implementation(libs.androidx.appcompat)

    // Sicherer Token-Speicher, Biometrie, QR-Scan
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.zxing.android.embedded)

    // DI (Hilt) – Annotation-Processing via KSP (kapt ist mit AGP-9-Built-in-Kotlin inkompatibel)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Netzwerk
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
}

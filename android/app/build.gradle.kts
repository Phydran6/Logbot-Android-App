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
// Die Namen folgen der Release-Kette in .github/workflows/release.yml
// (-Plogbot.versionCode / -Plogbot.versionName). Der alte Name aus dem
// native-rewrite-Zweig wird weiter akzeptiert, damit Aufrufe von dort nicht
// stillschweigend auf den Fallback zurueckfallen.
fun versionProperty(vararg namen: String): String? =
    namen.firstNotNullOfOrNull { project.findProperty(it) as String? }?.takeIf { it.isNotBlank() }

val buildVersionCode = versionProperty("logbot.versionCode", "logbotVersionCode")
    ?.toIntOrNull()?.coerceAtLeast(5) ?: 5
val buildVersionName = versionProperty("logbot.versionName", "logbotVersionName")
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
            // Ohne signing.properties faellt der Build auf die Debug-Signatur
            // zurueck, statt unsigniert zu bleiben: Ein unsigniertes APK laesst
            // sich auf keinem Geraet installieren, das Release waere wertlos.
            // Fuer Play taugt der Rueckfall nicht, und zwischen zwei so
            // gebauten Releases gibt es keinen Update-Pfad - siehe docs/RELEASE.md.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
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

    lint {
        // Lint soll melden, nicht abbrechen. Der Bericht liegt unter
        // app/build/reports/lint-results-debug.html.
        abortOnError = false
        warningsAsErrors = false
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

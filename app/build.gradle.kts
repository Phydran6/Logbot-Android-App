import java.util.Properties

plugins {
    // AGP 9 hat Built-in-Kotlin (kein kotlin.android-Plugin); kotlin.compose
    // aktiviert den Compose-Compiler (Version muss zur Built-in-Kotlin-Version passen).
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val signingProps = Properties().apply {
    val f = rootProject.file("signing.properties")
    if (f.exists()) load(f.inputStream())
}
val hasReleaseSigning = signingProps.getProperty("storeFile") != null

// --- Auto-Versioning aus Git ------------------------------------------------
// Kein manuelles Bumpen mehr: Der Build leitet die Version aus Git ab.
//   versionCode = Anzahl Commits (monoton steigend)
//   versionName = Datum des letzten Commits (Schema JAHR.MONAT.TAG.STD.MIN.SEK)
//                 + "-alpha" (Rewrite ist work-in-progress) + Kurz-SHA
// providers.exec ist Configuration-Cache-kompatibel. Ohne Git greifen Fallbacks.
// Hinweis: CI braucht volle History (actions/checkout fetch-depth: 0).
fun git(vararg args: String): String? = runCatching {
    providers.exec {
        commandLine(*args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { null }
}.getOrNull()

val gitCommitCount = git("git", "rev-list", "--count", "HEAD")?.toIntOrNull()
val gitShortSha = git("git", "rev-parse", "--short", "HEAD")
val gitCommitDate = git("git", "show", "-s", "--format=%cd", "--date=format:%Y.%m.%d.%H.%M.%S", "HEAD")

// Sicherheits-Untergrenze 5, damit nie unter die letzte WebView-Release (4) gefallen wird.
val buildVersionCode = (gitCommitCount ?: 1).coerceAtLeast(5)
val buildVersionName = buildString {
    append(
        gitCommitDate ?: java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Berlin"))
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss"))
    )
    append("-alpha")
    gitShortSha?.let { append("."); append(it) }
}

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

    // Material Components – wird für das XML-App-Theme (Theme.Material3.*) benötigt
    implementation(libs.material)
    implementation(libs.androidx.appcompat)

    // Wird in Phase 2b genutzt (sicherer Token-Speicher, Biometrie, QR-Scan)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.zxing.android.embedded)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
}

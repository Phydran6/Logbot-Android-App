import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val signingProps = Properties().apply {
    val f = rootProject.file("signing.properties")
    if (f.exists()) load(f.inputStream())
}
val hasReleaseSigning = signingProps.getProperty("storeFile") != null

val ciVersionCode = (project.findProperty("logbot.versionCode") as String?)?.toIntOrNull()
val ciVersionName = project.findProperty("logbot.versionName") as String?

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
        // Version kommt aus der CI (-P-Parameter), damit ein Tag genuegt und
        // niemand von Hand hochzaehlt. Die Werte hier sind der Stand fuer
        // lokale Builds; die CI ueberschreibt beide.
        //
        // versionCode zaehlt mit der Lauf-Nummer hoch und startet bei 100 -
        // die letzte von Hand gebaute Release trug die 4, ein Rueckwaertsschritt
        // waere fuer Play ein Ausschlusskriterium.
        versionCode = ciVersionCode ?: 5
        versionName = ciVersionName ?: "1.0.0"

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.security.crypto)
    implementation(libs.zxing.android.embedded)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
/**
 * Datei:        ExampleInstrumentedTest.kt
 * Projekt:      Logbot
 * Paket:        de.phytech.logbot
 * Autor:        Phydran6
 * Version:      1.0 – 05.04.2026.15.44.55
 *
 * Beschreibung:
 * Instrumentierter Test, der auf einem echten Android-Gerät oder Emulator läuft.
 * Ermöglicht Tests, die den App-Kontext (Context) benötigen.
 * Dient als Vorlage für eigene UI- oder Integrationstests.
 */
package de.phytech.logbot

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

// Führt den Test mit dem AndroidJUnit4-Runner auf einem Gerät aus
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * Beispieltest: Prüft, ob der App-Kontext den korrekten Paketnamen hat.
     * Stellt sicher, dass die App korrekt installiert und gestartet wurde.
     */
    @Test
    fun useAppContext() {
        // App-Kontext vom Gerät holen
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // Paketname muss exakt "de.phytech.logbot" sein
        assertEquals("de.phytech.logbot", appContext.packageName)
    }
}

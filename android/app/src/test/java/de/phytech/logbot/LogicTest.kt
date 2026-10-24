/**
 * Tests für die reine Logik ohne Android: Zeitstempel des Servers, Schwellen
 * für Benachrichtigungen und die Prüfung von Einstellungswerten.
 */
package de.phytech.logbot

import de.phytech.logbot.core.notifications.Notifier
import de.phytech.logbot.core.util.Formats
import de.phytech.logbot.data.repository.KNOWN_SETTINGS
import de.phytech.logbot.data.repository.SettingsRepository
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    @Test
    fun serverTimeWithoutZoneIsUtc() {
        // 2026-05-29T22:30:00 UTC = 1780093800000 ms
        assertEquals(1_780_093_800_000L, Formats.parseServerTime("2026-05-29T22:30:00"))
        assertEquals(1_780_093_800_123L, Formats.parseServerTime("2026-05-29T22:30:00.123456"))
    }

    @Test
    fun serverTimeWithZoneIsConverted() {
        assertEquals(1_780_093_800_000L, Formats.parseServerTime("2026-05-29T22:30:00Z"))
        assertEquals(1_780_093_800_000L, Formats.parseServerTime("2026-05-30T00:30:00+02:00"))
    }

    @Test
    fun unreadableTimeIsNull() {
        assertNull(Formats.parseServerTime(null))
        assertNull(Formats.parseServerTime("gestern"))
    }

    @Test
    fun relativeTime() {
        val base = Formats.parseServerTime("2026-05-29T22:30:00")!!
        assertEquals("gerade eben", Formats.relative("2026-05-29T22:30:00", base + 10_000))
        assertEquals("vor 5 min", Formats.relative("2026-05-29T22:30:00", base + 5 * 60_000))
        assertEquals("vor 3 h", Formats.relative("2026-05-29T22:30:00", base + 3 * 3_600_000))
    }

    @Test
    fun severityThreshold() {
        assertTrue(Notifier.atLeast("critical", "error"))
        assertTrue(Notifier.atLeast("err", "error"))
        assertTrue(Notifier.atLeast("EMERGENCY", "critical"))
        assertFalse(Notifier.atLeast("warning", "error"))
        assertFalse(Notifier.atLeast("info", "warning"))
        // Unbekannt zählt wie "info" – lieber still als Fehlalarm.
        assertFalse(Notifier.atLeast("seltsam", "warning"))
    }

    @Test
    fun settingsAreValidatedByKind() {
        val retention = KNOWN_SETTINGS.first { it.key == "log_retention_days" }
        assertEquals(JsonPrimitive(30L), SettingsRepository.parse(retention, " 30 "))
        assertNull(SettingsRepository.parse(retention, "0"))
        assertNull(SettingsRepository.parse(retention, "dreißig"))

        val flag = KNOWN_SETTINGS.first { it.key == "magic_link_enabled" }
        assertEquals(JsonPrimitive(false), SettingsRepository.parse(flag, "false"))
        assertNull(SettingsRepository.parse(flag, "vielleicht"))
    }
}

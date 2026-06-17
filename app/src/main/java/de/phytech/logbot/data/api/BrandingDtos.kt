/**
 * DTOs für Branding (/api/branding/config). Enthält ALLE Felder, damit Speichern
 * (PUT) die nicht bearbeiteten Felder verlustfrei zurückschreibt.
 */
package de.phytech.logbot.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ColorSchemeDto(
    val background: String = "",
    val surface: String = "",
    @SerialName("surface_elevated") val surfaceElevated: String = "",
    val border: String = "",
    @SerialName("text_primary") val textPrimary: String = "",
    @SerialName("text_secondary") val textSecondary: String = "",
    @SerialName("text_muted") val textMuted: String = "",
)

@Serializable
data class BrandingConfigDto(
    @SerialName("company_name") val companyName: String = "LogBot",
    val tagline: String = "",
    @SerialName("footer_text") val footerText: String = "",
    @SerialName("support_email") val supportEmail: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("favicon_path") val faviconPath: String? = null,
    @SerialName("default_theme") val defaultTheme: String = "dark",
    @SerialName("allow_theme_toggle") val allowThemeToggle: Boolean = true,
    @SerialName("primary_color") val primaryColor: String = "#3b82f6",
    @SerialName("secondary_color") val secondaryColor: String = "#8b5cf6",
    @SerialName("accent_color") val accentColor: String = "#10b981",
    @SerialName("success_color") val successColor: String = "#22c55e",
    @SerialName("warning_color") val warningColor: String = "#f59e0b",
    @SerialName("danger_color") val dangerColor: String = "#ef4444",
    @SerialName("dark_scheme") val darkScheme: ColorSchemeDto = ColorSchemeDto(),
    @SerialName("light_scheme") val lightScheme: ColorSchemeDto = ColorSchemeDto(),
    @SerialName("custom_css") val customCss: String = "",
)

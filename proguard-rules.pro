# ─── WebView ──────────────────────────────────────────────────────────────────
# WebView-Klassen nicht entfernen
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}

# ─── ZXing / QR-Code Scanner ──────────────────────────────────────────────────
-keep class com.journeyapps.** { *; }
-keep class com.google.zxing.** { *; }

# ─── Security Crypto (EncryptedSharedPreferences / Tink) ──────────────────────
-keep class androidx.security.crypto.** { *; }
# Tink-Interna die reflection nutzen
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ─── Crash-Reports ────────────────────────────────────────────────────────────
# Zeilennummern in Stack Traces erhalten (wichtig für die Play Console)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

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

# ─── kotlinx.serialization ────────────────────────────────────────────────────
# Der Serialisierungs-Compiler legt je @Serializable-Klasse einen Companion mit
# serializer() an. R8 sieht keinen Aufrufer dafuer und wuerde ihn entfernen -
# zur Laufzeit gaebe es dann "Serializer for class ... not found".
#
# Wichtig, weil der Release-Build minify nutzt: Im frueheren Zweig wurde nur
# Debug gebaut, diese Regeln waren dort nie noetig.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class de.phytech.logbot.**$$serializer { *; }
-keepclassmembers class de.phytech.logbot.** {
    *** Companion;
}
-keepclasseswithmembers class de.phytech.logbot.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── Retrofit ─────────────────────────────────────────────────────────────────
# Die Schnittstellen werden per Reflection ueber ihre generischen Rueckgabetypen
# ausgewertet; ohne Signature-Attribut verliert Retrofit den Typ.
-keepattributes Signature, Exceptions
-keep,allowobfuscation interface de.phytech.logbot.data.api.**

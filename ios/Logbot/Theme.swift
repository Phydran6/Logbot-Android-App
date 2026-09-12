//
//  Theme.swift
//  Logbot
//
//  Farben und Formatierung, die mehrere Ansichten teilen.
//
//  Die Oberflaeche uebernimmt sonst die Systemfarben. Nur der Schweregrad
//  bekommt feste Werte: Er traegt eine Bedeutung und darf nicht mit dem
//  Systemakzent wandern. Die Werte sind dieselben wie auf Android
//  (android/app/src/main/res/values/colors.xml).
//

import SwiftUI

extension Severity {
    var color: Color {
        switch self {
        case .error: return Color(red: 0.83, green: 0.18, blue: 0.18)
        case .warn:  return Color(red: 0.93, green: 0.42, blue: 0.01)
        case .info:  return Color(red: 0.01, green: 0.53, blue: 0.82)
        case .debug: return Color(red: 0.47, green: 0.56, blue: 0.61)
        }
    }
}

enum Palette {
    static let ok = Color(red: 0.18, green: 0.49, blue: 0.20)
    static let warn = Color(red: 0.93, green: 0.42, blue: 0.01)
    static let error = Color(red: 0.83, green: 0.18, blue: 0.18)

    /// Schwellen wie auf Android: ein Logserver darf auslasten, ohne dass die
    /// Ansicht sofort Alarm schlaegt.
    static func load(_ percent: Double) -> Color {
        switch percent {
        case 90...: return error
        case 75...: return warn
        default:    return ok
        }
    }
}

enum Format {

    /// Der Server liefert ISO-8601 in UTC, teils ohne Zonenangabe. Ohne Zone
    /// laesst sich nichts umrechnen - also als UTC lesen, lokal ausgeben.
    private static let incoming: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        f.timeZone = TimeZone(identifier: "UTC")
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    private static func parse(_ raw: String) -> Date? {
        guard raw.count >= 19 else { return nil }
        return incoming.date(from: String(raw.prefix(19)))
    }

    static func time(_ raw: String) -> String { render(raw, "HH:mm:ss") }
    static func full(_ raw: String) -> String { render(raw, "dd.MM.yyyy, HH:mm:ss") }
    static func day(_ raw: String) -> String { render(raw, "EEEE, dd.MM.yyyy") }

    static func dayKey(_ raw: String) -> String {
        raw.count >= 10 ? String(raw.prefix(10)) : raw
    }

    private static func render(_ raw: String, _ pattern: String) -> String {
        guard let date = parse(raw) else { return raw }
        let out = DateFormatter()
        out.dateFormat = pattern
        out.locale = Locale(identifier: "de_DE")
        return out.string(from: date)
    }

    /// `3 T 04:12 h` - Laufzeit in einer Zeile.
    static func uptime(_ seconds: Double) -> String {
        let total = Int(max(seconds, 0))
        let days = total / 86_400
        let hours = (total % 86_400) / 3_600
        let minutes = (total % 3_600) / 60
        return days > 0
            ? String(format: "%d T %02d:%02d h", days, hours, minutes)
            : String(format: "%02d:%02d h", hours, minutes)
    }

    /// Grosse Zahlen lesbar: 8123456 wird zu "8.123.456".
    static func grouped(_ value: Int) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.locale = Locale(identifier: "de_DE")
        return f.string(from: NSNumber(value: value)) ?? String(value)
    }
}

//
//  LogbotApi.swift
//  Logbot
//
//  REST-Zugriff auf den Logbot-Server. Gleiche Endpunkte wie die
//  Android-Fassung, gleiche Einordnung der Fehler.
//
//  Welche Endpunkte Pflicht und welche optional sind: docs/SERVER-API.md
//

import Foundation

// MARK: - Fehler

enum ApiError: LocalizedError {
    case offline(String)
    case unauthorized
    case notImplemented
    case server(Int, String)
    case malformed

    var errorDescription: String? {
        switch self {
        case .offline(let detail):
            return "Server nicht erreichbar: \(detail)"
        case .unauthorized:
            return "Zugang abgelehnt – Token ungültig oder abgelaufen"
        case .notImplemented:
            return "Dieser Serverstand kennt den Endpunkt noch nicht"
        case .server(let code, let detail):
            return detail.isEmpty ? "Server meldet Fehler \(code)" : detail
        case .malformed:
            return "Antwort des Servers war nicht lesbar"
        }
    }
}

// MARK: - Datentypen

struct ServerHealth: Decodable {
    let status: String
    let version: String
    let uptimeSeconds: Double
    let cpuPercent: Double
    let memoryPercent: Double
    let diskPercent: Double
    let databaseConnected: Bool
    let logsTotal: Int
    let logsLast24h: Int
    let agentsTotal: Int
    let agentsOnline: Int

    var healthy: Bool { status.lowercased() == "healthy" }

    enum CodingKeys: String, CodingKey {
        case status, version
        case uptimeSeconds = "uptime_seconds"
        case cpuPercent = "cpu_percent"
        case memoryPercent = "memory_percent"
        case diskPercent = "disk_percent"
        case databaseConnected = "database_connected"
        case logsTotal = "logs_total"
        case logsLast24h = "logs_last_24h"
        case agentsTotal = "agents_total"
        case agentsOnline = "agents_online"
    }
}

struct LogEntry: Decodable, Identifiable, Hashable {
    let id: Int
    let hostname: String?
    let ipAddress: String?
    let timestamp: String
    let level: String?
    let source: String?
    let message: String?

    enum CodingKeys: String, CodingKey {
        case id, hostname, timestamp, level, source, message
        case ipAddress = "ip_address"
    }

    var severity: Severity { Severity.of(level ?? "") }
}

struct LogPage: Decodable {
    let items: [LogEntry]
    let total: Int
    let page: Int
    let pageSize: Int

    var hasMore: Bool { page * pageSize < total }

    enum CodingKeys: String, CodingKey {
        case items, total, page
        case pageSize = "page_size"
    }
}

struct FilterOption: Decodable, Hashable, Identifiable {
    let key: String
    let label: String
    var id: String { key }
}

struct FilterOptions: Decodable {
    let severities: [FilterOption]
    let categories: [FilterOption]

    static let empty = FilterOptions(severities: [], categories: [])
}

struct MailStatus: Decodable {
    let postfixRunning: Bool
    let queueLength: Int
    let deferredLength: Int
    let lastError: String?

    enum CodingKeys: String, CodingKey {
        case postfixRunning = "postfix_running"
        case queueLength = "queue_length"
        case deferredLength = "deferred_length"
        case lastError = "last_error"
    }
}

struct MessageResponse: Decodable {
    let message: String?
}

/// FastAPI meldet Fehler als {"detail": "..."}. Ist `detail` etwas anderes
/// als ein String (etwa eine Liste bei Validierungsfehlern), schlaegt das
/// Dekodieren fehl und der Aufrufer nimmt seinen eigenen Text.
private struct ErrorBody: Decodable {
    let detail: String?
}

/// Schweregrad-Gruppen. Der Server kennt viele Schreibweisen, die Anzeige vier.
enum Severity {
    case error, warn, info, debug

    static func of(_ level: String) -> Severity {
        switch level.lowercased() {
        case "emerg", "emergency", "alert", "crit", "critical", "err", "error", "fatal":
            return .error
        case "warn", "warning", "notice":
            return .warn
        case "debug", "trace":
            return .debug
        default:
            return .info
        }
    }
}

/// Filter der Logansicht. Leere Werte werden weggelassen.
struct LogQuery: Equatable {
    var page = 1
    var pageSize = 50
    var search = ""
    var minSeverity = ""
    var category = ""

    var filtered: Bool { !search.isEmpty || !minSeverity.isEmpty || !category.isEmpty }

    var queryItems: [URLQueryItem] {
        var items = [
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "page_size", value: String(pageSize))
        ]
        if !search.isEmpty { items.append(URLQueryItem(name: "search", value: search)) }
        if !minSeverity.isEmpty { items.append(URLQueryItem(name: "min_severity", value: minSeverity)) }
        if !category.isEmpty { items.append(URLQueryItem(name: "category", value: category)) }
        return items
    }
}

// MARK: - Client

struct LogbotApi {

    let baseURL: String
    let token: String

    init?(baseURL: String?, token: String?) {
        guard let baseURL, let token, !baseURL.isEmpty, !token.isEmpty else { return nil }
        self.baseURL = baseURL
        self.token = token
    }

    func health() async throws -> ServerHealth {
        try await get("/api/health/detailed")
    }

    func filterOptions() async throws -> FilterOptions {
        try await get("/api/logs/filter-options")
    }

    func logs(_ query: LogQuery) async throws -> LogPage {
        try await get("/api/logs", query: query.queryItems)
    }

    /// Optionaler Endpunkt: aeltere Serverstaende antworten mit 404.
    func mailStatus() async throws -> MailStatus {
        try await get("/api/mail/status")
    }

    /// Optionaler Endpunkt: stoesst eine Reset-Mail ueber Postfix an.
    func requestPasswordReset(login: String) async throws -> String {
        let body = try JSONSerialization.data(withJSONObject: ["login": login])
        let response: MessageResponse = try await send("POST", "/api/mail/password-reset", body: body)
        return response.message ?? "Reset-Mail wurde in die Warteschlange gelegt."
    }

    // MARK: Innereien

    private func get<T: Decodable>(_ path: String, query: [URLQueryItem] = []) async throws -> T {
        try await send("GET", path, query: query, body: nil)
    }

    private func send<T: Decodable>(
        _ method: String,
        _ path: String,
        query: [URLQueryItem] = [],
        body: Data? = nil
    ) async throws -> T {
        guard var components = URLComponents(string: baseURL + path) else { throw ApiError.malformed }
        if !query.isEmpty { components.queryItems = query }
        guard let url = components.url else { throw ApiError.malformed }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 20
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.httpBody = body
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw ApiError.offline(error.localizedDescription)
        }

        guard let http = response as? HTTPURLResponse else { throw ApiError.malformed }
        switch http.statusCode {
        case 200...299:
            do {
                return try JSONDecoder().decode(T.self, from: data)
            } catch {
                throw ApiError.malformed
            }
        case 401, 403:
            throw ApiError.unauthorized
        case 404:
            throw ApiError.notImplemented
        default:
            // FastAPI liefert {"detail": "..."}. Alles andere (etwa die
            // HTML-Fehlerseite eines Reverse-Proxy) wuerde nur verwirren.
            let detail = (try? JSONDecoder().decode(ErrorBody.self, from: data))?.detail ?? ""
            throw ApiError.server(http.statusCode, detail)
        }
    }
}

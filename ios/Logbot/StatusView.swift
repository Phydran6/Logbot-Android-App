//
//  StatusView.swift
//  Logbot
//
//  Serverzustand auf einer Seite: erreichbar, Auslastung, Datenbank,
//  Agenten, Logaufkommen. Quelle ist GET /api/health/detailed.
//

import SwiftUI

struct StatusView: View {

    let onDisconnect: () -> Void

    @State private var health: ServerHealth?
    @State private var error: String?
    @State private var unauthorized = false
    @State private var showAbout = false

    private var api: LogbotApi? {
        LogbotApi(baseURL: Credentials.instanceURL, token: Credentials.authToken)
    }

    var body: some View {
        NavigationStack {
            List {
                stateSection

                if let health {
                    Section("Auslastung") {
                        gauge("Prozessor", health.cpuPercent)
                        gauge("Arbeitsspeicher", health.memoryPercent)
                        gauge("Speicherplatz", health.diskPercent)
                    }

                    Section("Zahlen") {
                        row("Datenbank", health.databaseConnected ? "verbunden" : "getrennt")
                        row("Agenten online", "\(health.agentsOnline) von \(health.agentsTotal)")
                        row("Logs (24 h)", Format.grouped(health.logsLast24h))
                        row("Logs gesamt", Format.grouped(health.logsTotal))
                    }
                }
            }
            .navigationTitle("Status")
            .toolbar {
                Menu {
                    Button("Über Logbot") { showAbout = true }
                    Button("Verbindung trennen", role: .destructive, action: onDisconnect)
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
            .refreshable { await load() }
            .task { await load() }
            .alert("Logbot", isPresented: $showAbout) {
                Button("OK", role: .cancel) { }
            } message: {
                Text("Version \(Credentials.appVersion)\n\nVerbunden mit:\n\(Credentials.instanceURL ?? "-")")
            }
        }
    }

    // MARK: Bausteine

    @ViewBuilder
    private var stateSection: some View {
        Section {
            HStack(spacing: 12) {
                Circle()
                    .fill(dotColor)
                    .frame(width: 12, height: 12)

                VStack(alignment: .leading, spacing: 2) {
                    Text(headline).font(.headline)
                    if let health {
                        Text("Version \(health.version) · seit \(Format.uptime(health.uptimeSeconds))")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    } else if let error {
                        Text(error).font(.caption).foregroundStyle(.secondary)
                    }
                    Text(Credentials.instanceURL ?? "-")
                        .font(.system(size: 11, design: .monospaced))
                        .foregroundStyle(.tertiary)
                }
            }
            .padding(.vertical, 4)

            if unauthorized {
                Text("Der Token wird nicht mehr akzeptiert. Unter „Verbindung trennen“ neu einrichten.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private var dotColor: Color {
        guard let health else { return error == nil ? Palette.warn : Palette.error }
        return health.healthy && health.databaseConnected ? Palette.ok : Palette.warn
    }

    private var headline: String {
        guard let health else { return error == nil ? "Server wird geprüft…" : "Server nicht erreichbar" }
        return health.healthy && health.databaseConnected ? "Server läuft" : "Server eingeschränkt"
    }

    private func gauge(_ label: String, _ percent: Double) -> some View {
        let value = min(max(percent, 0), 100)
        return VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label)
                Spacer()
                Text("\(Int(value.rounded())) %")
                    .font(.system(.body, design: .monospaced))
            }
            ProgressView(value: value, total: 100)
                .tint(Palette.load(value))
        }
        .padding(.vertical, 2)
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).foregroundStyle(.secondary)
            Spacer()
            Text(value)
        }
    }

    private func load() async {
        guard let api else { return }
        do {
            health = try await api.health()
            error = nil
            unauthorized = false
        } catch let failure as ApiError {
            health = nil
            error = failure.errorDescription
            if case .unauthorized = failure { unauthorized = true }
        } catch {
            health = nil
            self.error = error.localizedDescription
        }
    }
}

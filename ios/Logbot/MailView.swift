//
//  MailView.swift
//  Logbot
//
//  Zugang zum Mailsystem des Servers (Postfix): Dienstzustand, Warteschlange,
//  Passwort-Reset und die letzten Mail-Logzeilen.
//
//  Zwei Quellen, wie auf Android:
//    - Logzeilen ueber GET /api/logs?category=mail (laeuft mit jedem Stand)
//    - Zustand und Reset ueber /api/mail/* (optional, aeltere Staende: 404)
//
//  Kontrakt: docs/SERVER-API.md
//

import SwiftUI

struct MailView: View {

    @State private var status: MailStatus?
    @State private var notice: String?
    @State private var entries: [LogEntry] = []
    @State private var askReset = false
    @State private var login = ""
    @State private var resultMessage: String?

    private var api: LogbotApi? {
        LogbotApi(baseURL: Credentials.instanceURL, token: Credentials.authToken)
    }

    var body: some View {
        NavigationStack {
            List {
                Section("Postfix") {
                    if let status {
                        HStack {
                            Circle()
                                .fill(status.postfixRunning ? Palette.ok : Palette.error)
                                .frame(width: 10, height: 10)
                            Text(status.postfixRunning ? "Dienst läuft" : "Dienst läuft nicht")
                        }
                        LabeledContent("Warteschlange", value: "\(status.queueLength)")
                        LabeledContent("Zurückgestellt", value: "\(status.deferredLength)")
                        if let lastError = status.lastError, !lastError.isEmpty {
                            Text(lastError).font(.footnote).foregroundStyle(.secondary)
                        }
                    } else if let notice {
                        Text(notice).font(.footnote).foregroundStyle(.secondary)
                    } else {
                        ProgressView()
                    }
                }

                Section("Aktionen") {
                    Button("Passwort zurücksetzen") { askReset = true }
                }

                Section("Letzte Mail-Logzeilen") {
                    if entries.isEmpty {
                        Text("Keine Mail-Logzeilen vorhanden.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(entries) { entry in
                            LogRowView(entry: entry)
                        }
                    }
                }
            }
            .navigationTitle("Mail")
            .refreshable { await load() }
            .task { await load() }
            .alert("Passwort zurücksetzen", isPresented: $askReset) {
                TextField("Benutzername oder E-Mail", text: $login)
                    .textInputAutocapitalization(.never)
                Button("Abbrechen", role: .cancel) { }
                Button("Mail senden") { Task { await sendReset() } }
            } message: {
                Text("Der Server schickt eine Mail mit Rücksetz-Link über Postfix. Ob es das Konto gibt, verrät die Antwort absichtlich nicht.")
            }
            .alert("Logbot", isPresented: Binding(
                get: { resultMessage != nil },
                set: { if !$0 { resultMessage = nil } }
            )) {
                Button("OK", role: .cancel) { resultMessage = nil }
            } message: {
                Text(resultMessage ?? "")
            }
        }
    }

    private func load() async {
        guard let api else { return }

        do {
            status = try await api.mailStatus()
            notice = nil
        } catch let failure as ApiError {
            status = nil
            notice = {
                if case .notImplemented = failure {
                    return "Dieser Serverstand liefert noch keinen Postfix-Zustand. Die Logzeilen unten funktionieren trotzdem."
                }
                return failure.errorDescription
            }()
        } catch {
            status = nil
            notice = error.localizedDescription
        }

        var query = LogQuery()
        query.pageSize = 40
        query.category = "mail"
        entries = (try? await api.logs(query))?.items ?? []
    }

    private func sendReset() async {
        guard let api, !login.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        do {
            resultMessage = try await api.requestPasswordReset(login: login)
        } catch let failure as ApiError {
            if case .notImplemented = failure {
                resultMessage = "Dieser Serverstand kennt den Reset per Mail noch nicht. Ein Administrator kann das Passwort in der Weboberfläche unter „Benutzer“ ändern."
            } else {
                resultMessage = failure.errorDescription
            }
        } catch {
            resultMessage = error.localizedDescription
        }
        login = ""
    }
}

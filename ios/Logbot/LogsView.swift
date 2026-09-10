//
//  LogsView.swift
//  Logbot
//
//  Logansicht: Suche, Schweregrad, Kategorie, Nachladen beim Scrollen.
//
//  Wie auf Android filtert der Server, nicht die App - bei Millionen Zeilen
//  ist alles andere aussichtslos.
//

import SwiftUI
import UIKit

struct LogsView: View {

    @State private var query = LogQuery()
    @State private var entries: [LogEntry] = []
    @State private var total = 0
    @State private var hasMore = false
    @State private var loading = false
    @State private var options = FilterOptions.empty
    @State private var error: String?
    @State private var selected: LogEntry?

    private var api: LogbotApi? {
        LogbotApi(baseURL: Credentials.instanceURL, token: Credentials.authToken)
    }

    var body: some View {
        NavigationStack {
            List {
                if !entries.isEmpty {
                    Section {
                        ForEach(entries) { entry in
                            LogRowView(entry: entry)
                                .onTapGesture { selected = entry }
                                .onAppear { loadMoreIfNeeded(at: entry) }
                        }
                    } header: {
                        Text(query.filtered
                             ? "\(Format.grouped(total)) Treffer"
                             : "\(Format.grouped(total)) Einträge")
                    }
                } else if !loading {
                    Text(error ?? "Keine Einträge für diese Auswahl.")
                        .foregroundStyle(.secondary)
                }
            }
            .listStyle(.plain)
            .navigationTitle("Logs")
            .searchable(text: $query.search, prompt: "In Nachrichten suchen")
            .onSubmit(of: .search) { Task { await reload() } }
            .safeAreaInset(edge: .top) { filterBar }
            .refreshable { await reload() }
            .task {
                options = (try? await api?.filterOptions()) ?? .empty
                await reload()
            }
            .sheet(item: $selected) { entry in
                LogDetailView(entry: entry)
                    .presentationDetents([.medium, .large])
            }
        }
    }

    // MARK: Filter

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                chip("Alle Stufen", key: "", current: query.minSeverity) {
                    query.minSeverity = ""; Task { await reload() }
                }
                ForEach(options.severities) { option in
                    chip(option.label, key: option.key, current: query.minSeverity) {
                        query.minSeverity = option.key; Task { await reload() }
                    }
                }
                Divider().frame(height: 20)
                chip("Alle Typen", key: "", current: query.category) {
                    query.category = ""; Task { await reload() }
                }
                ForEach(options.categories) { option in
                    chip(option.label, key: option.key, current: query.category) {
                        query.category = option.key; Task { await reload() }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .background(.bar)
    }

    private func chip(_ label: String, key: String, current: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.footnote)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(key == current ? Color.accentColor.opacity(0.18) : Color.secondary.opacity(0.12))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: Laden

    private func reload() async {
        query.page = 1
        entries = []
        hasMore = false
        await fetch()
    }

    private func loadMoreIfNeeded(at entry: LogEntry) {
        guard hasMore, !loading, entries.suffix(5).contains(entry) else { return }
        query.page += 1
        Task { await fetch() }
    }

    private func fetch() async {
        guard let api, !loading else { return }
        loading = true
        defer { loading = false }
        do {
            let page = try await api.logs(query)
            // Der Server kann beim Nachladen dieselbe ID erneut liefern, wenn
            // gerade neue Zeilen ankommen. Doppelte fallen hier raus.
            let known = Set(entries.map(\.id))
            entries += page.items.filter { !known.contains($0.id) }
            total = page.total
            hasMore = page.hasMore
            error = nil
        } catch let failure as ApiError {
            error = failure.errorDescription
        } catch {
            self.error = error.localizedDescription
        }
    }
}

struct LogRowView: View {
    let entry: LogEntry

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Rectangle()
                .fill(entry.severity.color)
                .frame(width: 3)
                .cornerRadius(1.5)

            VStack(alignment: .leading, spacing: 3) {
                Text(entry.message ?? "(keine Nachricht)")
                    .font(.callout)
                    .lineLimit(3)
                Text(meta)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                    .lineLimit(1)
            }

            Spacer(minLength: 8)

            Text((entry.level ?? "-").uppercased())
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(entry.severity.color)
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
    }

    private var meta: String {
        [Format.time(entry.timestamp), entry.hostname, entry.source]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: "  ·  ")
    }
}

struct LogDetailView: View {
    let entry: LogEntry

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text((entry.level ?? "-").uppercased())
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(entry.severity.color)
                    Text(entry.message ?? "(keine Nachricht)")
                        .textSelection(.enabled)
                }
                Section {
                    LabeledContent("Zeitpunkt", value: Format.full(entry.timestamp))
                    LabeledContent("Host", value: entry.hostname ?? "-")
                    LabeledContent("Adresse", value: entry.ipAddress ?? "-")
                    LabeledContent("Quelle", value: entry.source ?? "-")
                }
                Section {
                    Button("Nachricht kopieren") {
                        UIPasteboard.general.string = entry.message ?? ""
                    }
                }
            }
            .navigationTitle("Eintrag")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

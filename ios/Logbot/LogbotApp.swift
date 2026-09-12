//
//  LogbotApp.swift
//  Logbot
//
//  Einstiegspunkt und Rahmen: App-Sperre, dann vier Bereiche in einer
//  Tab-Leiste. Aufbau und Reihenfolge sind dieselben wie auf Android -
//  wer beide Geraete benutzt, soll nicht umdenken muessen.
//

import LocalAuthentication
import SwiftUI

@main
struct LogbotApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}

struct RootView: View {

    @State private var configured = Credentials.isConfigured
    @State private var unlocked = !Credentials.appLockEnabled
    @State private var lockFailed = false

    var body: some View {
        Group {
            if !configured {
                SetupView { configured = true; unlocked = true }
            } else if !unlocked {
                LockView(failed: lockFailed) { authenticate() }
            } else {
                MainTabs(onDisconnect: {
                    Credentials.clear()
                    configured = false
                })
            }
        }
        .task {
            if configured && Credentials.appLockEnabled { authenticate() }
        }
    }

    /// Face ID, Touch ID oder Gerätecode. Schlaegt es fehl, bleibt die App
    /// gesperrt - Zugangsdaten werden gar nicht erst gelesen.
    private func authenticate() {
        let context = LAContext()
        context.localizedCancelTitle = "Abbrechen"

        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
            // Kein Code und keine Biometrie eingerichtet: sperren waere eine
            // Sackgasse, aus der man nicht mehr herauskommt.
            unlocked = true
            return
        }

        context.evaluatePolicy(
            .deviceOwnerAuthentication,
            localizedReason: "Logbot entsperren"
        ) { success, _ in
            Task { @MainActor in
                unlocked = success
                lockFailed = !success
            }
        }
    }
}

struct LockView: View {
    let failed: Bool
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "lock.fill")
                .font(.system(size: 40))
                .foregroundStyle(.secondary)
            Text("Logbot ist gesperrt")
                .font(.headline)
            if failed {
                Text("Entsperren wurde abgebrochen.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            Button("Entsperren", action: retry)
                .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

struct MainTabs: View {

    let onDisconnect: () -> Void

    var body: some View {
        TabView {
            StatusView(onDisconnect: onDisconnect)
                .tabItem { Label("Status", systemImage: "speedometer") }

            LogsView()
                .tabItem { Label("Logs", systemImage: "list.bullet") }

            MailView()
                .tabItem { Label("Mail", systemImage: "envelope") }

            WebPane()
                .tabItem { Label("Weboberfläche", systemImage: "globe") }
        }
    }
}

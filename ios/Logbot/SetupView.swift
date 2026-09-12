//
//  SetupView.swift
//  Logbot
//
//  Erste Einrichtung: Instanz-URL und App-Token.
//
//  Den Token erzeugt die Weboberflaeche des Servers unter "App verbinden".
//  Dort steht auch der QR-Code - auf iOS wird sein Inhalt eingefuegt, statt
//  die Kamera zu bemuehen: Er ist kurz, und die Kamera-Berechtigung waere
//  eine Huerde mehr im App-Review.
//

import SwiftUI

struct SetupView: View {

    let onDone: () -> Void

    @State private var url = "https://"
    @State private var token = ""
    @State private var appLock = false
    @State private var error: String?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("https://logbot.example.de", text: $url)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                    SecureField("Auth-Token", text: $token)
                } header: {
                    Text("Instanz")
                } footer: {
                    Text("Beides steht in der Weboberfläche unter „App verbinden“. Der QR-Code dort enthält denselben Token als JSON – Inhalt einfügen geht auch.")
                }

                Section {
                    Toggle("App mit Face ID / Code sperren", isOn: $appLock)
                }

                if let error {
                    Section {
                        Text(error).foregroundStyle(.red)
                    }
                }

                Section {
                    Button("Verbinden") { connect() }
                        .disabled(token.isEmpty || url.count < 9)
                }
            }
            .navigationTitle("Logbot einrichten")
        }
    }

    private func connect() {
        let cleanURL = url.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanToken = token.trimmingCharacters(in: .whitespacesAndNewlines)

        // Nur https: Der Token geht als Bearer-Kopfzeile mit, ueber http waere
        // er im selben Netz mitlesbar.
        guard cleanURL.hasPrefix("https://"), cleanURL.count > 9 else {
            error = "Die URL muss mit https:// beginnen."
            return
        }
        guard !cleanToken.isEmpty else {
            error = "Bitte den Token eintragen."
            return
        }

        // Ein eingefuegter QR-Inhalt ist JSON: {"url":"…","token":"…"}
        if let data = cleanToken.data(using: .utf8),
           let json = try? JSONSerialization.jsonObject(with: data) as? [String: String],
           let pastedURL = json["url"], let pastedToken = json["token"] {
            Credentials.save(url: pastedURL, token: pastedToken, appLock: appLock)
            onDone()
            return
        }

        Credentials.save(url: cleanURL, token: cleanToken, appLock: appLock)
        onDone()
    }
}

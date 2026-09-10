//
//  WebPane.swift
//  Logbot
//
//  Die vollstaendige Weboberflaeche des Servers in einer WKWebView.
//
//  Haertung wie auf Android: Der Token geht als Bearer-Kopfzeile mit,
//  Navigation ausserhalb der eingerichteten Instanz geht in Safari, und bei
//  einer 401 auf dem Hauptrahmen sind die Zugangsdaten hinueber.
//

import SwiftUI
import WebKit

struct WebPane: View {
    var body: some View {
        NavigationStack {
            WebContainer()
                .ignoresSafeArea(edges: .bottom)
                .navigationTitle("Weboberfläche")
                .navigationBarTitleDisplayMode(.inline)
        }
    }
}

struct WebContainer: UIViewRepresentable {

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .default()

        let view = WKWebView(frame: .zero, configuration: config)
        view.navigationDelegate = context.coordinator
        view.allowsBackForwardNavigationGestures = true

        if let request = context.coordinator.request(path: "") {
            view.load(request)
        }
        return view
    }

    func updateUIView(_ uiView: WKWebView, context: Context) { }

    final class Coordinator: NSObject, WKNavigationDelegate {

        private var host: String? {
            guard let url = Credentials.instanceURL else { return nil }
            return URL(string: url)?.host
        }

        func request(path: String) -> URLRequest? {
            guard let base = Credentials.instanceURL,
                  let token = Credentials.authToken,
                  let url = URL(string: base + path) else { return nil }
            var request = URLRequest(url: url)
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            return request
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard let url = navigationAction.request.url, let target = url.host, let host else {
                decisionHandler(.cancel)
                return
            }

            if target == host || target.hasSuffix(".\(host)") {
                decisionHandler(.allow)
            } else {
                // Fremde Adresse: nach draussen, nicht in der App.
                UIApplication.shared.open(url)
                decisionHandler(.cancel)
            }
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationResponse: WKNavigationResponse,
            decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void
        ) {
            if let http = navigationResponse.response as? HTTPURLResponse,
               http.statusCode == 401,
               navigationResponse.isForMainFrame {
                Credentials.clear()
            }
            decisionHandler(.allow)
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            // Die Weboberflaeche liest den Token aus dem localStorage.
            // JSONSerialization maskiert alles, was sonst JavaScript waere.
            guard let token = Credentials.authToken,
                  let data = try? JSONSerialization.data(
                      withJSONObject: [token], options: .fragmentsAllowed
                  ),
                  let array = String(data: data, encoding: .utf8) else { return }

            let quoted = String(array.dropFirst().dropLast())
            webView.evaluateJavaScript(
                "(function(){try{localStorage.setItem('authToken',\(quoted));}catch(e){}})();"
            )
        }
    }
}

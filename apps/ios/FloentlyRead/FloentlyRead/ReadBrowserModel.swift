import Combine
import Foundation
import WebKit

@MainActor
final class ReadBrowserController: ObservableObject {
    @Published var addressText: String = ""
    @Published var currentURL: URL?
    @Published var pageTitle: String = ""
    @Published var canGoBack = false
    @Published var canGoForward = false
    @Published var isLoading = false
    @Published var estimatedProgress: Double = 0
    @Published var readingStatus = "Ready"
    @Published var extractedText: String = ""
    @Published var extractedLanguage: String = "auto"
    @Published var selectionText: String = ""
    @Published var selectionLanguage: String = "auto"
    @Published private(set) var webViewGeneration = 0
    @Published private(set) var lastJavaScriptError: String?

    weak var webView: WKWebView?
    private var pendingURL: URL?
    private var navigationGeneration = 0
    private var extractionGeneration = 0
    private var rendererCrashURL: URL?
    private var rendererCrashCount = 0
    private var lastRendererCrashAt: Date?

    private struct ExtractionRequest {
        let navigationGeneration: Int
        let requestGeneration: Int
        let url: String
    }

    static let protectedAuthenticationHosts: Set<String> = [
        "accounts.google.com",
        "login.microsoftonline.com",
        "login.live.com",
        "appleid.apple.com",
        "www.facebook.com",
        "m.facebook.com"
    ]

    func attach(_ webView: WKWebView) {
        self.webView = webView
        if let pendingURL {
            self.pendingURL = nil
            load(url: pendingURL)
        }
    }

    func open(_ input: String) {
        guard let url = Self.normalizeAddress(input) else {
            readingStatus = "Enter a valid website address or search term."
            return
        }
        load(url: url)
    }

    func open(url: URL) {
        guard let resolved = Self.resolveIncomingURL(url) ?? Self.normalizeAddress(url.absoluteString) else {
            readingStatus = "That link cannot be opened."
            return
        }
        load(url: resolved)
    }

    func load(url: URL) {
        guard ["http", "https"].contains(url.scheme?.lowercased() ?? "") else {
            readingStatus = "Only web pages can be loaded in the Read browser."
            return
        }

        invalidateReadableSource()
        addressText = url.absoluteString
        currentURL = url
        readingStatus = "Loading…"

        guard let webView else {
            pendingURL = url
            return
        }

        webView.load(URLRequest(url: url, cachePolicy: .useProtocolCachePolicy, timeoutInterval: 60))
    }

    func goBack() {
        invalidateReadableSource()
        webView?.goBack()
    }

    func goForward() {
        invalidateReadableSource()
        webView?.goForward()
    }

    func reload() {
        rendererCrashURL = nil
        rendererCrashCount = 0
        lastRendererCrashAt = nil
        hardRestartPreservingPage(status: "Reloading page…")
    }

    func stopLoading() {
        webView?.stopLoading()
        isLoading = false
    }

    func navigationDidStart(from webView: WKWebView) {
        invalidateReadableSource()
        readingStatus = "Loading…"
        refreshNavigationState(from: webView)
    }

    func recoverFromWebContentTermination(url: URL?) {
        let target = url ?? webView?.url ?? currentURL
        let now = Date()

        if
            target != nil,
            target == rendererCrashURL,
            let lastRendererCrashAt,
            now.timeIntervalSince(lastRendererCrashAt) < 30
        {
            rendererCrashCount += 1
        } else {
            rendererCrashURL = target
            rendererCrashCount = 1
        }
        lastRendererCrashAt = now

        if rendererCrashCount >= 3 {
            invalidateReadableSource()
            webView?.stopLoading()
            webView = nil
            pendingURL = target
            isLoading = false
            estimatedProgress = 0
            readingStatus =
                "This page repeatedly stopped the web renderer. "
                + "Tap Reload to start a fresh browser process."
            return
        }

        hardRestartPreservingPage(
            status: "The page renderer restarted. Restoring the page…"
        )
    }

    func markPageHealthy() {
        rendererCrashURL = nil
        rendererCrashCount = 0
        lastRendererCrashAt = nil
    }

    private func hardRestartPreservingPage(status: String) {
        let target = webView?.url ?? currentURL ?? pendingURL
        invalidateReadableSource()
        webView?.stopLoading()
        webView = nil
        pendingURL = target

        if let target {
            currentURL = target
            addressText = target.absoluteString
            isLoading = true
        } else {
            isLoading = false
        }

        estimatedProgress = 0
        readingStatus = status
        webViewGeneration &+= 1
    }

    func refreshNavigationState(from webView: WKWebView) {
        let nextURL = webView.url

        if
            let currentURL,
            let nextURL,
            currentURL.absoluteString != nextURL.absoluteString
        {
            // Covers redirects and same-document/SPA URL changes that may
            // not produce a new provisional navigation callback.
            invalidateReadableSource()
        }

        currentURL = nextURL
        addressText = nextURL?.absoluteString ?? addressText
        pageTitle = webView.title ?? ""
        canGoBack = webView.canGoBack
        canGoForward = webView.canGoForward
        isLoading = webView.isLoading
        estimatedProgress = webView.estimatedProgress
    }

    func readPage(
        onReady: (() -> Void)? = nil
    ) {
        guard let webView, let url = webView.url else { return }
        guard !Self.isProtectedAuthenticationURL(url) else {
            readingStatus = "Finish signing in before using Read on this page."
            return
        }

        extractedText = ""
        extractedLanguage = "auto"
        selectionText = ""
        selectionLanguage = "auto"

        let request = beginExtraction(for: url)
        let script = Self.guardedExtractionJavaScript(
            Self.pageExtractionJavaScript,
            expectedURL: url
        )

        readingStatus = "Finding the main reading area…"
        webView.evaluateJavaScript(script) { [weak self] result, error in
            Task { @MainActor in
                guard let self else { return }
                guard self.isCurrentExtraction(request) else { return }

                if let error {
                    self.lastJavaScriptError = error.localizedDescription
                    self.readingStatus =
                        "Could not read this page: "
                        + error.localizedDescription
                    return
                }

                guard
                    let json = result as? String,
                    let data = json.data(using: .utf8),
                    let payload = try? JSONDecoder().decode(
                        ReadBrowserExtraction.self,
                        from: data
                    ),
                    self.isCurrentExtraction(
                        request,
                        payloadURL: payload.url
                    )
                else {
                    return
                }

                let text = payload.text.trimmingCharacters(
                    in: .whitespacesAndNewlines
                )
                guard !text.isEmpty else {
                    self.readingStatus =
                        "No readable lesson or article text "
                        + "was found on the visible page."
                    return
                }

                self.lastJavaScriptError = nil
                self.extractedText = text
                self.extractedLanguage =
                    payload.language.isEmpty ? "auto" : payload.language
                self.pageTitle =
                    payload.title.isEmpty ? self.pageTitle : payload.title
                self.readingStatus =
                    "Starting Read for \(payload.wordCount) words "
                    + "while the live page stays visible."
                onReady?()
            }
        }
    }

    func readSelection(
        onReady: (() -> Void)? = nil
    ) {
        guard let webView, let url = webView.url else { return }
        guard !Self.isProtectedAuthenticationURL(url) else {
            readingStatus = "Finish signing in before using Read on this page."
            return
        }

        selectionText = ""
        selectionLanguage = "auto"

        let request = beginExtraction(for: url)
        let script = Self.guardedExtractionJavaScript(
            Self.selectionExtractionJavaScript,
            expectedURL: url
        )

        webView.evaluateJavaScript(script) { [weak self] result, error in
            Task { @MainActor in
                guard let self else { return }
                guard self.isCurrentExtraction(request) else { return }

                if let error {
                    self.lastJavaScriptError = error.localizedDescription
                    self.readingStatus =
                        "Could not read the selection: "
                        + error.localizedDescription
                    return
                }

                guard
                    let json = result as? String,
                    let data = json.data(using: .utf8),
                    let payload = try? JSONDecoder().decode(
                        ReadBrowserExtraction.self,
                        from: data
                    ),
                    self.isCurrentExtraction(
                        request,
                        payloadURL: payload.url
                    )
                else {
                    return
                }

                self.lastJavaScriptError = nil
                let text = payload.text.trimmingCharacters(
                    in: .whitespacesAndNewlines
                )
                self.selectionText = text
                self.selectionLanguage =
                    payload.language.isEmpty
                    ? "auto"
                    : payload.language
                if text.isEmpty {
                    self.readingStatus =
                        "Select text on the page first."
                } else {
                    self.readingStatus =
                        "Starting Read for the selected text "
                        + "while the live page stays visible."
                    onReady?()
                }
            }
        }
    }

    func setReadingVisual(
        active: Bool,
        pulse: Bool = false
    ) {
        guard let webView, let pageURL = webView.url else { return }

        let activeValue = active ? "true" : "false"
        let pulseValue = pulse ? "true" : "false"
        let expectedURL = Self.javaScriptLiteral(
            pageURL.absoluteString
        )
        let generation = navigationGeneration

        let script = """
        (() => {
          const expectedUrl = \(expectedURL);
          if (location.href !== expectedUrl) return;

          const active = \(activeValue);
          const pulse = \(pulseValue);
          const root = document.querySelector(
            "[data-floently-read-root='true']"
          );
          if (!root) return;

          const styleId = "floently-read-live-visual-style";
          if (!document.getElementById(styleId)) {
            const style = document.createElement("style");
            style.id = styleId;
            style.textContent = `
              [data-floently-read-root='true'] {
                transition:
                  outline-color 180ms ease,
                  box-shadow 180ms ease;
                outline: 2px solid transparent;
                outline-offset: 4px;
              }
              [data-floently-read-root='true'].floently-read-active {
                outline-color: rgba(75, 195, 255, 0.38);
                box-shadow:
                  0 0 0 5px rgba(75, 195, 255, 0.07),
                  0 0 22px rgba(75, 195, 255, 0.12);
              }
              [data-floently-read-root='true'].floently-read-pulse {
                animation: floentlyReadPulse 720ms ease-out;
              }
              @keyframes floentlyReadPulse {
                0% {
                  outline-color: rgba(135, 225, 255, 0.75);
                  box-shadow:
                    0 0 0 4px rgba(100, 210, 255, 0.15),
                    0 0 32px rgba(100, 210, 255, 0.25);
                }
                100% {
                  outline-color: rgba(75, 195, 255, 0.38);
                  box-shadow:
                    0 0 0 5px rgba(75, 195, 255, 0.07),
                    0 0 22px rgba(75, 195, 255, 0.12);
                }
              }
            `;
            document.head.appendChild(style);
          }

          root.classList.toggle(
            "floently-read-active",
            active
          );

          if (!active) {
            root.classList.remove(
              "floently-read-pulse"
            );
            return;
          }

          if (pulse) {
            root.classList.remove(
              "floently-read-pulse"
            );
            void root.offsetWidth;
            root.classList.add(
              "floently-read-pulse"
            );
            setTimeout(() => {
              root.classList.remove(
                "floently-read-pulse"
              );
            }, 760);
          }
        })();
        """

        webView.evaluateJavaScript(script) { [weak self] _, error in
            Task { @MainActor in
                guard
                    let self,
                    generation == self.navigationGeneration
                else {
                    return
                }

                if let error {
                    self.lastJavaScriptError =
                        error.localizedDescription
                } else {
                    self.lastJavaScriptError = nil
                }
            }
        }
    }

    private func invalidateReadableSource() {
        navigationGeneration &+= 1
        extractionGeneration &+= 1
        extractedText = ""
        extractedLanguage = "auto"
        selectionText = ""
        selectionLanguage = "auto"
        lastJavaScriptError = nil
    }

    private func beginExtraction(
        for url: URL
    ) -> ExtractionRequest {
        extractionGeneration &+= 1
        return ExtractionRequest(
            navigationGeneration: navigationGeneration,
            requestGeneration: extractionGeneration,
            url: url.absoluteString
        )
    }

    private func isCurrentExtraction(
        _ request: ExtractionRequest,
        payloadURL: String? = nil
    ) -> Bool {
        guard
            request.navigationGeneration == navigationGeneration,
            request.requestGeneration == extractionGeneration,
            webView?.url?.absoluteString == request.url
        else {
            return false
        }

        if let payloadURL, !payloadURL.isEmpty {
            return payloadURL == request.url
        }

        return true
    }

    private static func guardedExtractionJavaScript(
        _ body: String,
        expectedURL: URL
    ) -> String {
        let expected = javaScriptLiteral(
            expectedURL.absoluteString
        )

        return """
        (() => {
          const expectedUrl = \(expected);
          if (location.href !== expectedUrl) {
            return JSON.stringify({
              title: "",
              url: location.href,
              language: "",
              text: "",
              wordCount: 0
            });
          }
          return (\(body));
        })();
        """
    }

    private static func javaScriptLiteral(
        _ value: String
    ) -> String {
        guard
            let data = try? JSONEncoder().encode(value),
            let literal = String(data: data, encoding: .utf8)
        else {
            return "\"\""
        }
        return literal
    }

    static func isProtectedAuthenticationURL(_ url: URL) -> Bool {
        guard let host = url.host?.lowercased() else { return false }
        return protectedAuthenticationHosts.contains(host)
            || host.hasSuffix(".okta.com")
            || host.hasSuffix(".auth0.com")
    }

    static func normalizeAddress(_ value: String) -> URL? {
        let raw = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return nil }

        if let url = URL(string: raw), ["http", "https"].contains(url.scheme?.lowercased() ?? "") {
            return url
        }

        if raw.range(of: #"^[A-Za-z0-9.-]+\.[A-Za-z]{2,}([/:?#].*)?$"#, options: .regularExpression) != nil {
            return URL(string: "https://\(raw)")
        }

        var components = URLComponents(string: "https://www.google.com/search")
        components?.queryItems = [URLQueryItem(name: "q", value: raw)]
        return components?.url
    }

    static func resolveIncomingURL(_ incoming: URL) -> URL? {
        if incoming.scheme?.lowercased() == "floentlyread", incoming.host?.lowercased() == "open" {
            return URLComponents(url: incoming, resolvingAgainstBaseURL: false)?
                .queryItems?.first(where: { $0.name == "url" })?.value
                .flatMap(normalizeAddress)
        }

        if ["http", "https"].contains(incoming.scheme?.lowercased() ?? ""),
           incoming.host?.lowercased() == "read.floently.com",
           incoming.path == "/mobile/open" {
            return URLComponents(url: incoming, resolvingAgainstBaseURL: false)?
                .queryItems?.first(where: { $0.name == "url" })?.value
                .flatMap(normalizeAddress)
        }

        return ["http", "https"].contains(incoming.scheme?.lowercased() ?? "") ? incoming : nil
    }

    private static let selectionExtractionJavaScript = #"""
    (() => {
      const normalize = (value) => String(value || "")
        .replace(/\s+/g, " ")
        .trim();
      const selection = window.getSelection();
      const text = normalize(
        selection ? selection.toString() : ""
      );

      document.querySelectorAll(
        "[data-floently-read-root='true']"
      ).forEach((node) => {
        node.removeAttribute("data-floently-read-root");
        node.classList.remove(
          "floently-read-active",
          "floently-read-pulse"
        );
      });

      if (selection && selection.rangeCount > 0) {
        const range = selection.getRangeAt(0);
        const container = range.commonAncestorContainer;
        const element =
          container.nodeType === Node.ELEMENT_NODE
          ? container
          : container.parentElement;
        if (element instanceof HTMLElement) {
          element.setAttribute(
            "data-floently-read-root",
            "true"
          );
        }
      }

      return JSON.stringify({
        title: normalize(document.title),
        url: location.href,
        language: normalize(
          document.documentElement?.lang || ""
        ),
        text,
        wordCount: text ? text.split(/\s+/).length : 0
      });
    })();
    """#

    private static let pageExtractionJavaScript = #"""
    (() => {
      const normalize = (value) => String(value || "").replace(/\s+/g, " ").trim();
      const excludedSelector = [
        "script", "style", "noscript", "template", "nav", "footer", "aside",
        "[role='navigation']", "[role='dialog']", "[role='menu']", "[aria-modal='true']",
        "[aria-hidden='true']", "[hidden]", ".cookie", ".cookies", ".modal", ".drawer",
        ".sidebar", ".side-nav", ".sidenav", ".toolbar", ".menu"
      ].join(",");

      const isVisible = (element) => {
        if (!(element instanceof HTMLElement)) return false;
        const style = getComputedStyle(element);
        if (style.display === "none" || style.visibility === "hidden" || Number(style.opacity) === 0) return false;
        const rect = element.getBoundingClientRect();
        if (rect.width < 80 || rect.height < 24) return false;
        if (rect.bottom < 0 || rect.top > innerHeight * 1.5) return false;
        return true;
      };

      const exposure = (element) => {
        const rect = element.getBoundingClientRect();
        const points = [
          [rect.left + rect.width * 0.5, rect.top + Math.min(rect.height * 0.25, innerHeight * 0.25)],
          [rect.left + rect.width * 0.5, rect.top + Math.min(rect.height * 0.5, innerHeight * 0.5)]
        ];
        let hits = 0;
        for (const [x, y] of points) {
          if (x < 0 || y < 0 || x > innerWidth || y > innerHeight) continue;
          const stack = document.elementsFromPoint(x, y);
          if (stack.some((node) => node === element || element.contains(node))) hits += 1;
        }
        return hits;
      };

      const candidates = Array.from(document.querySelectorAll([
        "main", "article", "[role='main']", "[data-testid*='lesson' i]", "[data-testid*='content' i]",
        "[class*='lesson' i]", "[class*='lecture' i]", "[class*='transcript' i]", "[class*='content' i]",
        "section", "body"
      ].join(",")));

      const scored = candidates
        .filter(isVisible)
        .map((element) => {
          const clone = element.cloneNode(true);
          clone.querySelectorAll(excludedSelector).forEach((node) => node.remove());
          const text = normalize(clone.innerText || clone.textContent || "");
          if (text.length < 80) return null;

          const rect = element.getBoundingClientRect();
          const linkText = Array.from(element.querySelectorAll("a")).reduce((sum, link) => sum + normalize(link.innerText).length, 0);
          const controls = element.querySelectorAll("button,input,select,textarea,[role='button'],[role='menuitem']").length;
          const linkDensity = Math.min(1, linkText / Math.max(text.length, 1));
          const edgeDistance = Math.min(Math.abs(rect.left), Math.abs(innerWidth - rect.right));
          const narrowEdgePenalty = rect.width < innerWidth * 0.42 && edgeDistance < 40 ? 2500 : 0;
          const shellPenalty = element === document.body ? 1800 : 0;
          const fixedPenalty = ["fixed", "sticky"].includes(getComputedStyle(element).position) ? 1400 : 0;
          const semanticBonus = element.matches("main,article,[role='main']") ? 1800 : 0;
          const lessonBonus = /lesson|lecture|transcript|content/i.test(`${element.id} ${element.className}`) ? 900 : 0;
          const exposedBonus = exposure(element) * 800;
          const viewportWidthBonus = rect.width >= innerWidth * 0.45 && rect.width <= innerWidth * 0.98 ? 500 : 0;
          const score = Math.min(text.length, 14000) + semanticBonus + lessonBonus + exposedBonus + viewportWidthBonus
            - linkDensity * 7000 - controls * 55 - narrowEdgePenalty - shellPenalty - fixedPenalty;

          return { element, text, score };
        })
        .filter(Boolean)
        .sort((a, b) => b.score - a.score);

      const best = scored[0];
      const text = best ? best.text : "";

      document.querySelectorAll(
        "[data-floently-read-root='true']"
      ).forEach((node) => {
        node.removeAttribute("data-floently-read-root");
        node.classList.remove(
          "floently-read-active",
          "floently-read-pulse"
        );
      });

      if (best?.element instanceof HTMLElement) {
        best.element.setAttribute(
          "data-floently-read-root",
          "true"
        );
      }

      return JSON.stringify({
        title: normalize(document.title),
        url: location.href,
        language: normalize(
          document.documentElement?.lang || ""
        ),
        text,
        wordCount: text ? text.split(/\s+/).length : 0
      });
    })();
    """#
}

private struct ReadBrowserExtraction: Decodable {
    let title: String
    let url: String
    let language: String
    let text: String
    let wordCount: Int
}

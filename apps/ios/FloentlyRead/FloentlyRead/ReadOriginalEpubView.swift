import SwiftUI
import UIKit
import WebKit
import FloentlyShared

struct ReadOriginalEpubView: View {
    let package: ReadLocalEpubPackage

    @State private var chapterIndex = 0

    private let palette = FloentlyPalette.read

    var body: some View {
        VStack(spacing: 0) {
            ReadEpubWebView(
                package: package,
                chapterIndex:
                    $chapterIndex
            )
            .background(
                FloentlyDesignTokens
                    .Colors
                    .canvas
            )

            HStack(
                spacing:
                    FloentlyDesignTokens
                        .Space
                        .s2
            ) {
                chapterButton(
                    systemName:
                        "chevron.left",
                    label:
                        "Previous EPUB chapter",
                    enabled:
                        chapterIndex > 0
                ) {
                    chapterIndex -= 1
                }

                VStack(
                    spacing:
                        FloentlyDesignTokens
                            .Space
                            .s1
                ) {
                    Text(
                        "Chapter "
                            + String(
                                chapterIndex + 1
                            )
                            + " of "
                            + String(
                                package
                                    .metadata
                                    .spine
                                    .count
                            )
                    )
                    .font(
                        .caption.weight(
                            .semibold
                        )
                    )
                    .foregroundStyle(
                        palette.text
                    )

                    if
                        !package.metadata.title
                            .trimmingCharacters(
                                in:
                                    .whitespacesAndNewlines
                            )
                            .isEmpty
                    {
                        Text(
                            package.metadata.title
                        )
                        .font(.caption2)
                        .foregroundStyle(
                            palette.muted
                        )
                        .lineLimit(1)
                    }
                }
                .frame(
                    maxWidth: .infinity
                )

                chapterButton(
                    systemName:
                        "chevron.right",
                    label:
                        "Next EPUB chapter",
                    enabled:
                        chapterIndex
                            + 1
                        < package
                            .metadata
                            .spine
                            .count
                ) {
                    chapterIndex += 1
                }
            }
            .padding(
                .horizontal,
                FloentlyDesignTokens
                    .Space
                    .s2
            )
            .frame(height: 56)
            .background(
                FloentlyDesignTokens
                    .Colors
                    .surface1
            )
            .overlay(alignment: .top) {
                Rectangle()
                    .fill(
                        FloentlyDesignTokens
                            .Colors
                            .borderSoft
                    )
                    .frame(height: 1)
            }
        }
    }

    private func chapterButton(
        systemName: String,
        label: String,
        enabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(
                    .system(
                        size: 17,
                        weight: .semibold
                    )
                )
                .frame(
                    width:
                        FloentlyDesignTokens
                            .Control
                            .iconTarget,
                    height:
                        FloentlyDesignTokens
                            .Control
                            .iconTarget
                )
        }
        .buttonStyle(.plain)
        .foregroundStyle(
            enabled
            ? palette.text
            : palette.muted.opacity(0.45)
        )
        .disabled(!enabled)
        .accessibilityLabel(label)
    }
}

private struct ReadEpubWebView:
    UIViewRepresentable
{
    let package: ReadLocalEpubPackage
    @Binding var chapterIndex: Int

    func makeCoordinator() -> Coordinator {
        Coordinator(
            package: package,
            onChapter: {
                index in
                if chapterIndex != index {
                    chapterIndex = index
                }
            }
        )
    }

    func makeUIView(
        context: Context
    ) -> WKWebView {
        let configuration =
            WKWebViewConfiguration()
        configuration
            .defaultWebpagePreferences
            .allowsContentJavaScript =
                false
        configuration.websiteDataStore =
            .nonPersistent()

        let webView = WKWebView(
            frame: .zero,
            configuration: configuration
        )
        webView.navigationDelegate =
            context.coordinator
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor =
            .clear
        webView.scrollView
            .contentInsetAdjustmentBehavior =
                .never

        loadChapter(
            webView,
            index: chapterIndex,
            coordinator:
                context.coordinator
        )

        return webView
    }

    func updateUIView(
        _ webView: WKWebView,
        context: Context
    ) {
        let bounded =
            chapterIndex
                .clamped(
                    to:
                        package.metadata
                            .spine.indices
                )

        if bounded != chapterIndex {
            DispatchQueue.main.async {
                chapterIndex = bounded
            }
        }

        loadChapter(
            webView,
            index: bounded,
            coordinator:
                context.coordinator
        )
    }

    private func loadChapter(
        _ webView: WKWebView,
        index: Int,
        coordinator: Coordinator
    ) {
        guard
            let url =
                package.chapterURL(
                    at: index
                )
        else {
            return
        }

        let standardized =
            url.standardizedFileURL
        guard
            coordinator.loadedURL
                != standardized
        else {
            return
        }

        coordinator.loadedURL =
            standardized

        webView.loadFileURL(
            standardized,
            allowingReadAccessTo:
                package.rootDirectory
        )
    }

    final class Coordinator:
        NSObject,
        WKNavigationDelegate
    {
        let rootDirectory: URL
        let spinePaths: [String]
        let onChapter: (Int) -> Void
        var loadedURL: URL?

        init(
            package: ReadLocalEpubPackage,
            onChapter:
                @escaping (Int) -> Void
        ) {
            rootDirectory =
                package.rootDirectory
                    .standardizedFileURL
            spinePaths =
                package.metadata
                    .spine
                    .compactMap {
                        package.chapterURL(
                            at: $0.index
                        )?
                        .standardizedFileURL
                        .path
                    }
            self.onChapter = onChapter
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction:
                WKNavigationAction,
            decisionHandler:
                @escaping (
                    WKNavigationActionPolicy
                ) -> Void
        ) {
            guard
                let url =
                    navigationAction
                        .request
                        .url
            else {
                decisionHandler(.cancel)
                return
            }

            if url.isFileURL {
                let target =
                    url.standardizedFileURL
                let rootPath =
                    rootDirectory.path
                let targetPath =
                    target.path
                let insideRoot =
                    targetPath == rootPath
                    || targetPath.hasPrefix(
                        rootPath + "/"
                    )

                guard insideRoot else {
                    decisionHandler(.cancel)
                    return
                }

                if
                    navigationAction
                        .targetFrame?
                        .isMainFrame == true,
                    let index =
                        spinePaths.firstIndex(
                            of: targetPath
                        )
                {
                    loadedURL = target
                    DispatchQueue.main.async {
                        self.onChapter(index)
                    }
                }

                decisionHandler(.allow)
                return
            }

            if
                navigationAction
                    .navigationType
                    == .linkActivated,
                let scheme =
                    url.scheme?
                        .lowercased(),
                scheme == "http"
                    || scheme == "https"
            {
                UIApplication.shared.open(
                    url
                )
            }

            decisionHandler(.cancel)
        }
    }
}

private extension Int {
    func clamped(
        to range: Range<Int>
    ) -> Int {
        guard
            let first = range.first,
            let last = range.last
        else {
            return 0
        }

        return min(
            max(self, first),
            last
        )
    }
}

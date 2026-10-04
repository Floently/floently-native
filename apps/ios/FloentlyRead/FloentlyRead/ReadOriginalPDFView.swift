import PDFKit
import SwiftUI

struct ReadOriginalPDFView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> PDFView {
        let view = PDFView()
        view.autoScales = true
        view.displayMode = .singlePageContinuous
        view.displayDirection = .vertical
        view.displaysPageBreaks = true
        view.pageShadowsEnabled = true
        view.backgroundColor = .secondarySystemBackground
        view.document = PDFDocument(url: url)
        return view
    }

    func updateUIView(
        _ view: PDFView,
        context: Context
    ) {
        if view.document?.documentURL != url {
            view.document = PDFDocument(url: url)
        }
    }
}

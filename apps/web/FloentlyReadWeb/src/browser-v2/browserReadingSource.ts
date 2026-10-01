import type {
  BrowserReadingAnchor,
  BrowserReadingDocument,
  BrowserReadingSentence,
} from "./browserContracts";

export interface BrowserReadingSpan {
  sentenceId: string;
  order: number;
  anchor: BrowserReadingAnchor;
  scalarStart: number;
  scalarEnd: number;
}

export interface BrowserReadingSource {
  documentId: string;
  revisionId: string;
  title: string;
  language: string;
  canonicalUrl: string | null;
  text: string;
  scalarLength: number;
  spans: BrowserReadingSpan[];
}

export function unicodeScalarLength(value: string): number {
  return Array.from(value).length;
}

function normalizedSentenceText(sentence: BrowserReadingSentence): string {
  return sentence.text.replace(/\r\n/g, "\n").replace(/\r/g, "\n").trim();
}

function stableSentences(
  document: BrowserReadingDocument,
): BrowserReadingSentence[] {
  return document.sentences
    .map((sentence, index) => ({ sentence, index }))
    .filter(({ sentence }) => normalizedSentenceText(sentence).length > 0)
    .sort((left, right) =>
      left.sentence.order === right.sentence.order
        ? left.index - right.index
        : left.sentence.order - right.sentence.order,
    )
    .map(({ sentence }) => sentence);
}

export function buildBrowserReadingSource(
  document: BrowserReadingDocument,
): BrowserReadingSource {
  const documentId = document.documentId.trim();
  const revisionId = document.revision.trim();

  if (!documentId || !revisionId) {
    throw new Error("Browser reading document identity is incomplete.");
  }

  const sentences = stableSentences(document);
  const parts: string[] = [];
  const spans: BrowserReadingSpan[] = [];
  let scalarCursor = 0;

  for (const sentence of sentences) {
    const text = normalizedSentenceText(sentence);
    if (parts.length > 0) {
      parts.push("\n\n");
      scalarCursor += 2;
    }

    const scalarStart = scalarCursor;
    const scalarEnd = scalarStart + unicodeScalarLength(text);
    parts.push(text);
    scalarCursor = scalarEnd;

    spans.push({
      sentenceId: sentence.id,
      order: sentence.order,
      anchor: { ...sentence.anchor },
      scalarStart,
      scalarEnd,
    });
  }

  return {
    documentId,
    revisionId,
    title: document.title.trim() || "Web page",
    language: document.language?.trim() || "en",
    canonicalUrl: document.canonicalUrl?.trim() || null,
    text: parts.join(""),
    scalarLength: scalarCursor,
    spans,
  };
}

export function anchorSpanForScalar(
  source: BrowserReadingSource,
  scalarOffset: number,
): BrowserReadingSpan | null {
  if (source.spans.length === 0) return null;

  const target = Math.min(
    source.scalarLength,
    Math.max(0, Math.round(scalarOffset)),
  );

  for (let index = 0; index < source.spans.length; index += 1) {
    const span = source.spans[index];
    const isLast = index + 1 === source.spans.length;

    if (target < span.scalarEnd || isLast) {
      return span;
    }
  }

  return source.spans[source.spans.length - 1] ?? null;
}

export function scalarStartForAnchor(
  source: BrowserReadingSource,
  anchor: BrowserReadingAnchor,
): number | null {
  const match = source.spans.find(
    (span) =>
      span.anchor.id === anchor.id
      && span.anchor.revision === anchor.revision,
  );

  return match?.scalarStart ?? null;
}

export function sameBrowserReadingIdentity(
  left: BrowserReadingSource,
  right: BrowserReadingSource,
): boolean {
  return (
    left.documentId === right.documentId
    && left.revisionId === right.revisionId
    && left.text === right.text
  );
}

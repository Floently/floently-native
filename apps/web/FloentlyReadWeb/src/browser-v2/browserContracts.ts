export interface BrowserViewportPoint {
  x: number;
  y: number;
  viewportRevision?: number;
}

export interface BrowserReadingAnchor {
  id: string;
  revision: string;
}

export interface BrowserReadingSentence {
  id: string;
  text: string;
  order: number;
  kind?: string;
  wordCount: number;
  anchor: BrowserReadingAnchor;
}

export interface BrowserReadingDocument {
  documentId: string;
  revision: string;
  title: string;
  language: string | null;
  canonicalUrl: string | null;
  sentences: BrowserReadingSentence[];
}

export interface BrowserReadingSelection {
  text: string;
  anchor: BrowserReadingAnchor | null;
}

export interface BrowserReadingAdapter {
  extractDocument(tabId: string): Promise<BrowserReadingDocument>;
  resolvePoint(
    tabId: string,
    point: BrowserViewportPoint,
  ): Promise<BrowserReadingAnchor | null>;
  getSelection(
    tabId: string,
  ): Promise<BrowserReadingSelection | null>;
  highlightSentence(
    tabId: string,
    anchor: BrowserReadingAnchor,
  ): Promise<void>;
  highlightWord(
    tabId: string,
    anchor: BrowserReadingAnchor,
    wordIndex: number,
  ): Promise<void>;
  clearHighlights(tabId: string): Promise<void>;
  scrollToSentence(
    tabId: string,
    anchor: BrowserReadingAnchor,
  ): Promise<void>;
  subscribeRevision(
    tabId: string,
    listener: (revision: string) => void,
  ): () => void;
}

export type BrowserLifecycle =
  | "CLOSED"
  | "STARTING"
  | "READY"
  | "DEGRADED"
  | "RECOVERING"
  | "FAILED"
  | "STOPPING";

export type BrowserConnectivity =
  | "local"
  | "connected"
  | "reconnecting"
  | "offline";

export interface BrowserTabSnapshot {
  id: string;
  url: string;
  displayUrl: string;
  title: string;
  loading: boolean;
  progress: number | null;
  canGoBack: boolean;
  canGoForward: boolean;
  securityState: "secure" | "insecure" | "local" | "unknown" | "error";
  documentRevision: string;
}

export interface BrowserSessionSnapshot {
  lifecycle: BrowserLifecycle;
  connectivity: BrowserConnectivity;
  activeTabId: string | null;
  tabs: BrowserTabSnapshot[];
}

export interface BrowserNavigationResult {
  tab: BrowserTabSnapshot;
}

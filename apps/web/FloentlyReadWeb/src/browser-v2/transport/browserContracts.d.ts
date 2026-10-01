export const BROWSER_V2_CONTRACT_VERSION: "0.1";

export type BrowserLifecycle =
  | "CLOSED" | "STARTING" | "READY" | "DEGRADED" | "RECOVERING"
  | "FAILED" | "STOPPING";

export type TabLifecycle =
  | "CREATING" | "LOADING" | "INTERACTIVE" | "COMPLETE" | "ERROR"
  | "CRASHED" | "RECOVERING" | "CLOSING" | "CLOSED";

export type ReaderState =
  | "IDLE" | "EXTRACTING" | "READY" | "PLAYING" | "PAUSED"
  | "ENDED" | "STALE" | "ERROR";

export type BrowserErrorCode =
  | "AUTH_REQUIRED"
  | "SESSION_GONE"
  | "ENGINE_START_FAILED"
  | "ENGINE_CRASHED"
  | "NETWORK_UNAVAILABLE"
  | "NAVIGATION_FAILED"
  | "PAGE_BLOCKED"
  | "PERMISSION_DENIED"
  | "DOWNLOAD_FAILED"
  | "UPLOAD_FAILED"
  | "READER_EXTRACTION_FAILED"
  | "READER_STALE_DOCUMENT"
  | "MEDIA_UNAVAILABLE"
  | "CLOUD_SIGNALING_FAILED"
  | "CLOUD_MEDIA_FAILED"
  | "CLOUD_RECONNECTING"
  | "UNSUPPORTED_CAPABILITY"
  | "INTERNAL_SAFE_ERROR";

export type ReaderErrorCode =
  | "NO_READABLE_CONTENT"
  | "EXTRACTION_FAILED"
  | "STALE_DOCUMENT"
  | "ANCHOR_GONE"
  | "POINT_OUTSIDE_PAGE"
  | "POINT_NOT_READABLE"
  | "FOLLOW_UNAVAILABLE"
  | "AUDIO_PREP_FAILED"
  | "VOICE_UNAVAILABLE";

export type SecurityState = "secure" | "insecure" | "local" | "unknown" | "error";

export interface BrowserViewport {
  width: number;
  height: number;
  deviceScaleFactor: number;
}

export interface BrowserCapabilities {
  tabs: boolean;
  downloads: boolean;
  uploads: boolean;
  clipboardRead: boolean;
  clipboardWrite: boolean;
  permissions: boolean;
  popups: boolean;
  persistentProfile: boolean;
  privateProfile: boolean;
  pageZoom: boolean;
  find: boolean;
  reader: boolean;
  readFromPoint: boolean;
  wordHighlight: boolean;
  cloudAutomation: boolean;
  mediaAudio: boolean;
}

export interface BrowserProfileSnapshot {
  id: string;
  mode: "persistent" | "private";
  displayName?: string;
}

export interface TabSnapshot {
  id: string;
  lifecycle: TabLifecycle;
  url: string;
  displayUrl: string;
  title: string;
  faviconUrl: string | null;
  loading: boolean;
  progress: number | null;
  canGoBack: boolean;
  canGoForward: boolean;
  securityState: SecurityState;
  crashed: boolean;
  audible: boolean;
  muted: boolean;
  documentRevision: string;
}

export interface BrowserSnapshot {
  backendId: string;
  lifecycle: BrowserLifecycle;
  activeTabId: string | null;
  tabs: TabSnapshot[];
  profile: BrowserProfileSnapshot;
  capabilities: BrowserCapabilities;
  connectivity: "local" | "connected" | "reconnecting" | "offline";
}

export interface NavigationResult {
  tab: TabSnapshot;
}

export interface FindResult {
  query: string;
  activeMatch: number | null;
  totalMatches: number;
}

export interface SelectionResult {
  text: string;
  sentenceIds?: string[];
}

export interface ViewportPoint {
  x: number;
  y: number;
  viewportRevision?: number;
}

export interface ReadingAnchor {
  id: string;
  revision: string;
}

export interface ReadingSentence {
  id: string;
  text: string;
  order: number;
  kind?: string;
  wordCount: number;
  anchor: ReadingAnchor;
}

export interface ReadingDocument {
  documentId: string;
  revision: string;
  title: string;
  language: string | null;
  canonicalUrl: string | null;
  sentences: ReadingSentence[];
}

export interface ReadingSelection {
  text: string;
  anchor: ReadingAnchor | null;
}

export interface ReadingAdapter {
  extractDocument(tabId: string): Promise<ReadingDocument>;
  resolvePoint(tabId: string, point: ViewportPoint): Promise<ReadingAnchor | null>;
  getSelection(tabId: string): Promise<ReadingSelection | null>;
  highlightSentence(tabId: string, anchor: ReadingAnchor): Promise<void>;
  highlightWord(tabId: string, anchor: ReadingAnchor, wordIndex: number): Promise<void>;
  clearHighlights(tabId: string): Promise<void>;
  scrollToSentence(tabId: string, anchor: ReadingAnchor): Promise<void>;
  subscribeRevision(tabId: string, listener: (revision: string) => void): () => void;
}

export type BrowserEventType =
  | "lifecycle.changed"
  | "tab.created"
  | "tab.closed"
  | "tab.activated"
  | "tab.updated"
  | "navigation.started"
  | "navigation.committed"
  | "navigation.completed"
  | "navigation.failed"
  | "popup.requested"
  | "permission.requested"
  | "download.started"
  | "download.progress"
  | "download.completed"
  | "download.failed"
  | "engine.crashed"
  | "engine.recovered"
  | "connectivity.changed"
  | "document.revision";

export interface BrowserEvent<T = unknown> {
  eventId: string;
  backendId: string;
  tabId?: string;
  sequence: number;
  timestamp: string;
  type: BrowserEventType;
  payload: T;
}

export interface BrowserStartInput {
  profileId: string;
  privateMode?: boolean;
  initialTarget?: string;
  viewport?: BrowserViewport;
}

export interface CreateTabInput {
  target?: string;
  activate?: boolean;
}

export interface BrowserBackend {
  capabilities(): BrowserCapabilities;
  start(input: BrowserStartInput): Promise<BrowserSnapshot>;
  stop(): Promise<void>;

  open(target: string, tabId?: string): Promise<NavigationResult>;
  back(tabId: string): Promise<NavigationResult>;
  forward(tabId: string): Promise<NavigationResult>;
  reload(tabId: string): Promise<NavigationResult>;
  stopLoading(tabId: string): Promise<void>;

  createTab(input?: CreateTabInput): Promise<TabSnapshot>;
  closeTab(tabId: string): Promise<void>;
  activateTab(tabId: string): Promise<TabSnapshot>;
  duplicateTab(tabId: string): Promise<TabSnapshot>;
  restoreClosedTab(): Promise<TabSnapshot | null>;

  find(tabId: string, query: string, direction: "next" | "previous"): Promise<FindResult>;
  setZoom(tabId: string, factor: number): Promise<void>;
  getSelection(tabId: string): Promise<SelectionResult | null>;

  reader(): ReadingAdapter;
  subscribe(listener: (event: BrowserEvent) => void): () => void;
}

export const BROWSER_LIFECYCLES: readonly BrowserLifecycle[];
export const TAB_LIFECYCLES: readonly TabLifecycle[];
export const READER_STATES: readonly ReaderState[];
export const BROWSER_ERROR_CODES: readonly BrowserErrorCode[];
export const READER_ERROR_CODES: readonly ReaderErrorCode[];
export const CAPABILITY_KEYS: readonly (keyof BrowserCapabilities)[];

export function normalizeViewport(input: Partial<BrowserViewport> & { width: number; height: number }): BrowserViewport;
export function isSafeTopLevelTarget(value: string): boolean;
export function createCapabilities(partial?: Partial<BrowserCapabilities>): Readonly<BrowserCapabilities>;

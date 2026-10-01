export type ReadThemePreference = "light" | "dark" | "system";
export type ReadResolvedTheme = "light" | "dark";
export type ReadHighlightMode = "none" | "sentence" | "word";
export type ReadReaderFont = "serif" | "sans";
export type ReadReaderTextSize = "compact" | "comfortable" | "large";
export type ReadReaderLineSpacing = "compact" | "comfortable" | "spacious";

export interface ReadPreferencesSnapshot {
  themePreference: ReadThemePreference;
  resolvedTheme: ReadResolvedTheme;
  highlightMode: ReadHighlightMode;
  selectedVoiceId: string | null;
  selectedVoiceLanguage: string | null;
  speed: number;
  readerFont: ReadReaderFont;
  readerTextSize: ReadReaderTextSize;
  readerLineSpacing: ReadReaderLineSpacing;
}

const THEME_STORAGE_KEY = "speechChrome.themeMode";
const HIGHLIGHT_STORAGE_KEY = "speechChrome.readingMode";
const VOICE_STORAGE_KEY = "speechChrome.selectedVoiceId";
const VOICE_LANGUAGE_STORAGE_KEY = "speechChrome.selectedVoiceLanguage";
const SPEED_STORAGE_KEY = "floently.read.playbackSpeed.v1";
const READER_FONT_STORAGE_KEY = "floently.read.readerFont.v1";
const READER_TEXT_SIZE_STORAGE_KEY = "floently.read.readerTextSize.v1";
const READER_LINE_SPACING_STORAGE_KEY = "floently.read.readerLineSpacing.v1";

const SYSTEM_THEME_QUERY = "(prefers-color-scheme: dark)";
const listeners = new Set<() => void>();

function storage(): Storage | null {
  if (
    typeof window === "undefined"
    || typeof window.localStorage === "undefined"
  ) {
    return null;
  }

  return window.localStorage;
}

function readStored(key: string): string | null {
  try {
    return storage()?.getItem(key) ?? null;
  } catch {
    return null;
  }
}

function writeStored(key: string, value: string | null): void {
  try {
    const target = storage();
    if (!target) return;

    if (value === null) {
      target.removeItem(key);
    } else {
      target.setItem(key, value);
    }
  } catch {
    // Preferences remain usable for the current session when storage is blocked.
  }
}

export function normalizeReadThemePreference(
  value: unknown,
): ReadThemePreference {
  return value === "light" || value === "dark" || value === "system"
    ? value
    : "dark";
}

function readThemePreference(): ReadThemePreference {
  return normalizeReadThemePreference(readStored(THEME_STORAGE_KEY));
}

export function normalizeReadHighlightMode(
  value: unknown,
): ReadHighlightMode {
  return value === "none" || value === "sentence" || value === "word"
    ? value
    : "sentence";
}

function readHighlightMode(): ReadHighlightMode {
  return normalizeReadHighlightMode(readStored(HIGHLIGHT_STORAGE_KEY));
}

export function normalizeReadReaderFont(
  value: unknown,
): ReadReaderFont {
  return value === "sans" ? "sans" : "serif";
}

function readReaderFont(): ReadReaderFont {
  return normalizeReadReaderFont(readStored(READER_FONT_STORAGE_KEY));
}

export function normalizeReadReaderTextSize(
  value: unknown,
): ReadReaderTextSize {
  return value === "compact" || value === "large"
    ? value
    : "comfortable";
}

function readReaderTextSize(): ReadReaderTextSize {
  return normalizeReadReaderTextSize(readStored(READER_TEXT_SIZE_STORAGE_KEY));
}

export function normalizeReadReaderLineSpacing(
  value: unknown,
): ReadReaderLineSpacing {
  return value === "compact" || value === "spacious"
    ? value
    : "comfortable";
}

function readReaderLineSpacing(): ReadReaderLineSpacing {
  return normalizeReadReaderLineSpacing(
    readStored(READER_LINE_SPACING_STORAGE_KEY),
  );
}

export function normalizeReadSpeed(value: unknown): number {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric >= 0.5 && numeric <= 3
    ? numeric
    : 1;
}

function readSpeed(): number {
  return normalizeReadSpeed(readStored(SPEED_STORAGE_KEY));
}

function systemPrefersDark(): boolean {
  return Boolean(
    typeof window !== "undefined"
    && typeof window.matchMedia === "function"
    && window.matchMedia(SYSTEM_THEME_QUERY).matches,
  );
}

export function resolveReadTheme(
  preference: ReadThemePreference,
  systemDark: boolean,
): ReadResolvedTheme {
  if (preference === "system") {
    return systemDark ? "dark" : "light";
  }
  return preference;
}

export function normalizeReadLanguage(language: string | null | undefined): string | null {
  const normalized = language?.trim().toLowerCase();
  if (!normalized) return null;
  return normalized.split("-")[0] || null;
}

export function sameReadLanguage(
  left: string | null | undefined,
  right: string | null | undefined,
): boolean {
  const normalizedLeft = normalizeReadLanguage(left);
  const normalizedRight = normalizeReadLanguage(right);
  return Boolean(
    normalizedLeft
    && normalizedRight
    && normalizedLeft === normalizedRight,
  );
}

export function effectiveReadHighlightMode(
  requested: ReadHighlightMode,
  supportsVerifiedWordTiming: boolean,
): "none" | "sentence" | "word" {
  if (requested === "none") return "none";
  if (requested === "word" && supportsVerifiedWordTiming) return "word";
  return "sentence";
}

let state: ReadPreferencesSnapshot = {
  themePreference: readThemePreference(),
  resolvedTheme: resolveReadTheme(readThemePreference(), systemPrefersDark()),
  highlightMode: readHighlightMode(),
  selectedVoiceId: readStored(VOICE_STORAGE_KEY)?.trim() || null,
  selectedVoiceLanguage:
    normalizeReadLanguage(readStored(VOICE_LANGUAGE_STORAGE_KEY)),
  speed: readSpeed(),
  readerFont: readReaderFont(),
  readerTextSize: readReaderTextSize(),
  readerLineSpacing: readReaderLineSpacing(),
};

function applyTheme(theme: ReadResolvedTheme): void {
  if (typeof document === "undefined") return;

  document.documentElement.dataset.readTheme = theme;
  document.documentElement.style.colorScheme = theme;
}

applyTheme(state.resolvedTheme);

function emit(): void {
  for (const listener of listeners) {
    listener();
  }
}

function replaceState(patch: Partial<ReadPreferencesSnapshot>): void {
  const next = { ...state, ...patch };
  if (
    next.themePreference === state.themePreference
    && next.resolvedTheme === state.resolvedTheme
    && next.highlightMode === state.highlightMode
    && next.selectedVoiceId === state.selectedVoiceId
    && next.selectedVoiceLanguage === state.selectedVoiceLanguage
    && next.speed === state.speed
    && next.readerFont === state.readerFont
    && next.readerTextSize === state.readerTextSize
    && next.readerLineSpacing === state.readerLineSpacing
  ) {
    return;
  }

  state = next;
  applyTheme(state.resolvedTheme);
  emit();
}

export const subscribeReadPreferences = (
  listener: () => void,
): (() => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

export const getReadPreferencesSnapshot = (): ReadPreferencesSnapshot => state;

export function setReadThemePreference(
  preference: ReadThemePreference,
): void {
  writeStored(THEME_STORAGE_KEY, preference);
  replaceState({
    themePreference: preference,
    resolvedTheme: resolveReadTheme(preference, systemPrefersDark()),
  });
}

export function setReadHighlightMode(mode: ReadHighlightMode): void {
  writeStored(HIGHLIGHT_STORAGE_KEY, mode);
  replaceState({ highlightMode: mode });
}

export function setReadVoicePreference(
  voiceId: string | null,
  language: string | null,
): void {
  const normalizedVoice = voiceId?.trim() || null;
  const normalizedLanguage = normalizeReadLanguage(language);

  writeStored(VOICE_STORAGE_KEY, normalizedVoice);
  writeStored(VOICE_LANGUAGE_STORAGE_KEY, normalizedLanguage);
  replaceState({
    selectedVoiceId: normalizedVoice,
    selectedVoiceLanguage: normalizedLanguage,
  });
}

export function setReadSpeedPreference(speed: number): void {
  const normalized =
    Number.isFinite(speed)
      ? Math.min(3, Math.max(0.5, speed))
      : state.speed;

  writeStored(SPEED_STORAGE_KEY, String(normalized));
  replaceState({ speed: normalized });
}

export function setReadReaderFont(font: ReadReaderFont): void {
  writeStored(READER_FONT_STORAGE_KEY, font);
  replaceState({ readerFont: font });
}

export function setReadReaderTextSize(size: ReadReaderTextSize): void {
  writeStored(READER_TEXT_SIZE_STORAGE_KEY, size);
  replaceState({ readerTextSize: size });
}

export function setReadReaderLineSpacing(
  spacing: ReadReaderLineSpacing,
): void {
  writeStored(READER_LINE_SPACING_STORAGE_KEY, spacing);
  replaceState({ readerLineSpacing: spacing });
}

let environmentUsers = 0;
let mediaCleanup: (() => void) | null = null;
let storageCleanup: (() => void) | null = null;

function rereadStoredPreferences(): void {
  const themePreference = readThemePreference();
  replaceState({
    themePreference,
    resolvedTheme: resolveReadTheme(themePreference, systemPrefersDark()),
    highlightMode: readHighlightMode(),
    selectedVoiceId: readStored(VOICE_STORAGE_KEY)?.trim() || null,
    selectedVoiceLanguage:
      normalizeReadLanguage(readStored(VOICE_LANGUAGE_STORAGE_KEY)),
    speed: readSpeed(),
    readerFont: readReaderFont(),
    readerTextSize: readReaderTextSize(),
    readerLineSpacing: readReaderLineSpacing(),
  });
}

export function startReadPreferencesEnvironment(): () => void {
  environmentUsers += 1;
  applyTheme(state.resolvedTheme);

  if (environmentUsers === 1 && typeof window !== "undefined") {
    if (typeof window.matchMedia === "function") {
      const media = window.matchMedia(SYSTEM_THEME_QUERY);
      const onThemeChange = () => {
        if (state.themePreference !== "system") return;
        replaceState({
          resolvedTheme: resolveReadTheme("system", media.matches),
        });
      };

      if (typeof media.addEventListener === "function") {
        media.addEventListener("change", onThemeChange);
        mediaCleanup = () => media.removeEventListener("change", onThemeChange);
      } else {
        media.addListener(onThemeChange);
        mediaCleanup = () => media.removeListener(onThemeChange);
      }
    }

    const onStorage = (event: StorageEvent) => {
      if (
        event.storageArea === window.localStorage
        && [
          THEME_STORAGE_KEY,
          HIGHLIGHT_STORAGE_KEY,
          VOICE_STORAGE_KEY,
          VOICE_LANGUAGE_STORAGE_KEY,
          SPEED_STORAGE_KEY,
          READER_FONT_STORAGE_KEY,
          READER_TEXT_SIZE_STORAGE_KEY,
          READER_LINE_SPACING_STORAGE_KEY,
        ].includes(event.key ?? "")
      ) {
        rereadStoredPreferences();
      }
    };

    window.addEventListener("storage", onStorage);
    storageCleanup = () => window.removeEventListener("storage", onStorage);
  }

  return () => {
    environmentUsers = Math.max(0, environmentUsers - 1);
    if (environmentUsers > 0) return;

    mediaCleanup?.();
    storageCleanup?.();
    mediaCleanup = null;
    storageCleanup = null;
  };
}

export const readPreferenceStorageKeys = {
  theme: THEME_STORAGE_KEY,
  highlightMode: HIGHLIGHT_STORAGE_KEY,
  voiceId: VOICE_STORAGE_KEY,
  voiceLanguage: VOICE_LANGUAGE_STORAGE_KEY,
  speed: SPEED_STORAGE_KEY,
  readerFont: READER_FONT_STORAGE_KEY,
  readerTextSize: READER_TEXT_SIZE_STORAGE_KEY,
  readerLineSpacing: READER_LINE_SPACING_STORAGE_KEY,
} as const;

import { useSyncExternalStore } from "react";

export type ReadLocale = "en" | "fi";

const READ_LOCALE_STORAGE_KEY = "floently.read.locale.v1";

const EN_MESSAGES = {
  "shell.skipToContent": "Skip to content",
  "shell.primaryNavigation": "Read",
  "shell.settingsNavigation": "Settings",
  "shell.library": "Library",
  "shell.import": "Import",
  "shell.reader": "Reader",
  "shell.browser": "Browser",
  "shell.preferences": "Preferences",
  "shell.plan": "Plan",
  "shell.account": "Account",
  "preferences.uiLanguage": "Interface language",
  "preferences.uiLanguageDescription":
    "Choose the language for Read's interface. Document language and voice language remain separate.",
} as const;

type ReadMessageKey = keyof typeof EN_MESSAGES;

const FI_MESSAGES: Record<ReadMessageKey, string> = {
  "shell.skipToContent": "Siirry sisältöön",
  "shell.primaryNavigation": "Lukeminen",
  "shell.settingsNavigation": "Asetukset",
  "shell.library": "Kirjasto",
  "shell.import": "Tuo",
  "shell.reader": "Lukija",
  "shell.browser": "Selain",
  "shell.preferences": "Asetukset",
  "shell.plan": "Tilaus",
  "shell.account": "Tili",
  "preferences.uiLanguage": "Käyttöliittymän kieli",
  "preferences.uiLanguageDescription":
    "Valitse Readin käyttöliittymän kieli. Asiakirjan kieli ja äänen kieli pysyvät erillisinä.",
};

const MESSAGES: Record<ReadLocale, Record<ReadMessageKey, string>> = {
  en: EN_MESSAGES,
  fi: FI_MESSAGES,
};

const listeners = new Set<() => void>();
let currentLocale: ReadLocale | null = null;

function browserLocale(): string {
  if (typeof navigator === "undefined") return "en";
  return navigator.languages?.[0] || navigator.language || "en";
}

function storedLocale(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(READ_LOCALE_STORAGE_KEY);
  } catch {
    return null;
  }
}

function applyDocumentLanguage(locale: ReadLocale): void {
  if (typeof document === "undefined") return;
  document.documentElement.lang = locale;
}

function emit(): void {
  for (const listener of listeners) listener();
}

export function normalizeReadLocale(value: string | null | undefined): ReadLocale {
  const normalized = String(value || "")
    .trim()
    .toLowerCase()
    .replace("_", "-");

  if (normalized === "fi" || normalized.startsWith("fi-")) return "fi";
  return "en";
}

export function getReadLocale(): ReadLocale {
  if (currentLocale) return currentLocale;
  currentLocale = normalizeReadLocale(storedLocale() || browserLocale());
  return currentLocale;
}

export function setReadLocale(value: string): void {
  const next = normalizeReadLocale(value);
  const changed = currentLocale !== next;
  currentLocale = next;

  if (typeof window !== "undefined") {
    try {
      window.localStorage.setItem(READ_LOCALE_STORAGE_KEY, next);
    } catch {
      // Storage can be unavailable in privacy-restricted browser contexts.
    }
  }

  applyDocumentLanguage(next);
  if (changed) emit();
}

export function subscribeReadLocale(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useReadLocale(): ReadLocale {
  return useSyncExternalStore(
    subscribeReadLocale,
    getReadLocale,
    () => "en",
  );
}

export function readMessage(
  locale: ReadLocale,
  key: ReadMessageKey,
): string {
  return MESSAGES[locale]?.[key] ?? EN_MESSAGES[key];
}

export function formatDocumentCount(
  locale: ReadLocale,
  count: number,
): string {
  const safeCount = Math.max(0, Math.trunc(count));
  const category = new Intl.PluralRules(locale).select(safeCount);

  if (locale === "fi") {
    return category === "one"
      ? `${safeCount} asiakirja`
      : `${safeCount} asiakirjaa`;
  }

  return category === "one"
    ? `${safeCount} document`
    : `${safeCount} documents`;
}

export function startReadLocaleEnvironment(): () => void {
  const locale = getReadLocale();
  applyDocumentLanguage(locale);

  if (typeof window === "undefined") return () => undefined;

  const onStorage = (event: StorageEvent) => {
    if (event.key !== READ_LOCALE_STORAGE_KEY) return;
    const next = normalizeReadLocale(event.newValue || browserLocale());
    if (next === currentLocale) return;
    currentLocale = next;
    applyDocumentLanguage(next);
    emit();
  };

  window.addEventListener("storage", onStorage);
  return () => window.removeEventListener("storage", onStorage);
}

export type { ReadMessageKey };

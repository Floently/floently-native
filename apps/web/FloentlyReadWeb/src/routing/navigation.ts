import { useSyncExternalStore } from "react";

const NAVIGATION_EVENT = "floently-read:navigate";

function locationSnapshot(): string {
  if (typeof window === "undefined") return "/";
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}

function subscribe(listener: () => void): () => void {
  if (typeof window === "undefined") return () => undefined;

  window.addEventListener("popstate", listener);
  window.addEventListener(NAVIGATION_EVENT, listener);

  return () => {
    window.removeEventListener("popstate", listener);
    window.removeEventListener(NAVIGATION_EVENT, listener);
  };
}

export interface BrowserLocation {
  pathname: string;
  search: string;
  hash: string;
  href: string;
}

export function useBrowserLocation(): BrowserLocation {
  const href = useSyncExternalStore(
    subscribe,
    locationSnapshot,
    () => "/",
  );

  const parsed = new URL(
    href,
    typeof window === "undefined"
      ? "https://read.floently.com"
      : window.location.origin,
  );

  return {
    pathname: parsed.pathname,
    search: parsed.search,
    hash: parsed.hash,
    href: `${parsed.pathname}${parsed.search}${parsed.hash}`,
  };
}

export function navigateTo(target: string, replace = false): void {
  if (typeof window === "undefined") return;

  const current = locationSnapshot();
  if (current === target) return;

  if (replace) {
    window.history.replaceState({}, "", target);
  } else {
    window.history.pushState({}, "", target);
  }

  window.dispatchEvent(new Event(NAVIGATION_EVENT));
}

export function safeReturnTo(
  candidate: string | null | undefined,
  origin =
    typeof window === "undefined"
      ? "https://read.floently.com"
      : window.location.origin,
): string | null {
  if (!candidate || !candidate.startsWith("/") || candidate.startsWith("//")) {
    return null;
  }

  try {
    const url = new URL(candidate, origin);

    if (
      url.origin !== origin
      || (url.pathname !== "/app" && !url.pathname.startsWith("/app/"))
    ) {
      return null;
    }

    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return null;
  }
}

export function loginPathForReturnTo(returnTo: string): string {
  return `/login?returnTo=${encodeURIComponent(returnTo)}`;
}

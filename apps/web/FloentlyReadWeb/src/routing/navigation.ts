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

export function canonicalReadPathname(pathname: string): string {
  if (!pathname || pathname === "/") return "/";

  const canonical = pathname.replace(/\/+$/, "");
  return canonical || "/";
}

export function canonicalizeCurrentReadLocation(): void {
  if (typeof window === "undefined") return;

  const canonicalPathname = canonicalReadPathname(
    window.location.pathname,
  );
  if (canonicalPathname === window.location.pathname) return;

  window.history.replaceState(
    window.history.state,
    "",
    `${canonicalPathname}${window.location.search}${window.location.hash}`,
  );
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

    const pathname = canonicalReadPathname(url.pathname);

    if (
      url.origin !== origin
      || (pathname !== "/app" && !pathname.startsWith("/app/"))
    ) {
      return null;
    }

    return `${pathname}${url.search}${url.hash}`;
  } catch {
    return null;
  }
}

export function loginPathForReturnTo(returnTo: string): string {
  return `/login?returnTo=${encodeURIComponent(returnTo)}`;
}

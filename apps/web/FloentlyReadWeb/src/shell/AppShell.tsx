import { useEffect, useRef, type ReactNode } from "react";
import type { ReadAuthUser } from "../auth/authStore";
import {
  readMessage,
  useReadLocale,
  type ReadMessageKey,
} from "../i18n/readLocale";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import { PlaybackDock } from "../reader/PlaybackDock";
import {
  PlaybackDiagnostics,
  resolvePlaybackDiagnosticsEnabled,
} from "../playback/PlaybackDiagnostics";
import { navigateTo } from "../routing/navigation";

interface NavItem {
  href: string;
  labelKey: ReadMessageKey;
  glyph: string;
}

const PRIMARY_NAV: NavItem[] = [
  { href: "/app/library", labelKey: "shell.library", glyph: "L" },
  { href: "/app/import", labelKey: "shell.import", glyph: "+" },
  { href: "/app/reader", labelKey: "shell.reader", glyph: "R" },
  { href: "/app/browser", labelKey: "shell.browser", glyph: "B" },
];

const SECONDARY_NAV: NavItem[] = [
  { href: "/app/preferences", labelKey: "shell.preferences", glyph: "P" },
  { href: "/app/subscription", labelKey: "shell.plan", glyph: "$" },
  { href: "/app/account", labelKey: "shell.account", glyph: "A" },
];

function isActive(pathname: string, href: string): boolean {
  return pathname === href || pathname.startsWith(`${href}/`);
}

function NavLink({
  item,
  pathname,
  label,
}: {
  item: NavItem;
  pathname: string;
  label: string;
}) {
  const active = isActive(pathname, item.href);

  return (
    <a
      href={item.href}
      className={active ? "app-nav-link active" : "app-nav-link"}
      aria-current={active ? "page" : undefined}
      onClick={(event) => {
        event.preventDefault();
        navigateTo(item.href);
      }}
    >
      <span aria-hidden="true">{item.glyph}</span>
      {label}
    </a>
  );
}

export function AppShell({
  pathname,
  user,
  children,
}: {
  pathname: string;
  user: ReadAuthUser;
  children: ReactNode;
}) {
  const runtime = useReadRuntime();
  const locale = useReadLocale();
  const playback = useWebPlaybackSnapshot(runtime?.playback);
  const accountLabel = user.name || user.email;
  const accountInitial = accountLabel.slice(0, 1).toUpperCase();
  const previousPathnameRef = useRef(pathname);
  const diagnosticsEnabled =
    resolvePlaybackDiagnosticsEnabled();

  useEffect(() => {
    if (previousPathnameRef.current === pathname) return;
    previousPathnameRef.current = pathname;

    const frame = window.requestAnimationFrame(() => {
      document.getElementById("read-main-content")?.focus({
        preventScroll: true,
      });
    });

    return () => window.cancelAnimationFrame(frame);
  }, [pathname]);

  return (
    <div className="app-shell">
      <a className="skip-link" href="#read-main-content">
        {readMessage(locale, "shell.skipToContent")}
      </a>

      <aside className="app-sidebar">
        <a
          className="app-brand"
          href="/app/library"
          onClick={(event) => {
            event.preventDefault();
            navigateTo("/app/library");
          }}
        >
          <span className="brand-mark" aria-hidden="true">F</span>
          <span>
            <strong>Floently</strong>
            <small>Read</small>
          </span>
        </a>

        <nav
          className="app-nav"
          aria-label={readMessage(locale, "shell.primaryNavigation")}
        >
          {PRIMARY_NAV.map((item) => (
            <NavLink
              item={item}
              pathname={pathname}
              label={readMessage(locale, item.labelKey)}
              key={item.href}
            />
          ))}
        </nav>

        <nav
          className="app-nav app-nav-secondary"
          aria-label={readMessage(locale, "shell.settingsNavigation")}
        >
          {SECONDARY_NAV.map((item) => (
            <NavLink
              item={item}
              pathname={pathname}
              label={readMessage(locale, item.labelKey)}
              key={item.href}
            />
          ))}
        </nav>

        <a
          href="/app/account"
          className="app-account-chip"
          onClick={(event) => {
            event.preventDefault();
            navigateTo("/app/account");
          }}
        >
          <span className="account-avatar" aria-hidden="true">
            {accountInitial}
          </span>
          <span>
            <strong>{accountLabel}</strong>
            <small>{user.readPolicy?.plan || user.readPlan || user.plan}</small>
          </span>
        </a>
      </aside>

      <div className="app-main">
        <header className="app-mobile-header">
          <button
            type="button"
            className="mobile-brand"
            onClick={() => navigateTo("/app/library")}
          >
            <span className="brand-mark" aria-hidden="true">F</span>
            Floently Read
          </button>
          <button
            type="button"
            className="mobile-import"
            onClick={() => navigateTo("/app/import")}
          >
            {readMessage(locale, "shell.import")}
          </button>
        </header>

        <main
          id="read-main-content"
          className="app-content"
          tabIndex={-1}
        >
          {children}
        </main>

        {runtime && playback.documentId ? (
          <PlaybackDock
            session={runtime.playback}
            snapshot={playback}
          />
        ) : null}

        {runtime && diagnosticsEnabled ? (
          <PlaybackDiagnostics
            session={runtime.playback}
            snapshot={playback}
          />
        ) : null}
      </div>
    </div>
  );
}

import type { ReactNode } from "react";
import type { ReadAuthUser } from "../auth/authStore";
import { useReadRuntime } from "../runtime/ReadRuntimeContext";
import { useWebPlaybackSnapshot } from "../playback/useWebPlaybackSnapshot";
import { PlaybackDock } from "../reader/PlaybackDock";
import { navigateTo } from "../routing/navigation";

interface NavItem {
  href: string;
  label: string;
  glyph: string;
}

const PRIMARY_NAV: NavItem[] = [
  { href: "/app/library", label: "Library", glyph: "L" },
  { href: "/app/import", label: "Import", glyph: "+" },
  { href: "/app/reader", label: "Reader", glyph: "R" },
  { href: "/app/browser", label: "Browser", glyph: "B" },
];

const SECONDARY_NAV: NavItem[] = [
  { href: "/app/preferences", label: "Preferences", glyph: "P" },
  { href: "/app/subscription", label: "Plan", glyph: "$" },
  { href: "/app/account", label: "Account", glyph: "A" },
];

function isActive(pathname: string, href: string): boolean {
  return pathname === href || pathname.startsWith(`${href}/`);
}

function NavLink({
  item,
  pathname,
}: {
  item: NavItem;
  pathname: string;
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
      {item.label}
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
  const playback = useWebPlaybackSnapshot(runtime?.playback);
  const accountLabel = user.name || user.email;
  const accountInitial = accountLabel.slice(0, 1).toUpperCase();

  return (
    <div className="app-shell">
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

        <nav className="app-nav" aria-label="Read">
          {PRIMARY_NAV.map((item) => (
            <NavLink item={item} pathname={pathname} key={item.href} />
          ))}
        </nav>

        <nav className="app-nav app-nav-secondary" aria-label="Settings">
          {SECONDARY_NAV.map((item) => (
            <NavLink item={item} pathname={pathname} key={item.href} />
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
            <small>{user.readPlan || user.plan}</small>
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
            Import
          </button>
        </header>

        <div className="app-content">{children}</div>

        {runtime && playback.documentId ? (
          <PlaybackDock
            session={runtime.playback}
            snapshot={playback}
          />
        ) : null}
      </div>
    </div>
  );
}

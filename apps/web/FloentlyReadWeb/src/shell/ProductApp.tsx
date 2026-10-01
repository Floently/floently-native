import type { ReadAuthUser } from "../auth/authStore";
import { AccountPage } from "../account/AccountPage";
import { BrowserPage } from "../browser/BrowserPage";
import { ImportPage } from "../import/ImportPage";
import { LibraryPage } from "../library/LibraryPage";
import { PreferencesPage } from "../preferences/PreferencesPage";
import { ReaderPage } from "../reader/ReaderPage";
import { ReadRuntimeProvider } from "../runtime/ReadRuntimeContext";
import { navigateTo } from "../routing/navigation";
import { AppShell } from "./AppShell";

function decodeDocumentId(pathname: string): string | null {
  const prefix = "/app/reader/";
  if (!pathname.startsWith(prefix)) return null;

  const raw = pathname.slice(prefix.length).split("/")[0];
  if (!raw) return null;

  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
}

function RouteContent({ pathname }: { pathname: string }) {
  if (pathname === "/app" || pathname === "/app/" || pathname === "/app/library") {
    return <LibraryPage />;
  }

  if (pathname === "/app/import") {
    return <ImportPage />;
  }

  if (pathname === "/app/reader") {
    return <ReaderPage documentId={null} />;
  }

  if (pathname.startsWith("/app/reader/")) {
    return <ReaderPage documentId={decodeDocumentId(pathname)} />;
  }

  if (pathname === "/app/browser") {
    return <BrowserPage />;
  }

  if (pathname === "/app/preferences") {
    return <PreferencesPage />;
  }

  if (pathname === "/app/account") {
    return <AccountPage />;
  }

  return (
    <section className="product-page">
      <div className="reader-empty-state">
        <span aria-hidden="true">404</span>
        <h1>This Read page does not exist.</h1>
        <button
          type="button"
          className="page-primary-action"
          onClick={() => navigateTo("/app/library", true)}
        >
          Open library
        </button>
      </div>
    </section>
  );
}

export function ProductApp({
  pathname,
  user,
}: {
  pathname: string;
  user: ReadAuthUser;
}) {
  return (
    <ReadRuntimeProvider>
      <AppShell pathname={pathname} user={user}>
        <RouteContent pathname={pathname} />
      </AppShell>
    </ReadRuntimeProvider>
  );
}

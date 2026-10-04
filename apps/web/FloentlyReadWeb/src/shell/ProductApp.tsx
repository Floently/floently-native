import type { ReadAuthUser } from "../auth/authStore";
import { AccountPage } from "../account/AccountPage";
import { SubscriptionPage } from "../billing/SubscriptionPage";
import { ImportPage } from "../import/ImportPage";
import { LibraryPage } from "../library/LibraryPage";
import { PreferencesPage } from "../preferences/PreferencesPage";
import { BrowserWorkspace } from "../browser-v2/BrowserWorkspace";
import { ReaderPage } from "../reader/ReaderPage";
import { ProjectReaderPage } from "../reader/ProjectReaderPage";
import { VisualDocumentPage } from "../reader/VisualDocumentPage";
import { ReadRuntimeProvider } from "../runtime/ReadRuntimeContext";
import { navigateTo } from "../routing/navigation";
import { AppShell } from "./AppShell";

function decodeRouteId(
  pathname: string,
  prefix: string,
): string | null {
  if (!pathname.startsWith(prefix)) return null;

  const raw = pathname.slice(prefix.length).split("/")[0];
  if (!raw) return null;

  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
}

function decodeDocumentId(pathname: string): string | null {
  return decodeRouteId(pathname, "/app/reader/");
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

  if (pathname.startsWith("/app/project/")) {
    const projectId = decodeRouteId(pathname, "/app/project/");
    return projectId
      ? <ProjectReaderPage projectId={projectId} />
      : null;
  }

  if (pathname.startsWith("/app/document/")) {
    const localDocumentId = decodeRouteId(pathname, "/app/document/");
    return localDocumentId
      ? <VisualDocumentPage localDocumentId={localDocumentId} />
      : null;
  }

  if (pathname === "/app/browser") {
    return null;
  }

  if (pathname === "/app/preferences") {
    return <PreferencesPage />;
  }

  if (pathname === "/app/account") {
    return <AccountPage />;
  }

  if (pathname === "/app/subscription") {
    return <SubscriptionPage />;
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
  search,
  user,
}: {
  pathname: string;
  search: string;
  user: ReadAuthUser;
}) {
  const browserActive = pathname === "/app/browser";

  return (
    <ReadRuntimeProvider ownerId={user.id}>
      <AppShell pathname={pathname} search={search} user={user}>
        <BrowserWorkspace active={browserActive} userId={user.id} />
        {browserActive ? null : <RouteContent pathname={pathname} />}
      </AppShell>
    </ReadRuntimeProvider>
  );
}

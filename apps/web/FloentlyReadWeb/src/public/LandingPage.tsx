import type { ReactNode } from "react";
import { navigateTo } from "../routing/navigation";
import type { ReadAuthSession } from "../auth/authStore";

function LinkButton({
  href,
  className,
  children,
}: {
  href: string;
  className: string;
  children: ReactNode;
}) {
  return (
    <a
      href={href}
      className={className}
      onClick={(event) => {
        event.preventDefault();
        navigateTo(href);
      }}
    >
      {children}
    </a>
  );
}

export function LandingPage({
  session,
}: {
  session: ReadAuthSession | null;
}) {
  return (
    <main className="public-shell">
      <header className="public-nav">
        <a
          href="/"
          className="brand-link"
          onClick={(event) => {
            event.preventDefault();
            navigateTo("/");
          }}
        >
          <span className="brand-mark" aria-hidden="true">F</span>
          <span>Floently Read</span>
        </a>

        <nav aria-label="Public navigation">
          {session ? (
            <LinkButton href="/app/library" className="nav-primary">
              Open Read
            </LinkButton>
          ) : (
            <>
              <LinkButton href="/login" className="nav-secondary">
                Sign in
              </LinkButton>
              <LinkButton href="/signup" className="nav-primary">
                Get started
              </LinkButton>
            </>
          )}
        </nav>
      </header>

      <section className="landing-hero">
        <div className="landing-copy">
          <p className="eyebrow">Read naturally. Keep your place.</p>
          <h1>Listen to long documents as one continuous reading experience.</h1>
          <p className="landing-lead">
            Floently Read keeps text, playback, seeking, speed and voice on one
            document-wide timeline, so reading can continue smoothly across the
            browser without exposing the audio chunks used underneath.
          </p>

          <div className="landing-actions">
            <LinkButton
              href={session ? "/app/library" : "/signup"}
              className="hero-primary"
            >
              {session ? "Open your library" : "Start reading"}
            </LinkButton>
            <LinkButton href="/login" className="hero-secondary">
              {session ? "Switch account" : "I already have an account"}
            </LinkButton>
          </div>
        </div>

        <div className="landing-reader-preview" aria-label="Read preview">
          <div className="preview-topline">
            <span className="preview-dot" />
            <span>One document timeline</span>
            <strong>42%</strong>
          </div>
          <article className="preview-page">
            <span className="preview-current">Currently reading</span>
            <p>
              A reader should feel like one book or document, not a sequence of
              unrelated audio clips. The next-generation engine keeps the
              person-facing document stable while preparing only what is needed
              next.
            </p>
          </article>
          <div className="preview-player">
            <button type="button" tabIndex={-1} aria-hidden="true">−15</button>
            <button
              className="preview-play"
              type="button"
              tabIndex={-1}
              aria-hidden="true"
            >
              Play
            </button>
            <button type="button" tabIndex={-1} aria-hidden="true">+15</button>
            <span className="preview-track"><i /></span>
            <strong>1.25×</strong>
          </div>
        </div>
      </section>

      <section className="landing-pillars" aria-label="Read principles">
        <article>
          <span>01</span>
          <h2>One document</h2>
          <p>
            Position and progress belong to the document, not to hidden speech
            segments.
          </p>
        </article>
        <article>
          <span>02</span>
          <h2>Progressive playback</h2>
          <p>
            Nearby audio and text can be prepared ahead without loading an
            entire multi-hour document into the visible interface.
          </p>
        </article>
        <article>
          <span>03</span>
          <h2>Built for continuity</h2>
          <p>
            Playback can remain active while you move through the signed-in
            application.
          </p>
        </article>
      </section>
    </main>
  );
}

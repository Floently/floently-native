import {
  useEffect,
  useMemo,
  useState,
  type MouseEvent,
} from "react";
import type { ReadCoreWorkerClient } from "../readCore.client";
import type {
  ReadingManifestSummary,
  ReadingSegmentDescriptor,
  ReadingSegmentWindow,
} from "../readCore.types";
import type {
  WebPlaybackSession,
  WebPlaybackSnapshot,
} from "../playback/webPlaybackSession";

const READER_WINDOW_RADIUS = 2;
const READER_PAGE_STRIDE = 4;

function clamp(value: number, minimum: number, maximum: number): number {
  return Math.min(maximum, Math.max(minimum, value));
}

function seekTimeForPointer(
  event: MouseEvent<HTMLElement>,
  segment: ReadingSegmentDescriptor,
): number {
  const rectangle = event.currentTarget.getBoundingClientRect();
  const fraction = rectangle.height > 0
    ? clamp((event.clientY - rectangle.top) / rectangle.height, 0, 1)
    : 0;

  return segment.logicalStartMs
    + (segment.logicalEndMs - segment.logicalStartMs) * fraction;
}

export function ReaderSurface({
  core,
  manifest,
  session,
  snapshot,
}: {
  core: ReadCoreWorkerClient;
  manifest: ReadingManifestSummary;
  session: WebPlaybackSession;
  snapshot: WebPlaybackSnapshot;
}) {
  const [windowValue, setWindowValue] =
    useState<ReadingSegmentWindow | null>(null);
  const [viewingCenterIndex, setViewingCenterIndex] = useState(
    snapshot.activeSegmentIndex ?? 0,
  );
  const [isFollowingPlayback, setIsFollowingPlayback] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setWindowValue(null);
    setError(null);
    setIsFollowingPlayback(true);
    setViewingCenterIndex(snapshot.activeSegmentIndex ?? 0);
  }, [manifest.handle]);

  useEffect(() => {
    if (!isFollowingPlayback || snapshot.activeSegmentIndex === null) {
      return;
    }

    setViewingCenterIndex(snapshot.activeSegmentIndex);
  }, [isFollowingPlayback, snapshot.activeSegmentIndex]);

  useEffect(() => {
    if (
      !isFollowingPlayback
      || snapshot.activeSegmentIndex !== null
      || snapshot.elapsedMs <= 0
    ) {
      return;
    }

    let cancelled = false;

    void core
      .segmentForLogicalTime(manifest.handle, snapshot.elapsedMs)
      .then((position) => {
        if (!cancelled && position) {
          setViewingCenterIndex(position.index);
        }
      })
      .catch(() => undefined);

    return () => {
      cancelled = true;
    };
  }, [
    core,
    isFollowingPlayback,
    manifest.handle,
    snapshot.activeSegmentIndex,
    snapshot.elapsedMs,
  ]);

  useEffect(() => {
    let cancelled = false;
    setError(null);

    void core
      .getSegmentWindow(
        manifest.handle,
        viewingCenterIndex,
        READER_WINDOW_RADIUS,
      )
      .then((value) => {
        if (!cancelled) {
          setWindowValue(value);
        }
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error ? reason.message : String(reason));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [core, manifest.handle, viewingCenterIndex]);

  const documentProgress = useMemo(() => {
    if (
      snapshot.canonicalScalarCursor === null
      || manifest.textScalarLength <= 0
    ) {
      return null;
    }

    return Math.round(
      clamp(
        snapshot.canonicalScalarCursor / manifest.textScalarLength,
        0,
        1,
      ) * 100,
    );
  }, [manifest.textScalarLength, snapshot.canonicalScalarCursor]);

  function browseBy(delta: number): void {
    setIsFollowingPlayback(false);
    setViewingCenterIndex((current) =>
      clamp(
        current + delta * READER_PAGE_STRIDE,
        0,
        Math.max(0, manifest.segmentCount - 1),
      ),
    );
  }

  function followPlayback(): void {
    setIsFollowingPlayback(true);

    if (snapshot.activeSegmentIndex !== null) {
      setViewingCenterIndex(snapshot.activeSegmentIndex);
      return;
    }

    void core
      .segmentForLogicalTime(manifest.handle, snapshot.elapsedMs)
      .then((position) => {
        if (position) {
          setViewingCenterIndex(position.index);
        }
      })
      .catch(() => undefined);
  }

  function seekFromRegion(
    event: MouseEvent<HTMLElement>,
    segment: ReadingSegmentDescriptor,
  ): void {
    const targetMs = seekTimeForPointer(event, segment);
    setIsFollowingPlayback(true);
    setViewingCenterIndex(segment.index);
    void session.seek(targetMs);
  }

  const canBrowseEarlier = Boolean(
    windowValue && windowValue.startIndex > 0,
  );
  const canBrowseLater = Boolean(
    windowValue
      && windowValue.endIndexExclusive < windowValue.totalSegments,
  );

  return (
    <section className="reader-surface" aria-label="Document reader">
      <header className="reader-surface-header">
        <div>
          <p className="panel-kicker">Reading surface</p>
          <h2>{manifest.title}</h2>
          <p className="reader-surface-status">
            {documentProgress === null
              ? "Document ready"
              : `${documentProgress}% through document`}
            {" · "}
            {isFollowingPlayback
              ? "Following playback"
              : "Browsing independently"}
          </p>
        </div>

        {!isFollowingPlayback ? (
          <button
            className="reader-follow-button"
            type="button"
            onClick={followPlayback}
          >
            Follow playback
          </button>
        ) : null}
      </header>

      <div className="reader-navigation" aria-label="Browse document">
        <button
          type="button"
          disabled={!canBrowseEarlier}
          onClick={() => browseBy(-1)}
        >
          Earlier
        </button>
        <span>
          {windowValue
            ? `${windowValue.segments.length} reading regions loaded`
            : "Loading reading regions…"}
        </span>
        <button
          type="button"
          disabled={!canBrowseLater}
          onClick={() => browseBy(1)}
        >
          Later
        </button>
      </div>

      {error ? (
        <p className="reader-surface-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="reader-window" aria-live="polite">
        {windowValue?.segments.map((segment) => {
          const isActive = snapshot.activeSegmentIndex === segment.index;

          return (
            <article
              className={
                isActive
                  ? "reader-region reader-region-active"
                  : "reader-region"
              }
              key={segment.id}
              aria-current={isActive ? "location" : undefined}
              onClick={(event) => seekFromRegion(event, segment)}
            >
              {isActive ? (
                <span className="reader-region-current">
                  Current playback region
                </span>
              ) : null}
              <p>{segment.text}</p>
            </article>
          );
        })}

        {!windowValue && !error ? (
          <div className="reader-window-loading">
            Loading the nearby document window…
          </div>
        ) : null}
      </div>

      <p className="reader-surface-note">
        Only the nearby reading window is mounted in the interface. The full
        document remains inside the worker, while playback continues on one
        document-wide timeline.
      </p>
    </section>
  );
}

import { useSyncExternalStore } from "react";
import type {
  WebPlaybackSession,
  WebPlaybackSnapshot,
} from "./webPlaybackSession";

const EMPTY_SNAPSHOT: WebPlaybackSnapshot = {
  status: "idle",
  documentId: null,
  revisionId: null,
  title: null,
  author: null,
  durationMs: 0,
  elapsedMs: 0,
  bufferedAheadMs: 0,
  activeSegmentIndex: null,
  canonicalScalarCursor: null,
  speed: 1,
  voiceId: "",
  error: null,
};

export function useWebPlaybackSnapshot(
  session: WebPlaybackSession | null | undefined,
): WebPlaybackSnapshot {
  return useSyncExternalStore(
    session?.subscribe ?? (() => () => undefined),
    session?.getSnapshot ?? (() => EMPTY_SNAPSHOT),
    () => EMPTY_SNAPSHOT,
  );
}

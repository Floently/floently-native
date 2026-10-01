import { useSyncExternalStore } from "react";
import type {
  ReadDocumentSession,
  ReadDocumentSessionSnapshot,
} from "./readDocumentSession";

const EMPTY: ReadDocumentSessionSnapshot = {
  status: "idle",
  documentId: null,
  revisionId: null,
  title: null,
  manifest: null,
  error: null,
};

export function useReadDocumentSnapshot(
  session: ReadDocumentSession | null | undefined,
): ReadDocumentSessionSnapshot {
  return useSyncExternalStore(
    session?.subscribe ?? (() => () => undefined),
    session?.getSnapshot ?? (() => EMPTY),
    () => EMPTY,
  );
}

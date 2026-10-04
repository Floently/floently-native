import { useSyncExternalStore } from "react";
import type {
  BrowserReadingBridge,
  BrowserReadingBridgeSnapshot,
} from "./BrowserReadingBridge";

const EMPTY: BrowserReadingBridgeSnapshot = {
  status: "idle",
  tabId: null,
  documentId: null,
  title: null,
  canonicalUrl: null,
  revisionId: null,
  follow: false,
  error: null,
};

export function useBrowserReadingSnapshot(
  bridge: BrowserReadingBridge | null | undefined,
): BrowserReadingBridgeSnapshot {
  return useSyncExternalStore(
    bridge?.subscribe ?? (() => () => undefined),
    bridge?.getSnapshot ?? (() => EMPTY),
    () => EMPTY,
  );
}

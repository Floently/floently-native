import { useSyncExternalStore } from "react";
import {
  getReadPreferencesSnapshot,
  subscribeReadPreferences,
} from "./readPreferencesStore";

export function useReadPreferences() {
  return useSyncExternalStore(
    subscribeReadPreferences,
    getReadPreferencesSnapshot,
    getReadPreferencesSnapshot,
  );
}

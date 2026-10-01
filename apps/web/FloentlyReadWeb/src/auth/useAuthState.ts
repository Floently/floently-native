import { useSyncExternalStore } from "react";
import {
  getAuthState,
  subscribeAuthState,
  type ReadAuthState,
} from "./authStore";

export function useAuthState(): ReadAuthState {
  return useSyncExternalStore(
    subscribeAuthState,
    getAuthState,
    getAuthState,
  );
}

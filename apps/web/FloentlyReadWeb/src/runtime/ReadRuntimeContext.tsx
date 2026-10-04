import {
  createContext,
  useContext,
  useEffect,
  useState,
  type PropsWithChildren,
} from "react";
import { ReadCoreWorkerClient } from "../readCore.client";
import { ReadAudioCache } from "../playback/readAudioCache";
import { WebPlaybackSession } from "../playback/webPlaybackSession";
import { RenderReadTtsProvider } from "../tts/readTtsProvider";
import { getAuthAccessToken } from "../auth/authStore";
import { getReadApiBaseUrl } from "../config/runtime";
import { ReadDocumentSession } from "./readDocumentSession";
import { BrowserV2CloudClient } from "../browser-v2/BrowserV2CloudClient";
import { BrowserReadingBridge } from "../browser-v2/BrowserReadingBridge";
import {
  effectiveReadHighlightMode,
  getReadPreferencesSnapshot,
  subscribeReadPreferences,
} from "../preferences/readPreferencesStore";

export interface ReadWebRuntime {
  core: ReadCoreWorkerClient;
  playback: WebPlaybackSession;
  documents: ReadDocumentSession;
  browser: BrowserV2CloudClient;
  browserReading: BrowserReadingBridge;
  tts: RenderReadTtsProvider;
}

const ReadRuntimeContext = createContext<ReadWebRuntime | null>(null);

export function ReadRuntimeProvider({
  children,
  ownerId,
}: PropsWithChildren<{ ownerId: string }>) {
  const [runtime, setRuntime] = useState<ReadWebRuntime | null>(null);

  useEffect(() => {
    const core = new ReadCoreWorkerClient();
    const tts = new RenderReadTtsProvider({
      baseUrl: getReadApiBaseUrl(),
      getAccessToken: getAuthAccessToken,
    });
    const cache = new ReadAudioCache({ ownerId });
    const preferences = getReadPreferencesSnapshot();
    const playback = new WebPlaybackSession({
      core,
      tts,
      ownerId,
      cache,
      initialPreferences: {
        speed: preferences.speed,
        voiceId: preferences.selectedVoiceId,
        voiceLanguage: preferences.selectedVoiceLanguage,
      },
    });
    const documents = new ReadDocumentSession(core, playback);
    const browser = new BrowserV2CloudClient();
    const browserReading = new BrowserReadingBridge(
      browser.reader(),
      core,
      playback,
    );

    const applyHighlightPreference = () => {
      const current = getReadPreferencesSnapshot();
      browserReading.setHighlightEnabled(
        effectiveReadHighlightMode(
          current.highlightMode,
          false,
        ) !== "none",
      );
    };
    applyHighlightPreference();
    const unsubscribePreferences =
      subscribeReadPreferences(applyHighlightPreference);

    const nextRuntime: ReadWebRuntime = {
      core,
      playback,
      documents,
      browser,
      browserReading,
      tts,
    };

    setRuntime(nextRuntime);

    return () => {
      unsubscribePreferences();
      browserReading.destroy();
      void browser.stop().catch(() => undefined);
      documents.destroy();
      playback.destroy();
      core.terminate();
    };
  }, [ownerId]);

  return (
    <ReadRuntimeContext.Provider value={runtime}>
      {children}
    </ReadRuntimeContext.Provider>
  );
}

export function useReadRuntime(): ReadWebRuntime | null {
  return useContext(ReadRuntimeContext);
}

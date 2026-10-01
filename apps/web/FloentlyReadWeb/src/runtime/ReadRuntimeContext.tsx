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

export interface ReadWebRuntime {
  core: ReadCoreWorkerClient;
  playback: WebPlaybackSession;
  documents: ReadDocumentSession;
  tts: RenderReadTtsProvider;
}

const ReadRuntimeContext = createContext<ReadWebRuntime | null>(null);

export function ReadRuntimeProvider({ children }: PropsWithChildren) {
  const [runtime, setRuntime] = useState<ReadWebRuntime | null>(null);

  useEffect(() => {
    const core = new ReadCoreWorkerClient();
    const tts = new RenderReadTtsProvider({
      baseUrl: getReadApiBaseUrl(),
      getAccessToken: getAuthAccessToken,
    });
    const cache = new ReadAudioCache();
    const playback = new WebPlaybackSession({
      core,
      tts,
      cache,
    });
    const documents = new ReadDocumentSession(core, playback);

    const nextRuntime: ReadWebRuntime = {
      core,
      playback,
      documents,
      tts,
    };

    setRuntime(nextRuntime);

    return () => {
      documents.destroy();
      playback.destroy();
      core.terminate();
    };
  }, []);

  return (
    <ReadRuntimeContext.Provider value={runtime}>
      {children}
    </ReadRuntimeContext.Provider>
  );
}

export function useReadRuntime(): ReadWebRuntime | null {
  return useContext(ReadRuntimeContext);
}

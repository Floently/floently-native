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

export interface ReadWebRuntime {
  core: ReadCoreWorkerClient;
  playback: WebPlaybackSession;
  tts: RenderReadTtsProvider;
}

const ReadRuntimeContext = createContext<ReadWebRuntime | null>(null);

export function ReadRuntimeProvider({ children }: PropsWithChildren) {
  const [runtime, setRuntime] = useState<ReadWebRuntime | null>(null);

  useEffect(() => {
    const core = new ReadCoreWorkerClient();
    const tts = new RenderReadTtsProvider();
    const cache = new ReadAudioCache();
    const playback = new WebPlaybackSession({
      core,
      tts,
      cache,
    });

    const nextRuntime: ReadWebRuntime = {
      core,
      playback,
      tts,
    };

    setRuntime(nextRuntime);

    return () => {
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

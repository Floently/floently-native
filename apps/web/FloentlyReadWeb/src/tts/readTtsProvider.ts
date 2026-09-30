export interface ReadVoice {
  id: string;
  name: string;
  language: string;
  locale: string;
  gender?: string | null;
  accent?: string | null;
  description?: string | null;
  previewUrl?: string | null;
}

export interface ReadVoiceCatalog {
  defaultVoiceId: string;
  voices: ReadVoice[];
}

export interface ReadTtsInput {
  text: string;
  language: string;
  voiceId: string;
  speed: number;
  segmentId: string;
}

export interface ReadTtsAsset {
  audioUrl: string;
  cacheKey: string;
  contentHash: string;
  cacheHit: boolean;
  voiceId: string;
  provider?: string | null;
  model?: string | null;
  durationMs?: number | null;
  rawWordTimings: unknown[];
}

export interface ReadTtsProvider {
  readonly id: string;
  synthesize(input: ReadTtsInput): Promise<ReadTtsAsset>;
  listVoices?(): Promise<ReadVoiceCatalog>;
}

export type AccessTokenResolver =
  () => string | null | Promise<string | null>;

const DEFAULT_READ_API_BASE_URL = "https://flowreader-api.onrender.com";
const DEFAULT_VOICE_ID = "google:en-US-Neural2-C";
const DEFAULT_FINNISH_VOICE_ID = "azure:fi-FI-SelmaNeural";

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}

function asOptionalString(value: unknown): string | null {
  return typeof value === "string" && value.trim()
    ? value.trim()
    : null;
}

function resolveDurationMs(source: Record<string, unknown>): number | null {
  const direct = Number(source.durationMs ?? source.duration_ms);
  if (Number.isFinite(direct) && direct >= 0) {
    return direct;
  }

  const seconds = Number(
    source.durationSeconds
      ?? source.duration_seconds,
  );
  if (Number.isFinite(seconds) && seconds >= 0) {
    return seconds * 1_000;
  }

  // The legacy Read endpoint also returns an ambiguous field named
  // "duration". We intentionally do not guess its unit. The physical media
  // element supplies authoritative asset duration after metadata loads.
  return null;
}

async function sha256(value: string): Promise<string> {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text.trim()) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { raw: text };
  }
}

function extractError(payload: unknown, fallback: string): string {
  const record = asRecord(payload);
  const error = asRecord(record.error);
  const detail = asRecord(record.detail);

  return (
    asOptionalString(error.message)
    ?? asOptionalString(record.detail)
    ?? asOptionalString(detail.message)
    ?? asOptionalString(record.message)
    ?? fallback
  );
}

function normalizeVoice(value: unknown): ReadVoice | null {
  const record = asRecord(value);
  const id = asOptionalString(record.id);
  const name = asOptionalString(record.name)
    ?? asOptionalString(record.voiceName)
    ?? id;

  if (!id || !name) return null;

  return {
    id,
    name,
    language: asOptionalString(record.language)?.toLowerCase() ?? "",
    locale: asOptionalString(record.locale) ?? "",
    gender: asOptionalString(record.gender),
    accent: asOptionalString(record.accent),
    description: asOptionalString(record.description),
    previewUrl: asOptionalString(record.previewUrl),
  };
}

export function defaultVoiceIdForLanguage(language: string): string {
  const normalized = language.trim().toLowerCase().split("-")[0];
  return normalized === "fi"
    ? DEFAULT_FINNISH_VOICE_ID
    : DEFAULT_VOICE_ID;
}

export class RenderReadTtsProvider implements ReadTtsProvider {
  readonly id = "render-read-prerender";

  private readonly baseUrl: string;
  private readonly getAccessToken?: AccessTokenResolver;

  constructor(options: {
    baseUrl?: string;
    getAccessToken?: AccessTokenResolver;
  } = {}) {
    this.baseUrl = (
      options.baseUrl
      ?? import.meta.env.VITE_READ_API_BASE_URL
      ?? DEFAULT_READ_API_BASE_URL
    ).replace(/\/+$/, "");
    this.getAccessToken = options.getAccessToken;
  }

  async synthesize(input: ReadTtsInput): Promise<ReadTtsAsset> {
    const text = input.text.trim();
    if (!text) {
      throw new Error("Cannot synthesize an empty Read segment.");
    }

    const headers = await this.headers(true);
    const response = await fetch(`${this.baseUrl}/api/tts/prerender`, {
      method: "POST",
      headers,
      body: JSON.stringify({
        text,
        language: input.language || "auto",
        voiceId: input.voiceId,
      }),
    });

    const payload = await readJson(response);
    if (!response.ok) {
      throw new Error(
        extractError(
          payload,
          `Read TTS request failed with HTTP ${response.status}`,
        ),
      );
    }

    const record = asRecord(payload);
    const data = asRecord(record.data);
    const source = Object.keys(data).length > 0 ? data : record;

    const audioUrl =
      asOptionalString(source.audioUrl)
      ?? asOptionalString(source.audio_url);

    if (!audioUrl) {
      throw new Error("Read TTS returned no audio URL.");
    }

    const providerVoiceId =
      asOptionalString(source.voiceId)
      ?? asOptionalString(source.voice_id)
      ?? input.voiceId;

    const hashInput = JSON.stringify({
      text,
      language: input.language,
      voiceId: providerVoiceId,
      provider: this.id,
    });
    const contentHash = await sha256(hashInput);

    const serverCacheKey =
      asOptionalString(source.cacheKey)
      ?? asOptionalString(source.cache_key);

    return {
      audioUrl,
      cacheKey: serverCacheKey ?? `read-tts:sha256:${contentHash}`,
      contentHash: `sha256:${contentHash}`,
      cacheHit: Boolean(source.cacheHit ?? source.cache_hit),
      voiceId: providerVoiceId,
      provider: asOptionalString(source.provider),
      model: asOptionalString(source.model),
      durationMs: resolveDurationMs(source),
      rawWordTimings: Array.isArray(source.wordTimings)
        ? source.wordTimings
        : Array.isArray(source.word_timings)
          ? source.word_timings
          : [],
    };
  }

  async listVoices(): Promise<ReadVoiceCatalog> {
    const response = await fetch(
      `${this.baseUrl}/api/voices/unified`,
      { headers: await this.headers(false) },
    );
    const payload = await readJson(response);

    if (!response.ok) {
      throw new Error(
        extractError(
          payload,
          `Read voice catalog failed with HTTP ${response.status}`,
        ),
      );
    }

    const record = asRecord(payload);
    const data = asRecord(record.data);
    const source = Object.keys(data).length > 0 ? data : record;
    const rawVoices = Array.isArray(source.voices)
      ? source.voices
      : Array.isArray(source.available)
        ? source.available
        : [];

    const voices = rawVoices
      .map(normalizeVoice)
      .filter((voice): voice is ReadVoice => voice !== null);

    return {
      defaultVoiceId:
        asOptionalString(source.default)
        ?? voices[0]?.id
        ?? DEFAULT_VOICE_ID,
      voices,
    };
  }

  private async headers(json: boolean): Promise<Headers> {
    const headers = new Headers({ Accept: "application/json" });
    if (json) {
      headers.set("Content-Type", "application/json");
    }

    const token = await this.getAccessToken?.();
    if (token?.trim()) {
      headers.set("Authorization", `Bearer ${token.trim()}`);
    }

    return headers;
  }
}

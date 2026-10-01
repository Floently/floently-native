import { ensureGoogleIdentityScript } from "../auth/googleIdentity";

const GOOGLE_API_SCRIPT_ID = "floently-google-api-script";
const GOOGLE_API_SCRIPT_SRC = "https://apis.google.com/js/api.js";
const GOOGLE_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file";

type GoogleTokenResponse = {
  access_token?: string;
  error?: string;
  error_description?: string;
};

type GoogleTokenClient = {
  callback: (response: GoogleTokenResponse) => void;
  requestAccessToken: (options?: { prompt?: string }) => void;
};

export type GoogleDriveImportStatusHandler = (message: string) => void;

const GOOGLE_DRIVE_MEDIA_IMPORT_MAX_BYTES = 15 * 1024 * 1024;
const GOOGLE_DRIVE_PICKER_MIME_TYPES = [
  "application/pdf",
  "text/plain",
  "text/markdown",
  "text/csv",
  "application/vnd.google-apps.document",
  "application/vnd.google-apps.presentation",
  "application/vnd.google-apps.spreadsheet",
  "audio/mpeg",
  "audio/mp3",
  "audio/mp4",
  "audio/wav",
  "audio/x-wav",
  "audio/webm",
  "audio/ogg",
  "video/mp4",
  "video/webm",
  "video/quicktime",
  "video/x-matroska",
].join(",");


function formatBytes(value: number): string {
  if (!Number.isFinite(value) || value <= 0) {
    return "0 MB";
  }

  if (value >= 1024 * 1024) {
    return `${(value / (1024 * 1024)).toFixed(1)} MB`;
  }

  return `${Math.max(1, Math.round(value / 1024))} KB`;
}

function isMediaMimeType(mimeType: string): boolean {
  const normalized = mimeType.toLowerCase();
  return normalized.startsWith("audio/") || normalized.startsWith("video/");
}

function parseDriveSize(value: string | undefined): number | null {
  if (!value) {
    return null;
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : null;
}

async function downloadDriveFileById(
  fileId: string,
  accessToken: string,
  options: { onStatus?: GoogleDriveImportStatusHandler } = {},
): Promise<File> {
  options.onStatus?.("Checking Google Drive file...");
  const metadata = await fetchDriveMetadata(fileId, accessToken);
  const sizeBytes = parseDriveSize(metadata.size);

  if (isMediaMimeType(metadata.mimeType) && sizeBytes !== null && sizeBytes > GOOGLE_DRIVE_MEDIA_IMPORT_MAX_BYTES) {
    throw new Error(
      `${metadata.name} is ${formatBytes(sizeBytes)}. Media imports are currently limited to ${formatBytes(GOOGLE_DRIVE_MEDIA_IMPORT_MAX_BYTES)}. Please choose a smaller audio or video file first.`,
    );
  }

  options.onStatus?.(`Downloading ${metadata.name} from Google Drive...`);
  const file = await fetchDriveFile(metadata, accessToken);
  options.onStatus?.(`Downloaded ${file.name} (${formatBytes(file.size)}). Preparing import...`);

  return file;
}

type PickedDriveDocument = {
  id: string;
  name?: string;
  mimeType?: string;
};

type DriveFileMetadata = {
  id: string;
  name: string;
  mimeType: string;
  size?: string;
  capabilities?: {
    canDownload?: boolean;
  };
};

export class GoogleDriveImportCancelledError extends Error {
  constructor() {
    super("Google Drive import cancelled.");
    this.name = "GoogleDriveImportCancelledError";
  }
}

export class GoogleDriveImportConfigError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "GoogleDriveImportConfigError";
  }
}

function googleDriveApiKey(): string {
  return (import.meta.env.VITE_GOOGLE_DRIVE_API_KEY ?? "").trim();
}

function googleDriveAppId(): string {
  return (import.meta.env.VITE_GOOGLE_DRIVE_APP_ID ?? "").trim();
}

function requireBrowserWindow(): Window {
  if (typeof window === "undefined") {
    throw new Error("Google Drive import can only run in the browser.");
  }

  return window;
}

function googleDriveConfig(clientId: string | null | undefined) {
  const apiKey = googleDriveApiKey();
  const appId = googleDriveAppId();

  if (!clientId) {
    throw new GoogleDriveImportConfigError("Google sign-in client ID is not configured yet.");
  }

  if (!apiKey || !appId) {
    throw new GoogleDriveImportConfigError(
      "Google Drive import needs VITE_GOOGLE_DRIVE_API_KEY and VITE_GOOGLE_DRIVE_APP_ID in the frontend build environment.",
    );
  }

  return { apiKey, appId, clientId };
}

function loadScript(id: string, src: string): Promise<void> {
  const win = requireBrowserWindow();
  const existing = win.document.getElementById(id) as HTMLScriptElement | null;

  if (existing?.dataset.ready === "true") {
    return Promise.resolve();
  }

  if (existing) {
    return new Promise((resolve, reject) => {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error(`${src} did not load.`)), { once: true });
    });
  }

  return new Promise((resolve, reject) => {
    const script = win.document.createElement("script");
    script.id = id;
    script.src = src;
    script.async = true;
    script.defer = true;
    script.onload = () => {
      script.dataset.ready = "true";
      resolve();
    };
    script.onerror = () => reject(new Error(`${src} did not load.`));
    win.document.head.appendChild(script);
  });
}

async function ensureGooglePicker(): Promise<void> {
  const win = requireBrowserWindow() as Window & { gapi?: any };

  await loadScript(GOOGLE_API_SCRIPT_ID, GOOGLE_API_SCRIPT_SRC);

  if (!win.gapi?.load) {
    throw new Error("Google API loader is unavailable.");
  }

  await new Promise<void>((resolve, reject) => {
    win.gapi.load("picker", {
      callback: () => resolve(),
      onerror: () => reject(new Error("Google Picker could not be loaded.")),
      ontimeout: () => reject(new Error("Google Picker loading timed out.")),
      timeout: 8000,
    });
  });
}

async function requestDriveAccessToken(
  clientId: string,
  options: { scope?: string; timeoutMessage?: string } = {},
): Promise<string> {
  await ensureGoogleIdentityScript();

  const win = requireBrowserWindow() as Window & { google?: any };
  const oauth = win.google?.accounts?.oauth2;

  if (!oauth?.initTokenClient) {
    throw new Error("Google OAuth token client is unavailable.");
  }

  return new Promise((resolve, reject) => {
    let settled = false;
    const finish = <T,>(handler: (value: T) => void, value: T) => {
      if (settled) {
        return;
      }

      settled = true;
      window.clearTimeout(timeoutId);
      handler(value);
    };

    const timeoutId = window.setTimeout(() => {
      finish(
        reject,
        new Error(
          options.timeoutMessage ||
            "Google Drive permission did not finish. Allow pop-ups for read.floently.com and try again.",
        ),
      );
    }, 20000);

    const tokenClient = oauth.initTokenClient({
      client_id: clientId,
      scope: options.scope ?? GOOGLE_DRIVE_SCOPE,
      callback: (response: GoogleTokenResponse) => {
        if (response.error) {
          finish(reject, new Error(response.error_description || response.error));
          return;
        }

        if (!response.access_token) {
          finish(reject, new Error("Google did not return an access token."));
          return;
        }

        finish(resolve, response.access_token);
      },
    }) as GoogleTokenClient;

    try {
      tokenClient.requestAccessToken({ prompt: "consent" });
    } catch (error) {
      finish(reject, error instanceof Error ? error : new Error("Google permission request failed."));
    }
  });
}

function openGooglePicker({
  accessToken,
  apiKey,
  appId,
  onStatus,
}: {
  accessToken: string;
  apiKey: string;
  appId: string;
  onStatus?: GoogleDriveImportStatusHandler;
}): Promise<PickedDriveDocument> {
  const win = requireBrowserWindow() as Window & { google?: any };
  const pickerApi = win.google?.picker;

  if (!pickerApi?.PickerBuilder) {
    throw new Error("Google Picker API is unavailable.");
  }

  return new Promise((resolve, reject) => {
    const ViewConstructor =
      typeof pickerApi.DocsView === "function" ? pickerApi.DocsView : pickerApi.View;
    const view = new ViewConstructor(pickerApi.ViewId.DOCS);

    view.setMimeTypes(GOOGLE_DRIVE_PICKER_MIME_TYPES);

    if (typeof view.setIncludeFolders === "function") {
      view.setIncludeFolders(true);
    }

    if (typeof view.setSelectFolderEnabled === "function") {
      view.setSelectFolderEnabled(false);
    }

    if (typeof view.setMode === "function" && pickerApi.DocsViewMode?.LIST) {
      view.setMode(pickerApi.DocsViewMode.LIST);
    }

    const picker = new pickerApi.PickerBuilder()
      .setAppId(appId)
      .setDeveloperKey(apiKey)
      .setOAuthToken(accessToken)
      .addView(view)
      .setSelectableMimeTypes(GOOGLE_DRIVE_PICKER_MIME_TYPES)
      .setCallback((data: any) => {
        const action = data?.[pickerApi.Response.ACTION] ?? data?.action;
        const docs = data?.[pickerApi.Response.DOCUMENTS] ?? data?.docs ?? data?.documents ?? [];
        const selected = Array.isArray(docs) ? docs[0] : null;

        (window as any).__FLOWREADER_LAST_PICKER_DATA = data;
        try {
          console.info("[FlowReader] Google Picker callback JSON", JSON.stringify(data, null, 2));
        } catch {
          console.info("[FlowReader] Google Picker callback", data);
        }

        onStatus?.(`Google Picker returned: ${String(action || "unknown")}`);

        if ((action === pickerApi.Action.CANCEL || action === "cancel") && !selected) {
          reject(new Error("Google Picker did not return a selected file. Click the file once so it is highlighted, then press Select again."));
          return;
        }

        if (action !== pickerApi.Action.PICKED && action !== "picked" && !selected) {
          onStatus?.(`Google Picker returned no selected file: ${String(action || "unknown")}`);
          return;
        }

        if (!selected) {
          reject(new Error("No Google Drive file was selected."));
          return;
        }

        const selectedId = selected[pickerApi.Document.ID] ?? selected.id;
        const selectedName = selected[pickerApi.Document.NAME] ?? selected.name ?? selected.title ?? "Google Drive file";
        const selectedMimeType = selected[pickerApi.Document.MIME_TYPE] ?? selected.mimeType ?? selected.mime_type ?? "";

        if (!selectedId) {
          reject(new Error("Google Drive did not return a selected file ID."));
          return;
        }

        onStatus?.(`Selected ${selectedName}. Checking file...`);

        resolve({
          id: selectedId,
          name: selectedName,
          mimeType: selectedMimeType,
        });
      })
      .build();

    picker.setVisible(true);
  });
}

function authHeaders(accessToken: string): HeadersInit {
  return {
    Authorization: `Bearer ${accessToken}`,
  };
}

async function fetchDriveMetadata(fileId: string, accessToken: string): Promise<DriveFileMetadata> {
  const url = new URL(`https://www.googleapis.com/drive/v3/files/${encodeURIComponent(fileId)}`);
  url.searchParams.set("fields", "id,name,mimeType,size,capabilities/canDownload");

  const response = await fetch(url.toString(), {
    headers: authHeaders(accessToken),
  });

  if (!response.ok) {
    throw new Error(`Could not read Google Drive file metadata (${response.status}).`);
  }

  return response.json() as Promise<DriveFileMetadata>;
}

function exportMimeType(mimeType: string): string {
  if (mimeType === "application/vnd.google-apps.spreadsheet") {
    return "text/csv";
  }

  return "text/plain";
}

function outputExtension(mimeType: string, responseType: string): string {
  if (mimeType === "application/pdf" || responseType.includes("pdf")) return ".pdf";
  if (mimeType === "audio/mpeg" || mimeType === "audio/mp3" || responseType.includes("mpeg")) return ".mp3";
  if (mimeType === "audio/mp4") return ".m4a";
  if (mimeType === "audio/wav" || mimeType === "audio/x-wav" || responseType.includes("wav")) return ".wav";
  if (mimeType === "audio/webm") return ".webm";
  if (mimeType === "audio/ogg") return ".ogg";
  if (mimeType === "video/mp4") return ".mp4";
  if (mimeType === "video/webm") return ".webm";
  if (mimeType === "video/quicktime") return ".mov";
  if (mimeType === "video/x-matroska") return ".mkv";
  if (responseType.includes("csv")) return ".csv";
  if (responseType.includes("markdown")) return ".md";
  return ".txt";
}

function safeFilename(name: string, extension: string): string {
  const cleaned = name.replace(/[\\/:*?"<>|]+/g, "-").replace(/\s+/g, " ").trim() || "google-drive-import";

  if (/\.[a-z0-9]{2,8}$/i.test(cleaned)) {
    return cleaned;
  }

  return `${cleaned}${extension}`;
}

async function fetchDriveFile(metadata: DriveFileMetadata, accessToken: string): Promise<File> {
  if (metadata.capabilities?.canDownload === false) {
    throw new Error("This Google Drive file cannot be downloaded by the current user.");
  }

  const isGoogleWorkspaceFile = metadata.mimeType.startsWith("application/vnd.google-apps.");
  const requestedMimeType = isGoogleWorkspaceFile ? exportMimeType(metadata.mimeType) : metadata.mimeType;

  const url = isGoogleWorkspaceFile
    ? new URL(`https://www.googleapis.com/drive/v3/files/${encodeURIComponent(metadata.id)}/export`)
    : new URL(`https://www.googleapis.com/drive/v3/files/${encodeURIComponent(metadata.id)}`);

  if (isGoogleWorkspaceFile) {
    url.searchParams.set("mimeType", requestedMimeType);
  } else {
    url.searchParams.set("alt", "media");
  }

  const response = await fetch(url.toString(), {
    headers: authHeaders(accessToken),
  });

  if (!response.ok) {
    throw new Error(`Could not download Google Drive file (${response.status}).`);
  }

  const blob = await response.blob();
  const responseType = blob.type || requestedMimeType || "text/plain";
  const filename = safeFilename(metadata.name, outputExtension(metadata.mimeType, responseType));

  return new File([blob], filename, {
    type: responseType,
  });
}

export async function pickGoogleDriveFileAsFile(
  clientId: string | null | undefined,
  options: { onStatus?: GoogleDriveImportStatusHandler } = {},
): Promise<File> {
  const config = googleDriveConfig(clientId);

  options.onStatus?.("Loading Google Drive picker...");
  await ensureGooglePicker();

  options.onStatus?.("Requesting Google Drive permission...");
  const accessToken = await requestDriveAccessToken(config.clientId);

  options.onStatus?.("Waiting for file selection...");
  const picked = await openGooglePicker({
    accessToken,
    apiKey: config.apiKey,
    appId: config.appId,
    onStatus: options.onStatus,
  });

  if (!picked.id) {
    throw new Error("Google Drive did not return a file ID.");
  }

  options.onStatus?.(`Checking ${picked.name || "selected file"}...`);
  return downloadDriveFileById(picked.id, accessToken, options);
}


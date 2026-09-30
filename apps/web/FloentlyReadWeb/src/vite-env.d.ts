/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_READ_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

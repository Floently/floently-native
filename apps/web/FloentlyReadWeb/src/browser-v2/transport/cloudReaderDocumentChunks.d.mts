/** A private article is reassembled only after every bounded WS chunk arrives. */
export const MAX_READER_DOCUMENT_BYTES: number;
export const READER_CHUNK_BYTES: number;
export class CloudReaderDocumentChunks {
  reset(): void;
  append(payload: unknown): {sentences: unknown[]; [key: string]: unknown} | null;
}

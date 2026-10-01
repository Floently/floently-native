/** Private article transport: bounded, ordered reassembly, never logs source text. */
export const MAX_READER_DOCUMENT_BYTES = 256_000;
export const READER_CHUNK_BYTES = 6_000;
const MAX_CHUNKS = Math.ceil(MAX_READER_DOCUMENT_BYTES / READER_CHUNK_BYTES);
const invalid = () => new Error("READER_DOCUMENT_TRANSFER_FAILED");

export class CloudReaderDocumentChunks {
  #batch = null;
  #next = 0;
  #total = 0;
  #parts = [];
  #bytes = 0;

  reset() {
    this.#batch = null;
    this.#next = 0;
    this.#total = 0;
    this.#parts = [];
    this.#bytes = 0;
  }

  append(payload) {
    if (!payload || typeof payload !== "object" || Array.isArray(payload))
      throw invalid();
    const {batch,index,total,base64} = payload;
    if (typeof batch !== "string" ||
        !/^[A-Za-z0-9_-]{16}$/.test(batch) ||
        !Number.isSafeInteger(index) || !Number.isSafeInteger(total) ||
        total < 1 || total > MAX_CHUNKS || index < 0 || index >= total ||
        typeof base64 !== "string" || base64.length < 1 ||
        base64.length > 8_000 || !/^[A-Za-z0-9+/]+={0,2}$/.test(base64))
      throw invalid();
    if (index === 0) {
      // An in-flight batch must not be silently replaced by replayed first
      // chunks or fragments of another article. A fresh RPC explicitly resets.
      if (this.#batch !== null) throw invalid();
      this.reset();
      this.#batch = batch;
      this.#total = total;
    }
    if (batch !== this.#batch || total !== this.#total ||
        index !== this.#next)
      throw invalid();

    let bytes;
    try {
      const decoded = atob(base64);
      bytes = Uint8Array.from(decoded, char => char.charCodeAt(0));
    } catch {
      throw invalid();
    }
    if (bytes.length < 1 || bytes.length > READER_CHUNK_BYTES ||
        this.#bytes + bytes.length > MAX_READER_DOCUMENT_BYTES)
      throw invalid();

    this.#bytes += bytes.length;
    this.#parts.push(bytes);
    this.#next += 1;
    if (this.#next < this.#total) return null;

    const full = new Uint8Array(this.#bytes);
    let position = 0;
    for (const part of this.#parts) {
      full.set(part, position);
      position += part.length;
    }
    this.reset();
    try {
      const document = JSON.parse(new TextDecoder("utf-8",{fatal:true}).decode(full));
      if (!document || typeof document !== "object" ||
          Array.isArray(document) ||
          !Array.isArray(document.sentences) ||
          !document.sentences.length)
        throw invalid();
      return document;
    } catch {
      throw invalid();
    }
  }
}

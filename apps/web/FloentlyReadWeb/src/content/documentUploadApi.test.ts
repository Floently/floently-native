import { describe, expect, it } from "vitest";
import { uploadBlobForFile } from "./documentUploadApi";

describe("document upload browser compatibility", () => {
  it("converts a selected File into a Blob-backed multipart payload", async () => {
    const file = new File(
      [new Uint8Array([1, 2, 3, 4])],
      "source.pdf",
      {
        type: "application/pdf",
        lastModified: 123,
      },
    );

    const blob = uploadBlobForFile(file);

    expect(blob).toBeInstanceOf(Blob);
    expect(blob).not.toBeInstanceOf(File);
    expect(blob.type).toBe("application/pdf");
    expect(blob.size).toBe(4);
    expect(
      Array.from(new Uint8Array(await blob.arrayBuffer())),
    ).toEqual([1, 2, 3, 4]);
  });

  it("uses a stable binary content type when the selected file has none", () => {
    const file = new File(["plain bytes"], "unknown.bin");
    const blob = uploadBlobForFile(file);

    expect(blob.type).toBe("application/octet-stream");
    expect(blob.size).toBe(file.size);
  });
});

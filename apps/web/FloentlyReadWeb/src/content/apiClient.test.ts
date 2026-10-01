import { afterEach, describe, expect, it, vi } from "vitest";
import {
  readApiError,
  requestApiJson,
} from "./apiClient";

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
  vi.restoreAllMocks();
});

describe("content api client", () => {
  it("accepts a successful empty response", async () => {
    globalThis.fetch = vi.fn(async () =>
      new Response(null, { status: 204 }),
    ) as typeof fetch;

    await expect(
      requestApiJson<void>(
        "/api/v1/projects/p1",
        { method: "DELETE" },
        "Delete failed.",
      ),
    ).resolves.toBeUndefined();
  });

  it("surfaces a stable backend detail message", async () => {
    const response = new Response(
      JSON.stringify({ detail: "Project is locked." }),
      {
        status: 409,
        headers: { "Content-Type": "application/json" },
      },
    );

    await expect(
      readApiError(response, "Fallback"),
    ).resolves.toBe("Project is locked.");
  });

  it("rejects malformed successful JSON instead of returning unknown data", async () => {
    globalThis.fetch = vi.fn(async () =>
      new Response("not-json", { status: 200 }),
    ) as typeof fetch;

    await expect(
      requestApiJson(
        "/api/v1/projects",
        { method: "GET" },
        "Load failed.",
      ),
    ).rejects.toThrow("invalid response");
  });
});

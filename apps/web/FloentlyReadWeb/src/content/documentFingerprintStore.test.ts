import { afterEach, describe, expect, it } from "vitest";
import {
  documentFingerprintStorageKey,
  forgetProjectForFileFingerprint,
  getProjectForFileFingerprint,
  rememberProjectForFileFingerprint,
} from "./documentFingerprintStore";

const originalWindowDescriptor =
  Object.getOwnPropertyDescriptor(globalThis, "window");

function installLocalStorage(): Storage {
  const values = new Map<string, string>();
  const storage: Storage = {
    get length() {
      return values.size;
    },
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => [...values.keys()][index] ?? null,
    removeItem: (key) => {
      values.delete(key);
    },
    setItem: (key, value) => {
      values.set(key, String(value));
    },
  };

  Object.defineProperty(globalThis, "window", {
    configurable: true,
    value: { localStorage: storage },
  });

  return storage;
}

afterEach(() => {
  if (originalWindowDescriptor) {
    Object.defineProperty(
      globalThis,
      "window",
      originalWindowDescriptor,
    );
  } else {
    Reflect.deleteProperty(globalThis, "window");
  }
});

describe("document fingerprint account isolation", () => {
  it("uses a separate localStorage key per authenticated owner", () => {
    const first = documentFingerprintStorageKey("account-a");
    const second = documentFingerprintStorageKey("account-b");

    expect(first).not.toBe(second);
    expect(first).toContain("file-project-index.v2");
    expect(first).toContain("account-a");
    expect(second).toContain("account-b");
  });

  it("does not reuse another account's project mapping", () => {
    installLocalStorage();

    rememberProjectForFileFingerprint(
      "account-a",
      "same-fingerprint",
      "project-a",
    );

    expect(
      getProjectForFileFingerprint(
        "account-a",
        "same-fingerprint",
      ),
    ).toBe("project-a");
    expect(
      getProjectForFileFingerprint(
        "account-b",
        "same-fingerprint",
      ),
    ).toBeNull();

    rememberProjectForFileFingerprint(
      "account-b",
      "same-fingerprint",
      "project-b",
    );

    expect(
      getProjectForFileFingerprint(
        "account-a",
        "same-fingerprint",
      ),
    ).toBe("project-a");
    expect(
      getProjectForFileFingerprint(
        "account-b",
        "same-fingerprint",
      ),
    ).toBe("project-b");

    forgetProjectForFileFingerprint(
      "account-b",
      "same-fingerprint",
    );

    expect(
      getProjectForFileFingerprint(
        "account-a",
        "same-fingerprint",
      ),
    ).toBe("project-a");
    expect(
      getProjectForFileFingerprint(
        "account-b",
        "same-fingerprint",
      ),
    ).toBeNull();
  });

  it("fails closed when owner identity is missing", () => {
    installLocalStorage();

    expect(() =>
      getProjectForFileFingerprint("", "fingerprint"),
    ).toThrow("Authenticated Read owner is required");
  });
});

import test from "node:test";
import assert from "node:assert/strict";
import {
  validateReadPreviewConfig,
} from "./validate-preview-config.mjs";

function valid(overrides = {}) {
  return {
    READ_PREVIEW_ORIGIN: "https://read-next-preview.example",
    VITE_API_URL: "https://api-preview.example",
    VITE_AUTH_API_URL: "https://auth-preview.example",
    VITE_READ_API_BASE_URL: "https://tts-preview.example",
    VITE_BROWSER_V2_ORIGIN: "https://browser-preview.example",
    ...overrides,
  };
}

test("accepts an isolated HTTPS preview without printing secrets", () => {
  const result = validateReadPreviewConfig(valid({
    VITE_GOOGLE_DRIVE_API_KEY: "browser-public-key-value",
    VITE_GOOGLE_DRIVE_APP_ID: "picker-app-id",
  }));

  assert.deepEqual(result.errors, []);
  assert.equal(
    result.summary.previewOrigin,
    "https://read-next-preview.example",
  );
  assert.equal(result.summary.googleDrivePickerConfigured, true);
  assert.equal(
    JSON.stringify(result.summary).includes("browser-public-key-value"),
    false,
  );
});

test("rejects production Read hosts and insecure or credential-bearing URLs", () => {
  const result = validateReadPreviewConfig(valid({
    READ_PREVIEW_ORIGIN: "https://read.floently.com",
    VITE_API_URL: "http://api-preview.example",
    VITE_AUTH_API_URL: "https://user:pass@auth-preview.example",
  }));

  assert.ok(result.errors.some((error) =>
    error.includes("must not be a Floently production hostname")));
  assert.ok(result.errors.some((error) =>
    error === "VITE_API_URL must use HTTPS."));
  assert.ok(result.errors.some((error) =>
    error === "VITE_AUTH_API_URL must not contain URL credentials."));
});

test("requires Browser V2 to be a pure HTTPS origin", () => {
  const result = validateReadPreviewConfig(valid({
    VITE_BROWSER_V2_ORIGIN:
      "https://browser-preview.example/api/browser-v2?ticket=nope",
  }));

  assert.ok(result.errors.some((error) =>
    error === "VITE_BROWSER_V2_ORIGIN must not contain a query string or fragment."));
  assert.ok(result.errors.some((error) =>
    error === "VITE_BROWSER_V2_ORIGIN must be an origin only, with no path."));
});

test("fails closed when a core preview endpoint is missing", () => {
  const env = valid();
  delete env.VITE_READ_API_BASE_URL;

  const result = validateReadPreviewConfig(env);
  assert.ok(result.errors.some((error) =>
    error === "VITE_READ_API_BASE_URL is required for the isolated Read preview."));
});

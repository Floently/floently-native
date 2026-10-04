#!/usr/bin/env node
import { pathToFileURL } from "node:url";

export const PRODUCTION_READ_HOSTS = new Set([
  "floently.com",
  "www.floently.com",
  "read.floently.com",
  "learn.floently.com",
  "create.floently.com",
]);

function read(env, name, errors) {
  const value = env[name]?.trim() ?? "";
  if (!value) {
    errors.push(`${name} is required for the isolated Read preview.`);
    return null;
  }
  return value;
}

function parseHttps(name, value, errors, options = {}) {
  if (!value) return null;

  let parsed;
  try {
    parsed = new URL(value);
  } catch {
    errors.push(`${name} must be a valid absolute URL.`);
    return null;
  }

  if (parsed.protocol !== "https:") {
    errors.push(`${name} must use HTTPS.`);
  }
  if (parsed.username || parsed.password) {
    errors.push(`${name} must not contain URL credentials.`);
  }
  if (!options.allowQuery && (parsed.search || parsed.hash)) {
    errors.push(`${name} must not contain a query string or fragment.`);
  }
  if (options.originOnly && parsed.pathname !== "/") {
    errors.push(`${name} must be an origin only, with no path.`);
  }

  return parsed;
}

export function validateReadPreviewConfig(env) {
  const errors = [];

  const preview = parseHttps(
    "READ_PREVIEW_ORIGIN",
    read(env, "READ_PREVIEW_ORIGIN", errors),
    errors,
    { originOnly: true },
  );

  if (
    preview
    && PRODUCTION_READ_HOSTS.has(preview.hostname.toLowerCase())
  ) {
    errors.push(
      "READ_PREVIEW_ORIGIN must not be a Floently production hostname.",
    );
  }

  const api = parseHttps(
    "VITE_API_URL",
    read(env, "VITE_API_URL", errors),
    errors,
  );
  const auth = parseHttps(
    "VITE_AUTH_API_URL",
    read(env, "VITE_AUTH_API_URL", errors),
    errors,
  );
  const readApi = parseHttps(
    "VITE_READ_API_BASE_URL",
    read(env, "VITE_READ_API_BASE_URL", errors),
    errors,
  );
  const browserV2 = parseHttps(
    "VITE_BROWSER_V2_ORIGIN",
    read(env, "VITE_BROWSER_V2_ORIGIN", errors),
    errors,
    { originOnly: true },
  );

  const portalValue =
    env.VITE_STRIPE_READ_BILLING_PORTAL_URL?.trim() ?? "";
  const portal = portalValue
    ? parseHttps(
        "VITE_STRIPE_READ_BILLING_PORTAL_URL",
        portalValue,
        errors,
        { allowQuery: true },
      )
    : null;

  return {
    errors,
    summary: {
      previewOrigin: preview?.origin ?? null,
      apiOrigin: api?.origin ?? null,
      authOrigin: auth?.origin ?? null,
      readApiOrigin: readApi?.origin ?? null,
      browserV2Origin: browserV2?.origin ?? null,
      billingPortalConfigured: Boolean(portal),
      googleDrivePickerConfigured: Boolean(
        env.VITE_GOOGLE_DRIVE_API_KEY?.trim()
          && env.VITE_GOOGLE_DRIVE_APP_ID?.trim(),
      ),
    },
  };
}

export function runReadPreviewConfigValidation(env = process.env) {
  const result = validateReadPreviewConfig(env);

  if (result.errors.length > 0) {
    console.error("Read preview configuration is invalid:");
    for (const error of result.errors) {
      console.error(`- ${error}`);
    }
    return 1;
  }

  console.log("Read preview configuration passed fail-closed validation.");
  console.log(JSON.stringify(result.summary, null, 2));
  return 0;
}

const isMain =
  process.argv[1]
  && import.meta.url === pathToFileURL(process.argv[1]).href;

if (isMain) {
  process.exitCode = runReadPreviewConfigValidation();
}

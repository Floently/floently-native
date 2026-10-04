#!/usr/bin/env node

const PRODUCTION_HOSTS = new Set([
  "floently.com",
  "www.floently.com",
  "read.floently.com",
  "learn.floently.com",
  "create.floently.com",
]);

const errors = [];

function required(name) {
  const value = process.env[name]?.trim() ?? "";
  if (!value) {
    errors.push(`${name} is required for the isolated Read preview.`);
    return null;
  }
  return value;
}

function httpsUrl(name, value, options = {}) {
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

const preview = httpsUrl(
  "READ_PREVIEW_ORIGIN",
  required("READ_PREVIEW_ORIGIN"),
  { originOnly: true },
);

if (preview && PRODUCTION_HOSTS.has(preview.hostname.toLowerCase())) {
  errors.push(
    "READ_PREVIEW_ORIGIN must not be a Floently production hostname.",
  );
}

const api = httpsUrl(
  "VITE_API_URL",
  required("VITE_API_URL"),
);
const auth = httpsUrl(
  "VITE_AUTH_API_URL",
  required("VITE_AUTH_API_URL"),
);
const readApi = httpsUrl(
  "VITE_READ_API_BASE_URL",
  required("VITE_READ_API_BASE_URL"),
);
const browserV2 = httpsUrl(
  "VITE_BROWSER_V2_ORIGIN",
  required("VITE_BROWSER_V2_ORIGIN"),
  { originOnly: true },
);

const billingPortalValue =
  process.env.VITE_STRIPE_READ_BILLING_PORTAL_URL?.trim() ?? "";
const billingPortal = billingPortalValue
  ? httpsUrl(
      "VITE_STRIPE_READ_BILLING_PORTAL_URL",
      billingPortalValue,
      { allowQuery: true },
    )
  : null;

if (errors.length > 0) {
  console.error("Read preview configuration is invalid:");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

const publicSummary = {
  previewOrigin: preview?.origin ?? null,
  apiOrigin: api?.origin ?? null,
  authOrigin: auth?.origin ?? null,
  readApiOrigin: readApi?.origin ?? null,
  browserV2Origin: browserV2?.origin ?? null,
  billingPortalConfigured: Boolean(billingPortal),
  googleDrivePickerConfigured: Boolean(
    process.env.VITE_GOOGLE_DRIVE_API_KEY?.trim()
      && process.env.VITE_GOOGLE_DRIVE_APP_ID?.trim(),
  ),
};

console.log("Read preview configuration passed fail-closed validation.");
console.log(JSON.stringify(publicSummary, null, 2));

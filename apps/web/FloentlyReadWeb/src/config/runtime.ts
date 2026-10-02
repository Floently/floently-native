function envValue(name: string): string {
  const value = import.meta.env[name];
  return typeof value === "string" ? value.trim() : "";
}

function normalizeBaseUrl(value: string): string {
  return value.replace(/\/+$/, "");
}

export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const generalApiBase = normalizeBaseUrl(envValue("VITE_API_URL"));

  return generalApiBase
    ? `${generalApiBase}${normalizedPath}`
    : normalizedPath;
}

export function buildAuthApiUrl(path: string): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const hostname =
    typeof window !== "undefined"
      ? window.location.hostname.toLowerCase()
      : "";

  if (hostname === "floently.com" || hostname === "www.floently.com") {
    return normalizedPath;
  }

  if (
    hostname === "read.floently.com"
    || hostname === "learn.floently.com"
    || hostname === "create.floently.com"
  ) {
    return `https://learn-api.floently.com${normalizedPath}`;
  }

  const explicitAuthBase = normalizeBaseUrl(envValue("VITE_AUTH_API_URL"));
  if (explicitAuthBase) {
    return `${explicitAuthBase}${normalizedPath}`;
  }

  const generalApiBase = normalizeBaseUrl(envValue("VITE_API_URL"));
  if (generalApiBase) {
    return `${generalApiBase}${normalizedPath}`;
  }

  return normalizedPath;
}

export function getReadApiBaseUrl(): string | undefined {
  const explicit = normalizeBaseUrl(envValue("VITE_READ_API_BASE_URL"));
  return explicit || undefined;
}

export function getReadBillingPortalUrl(): string | null {
  const candidate =
    envValue("VITE_STRIPE_READ_BILLING_PORTAL_URL")
    || envValue("VITE_STRIPE_BILLING_PORTAL_URL");

  if (!candidate) return null;

  try {
    const parsed = new URL(candidate);
    return parsed.protocol === "https:" ? parsed.href : null;
  } catch {
    return null;
  }
}

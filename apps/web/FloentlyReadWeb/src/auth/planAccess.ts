const APP_ONLY_PLANS = new Set(["app", "app_only", "paid_app", "starter"]);
const FULL_ACCESS_PLANS = new Set([
  "pro", "paid", "premium", "full", "api", "api_included",
  "full_access", "fullaccess",
]);
const READ_READER_PLANS = new Set([
  "reader", "read_reader", "paid_reader", "essential", "read_essential", "scholar",
]);
const READ_CREATOR_PLANS = new Set([
  "creator", "read_creator", "creator_pro", "pro_creator",
  "professional", "read_professional", "read_pro",
]);

export function normalizePlanName(plan: string | null | undefined): string {
  return String(plan || "").trim().toLowerCase().replace(/[- ]+/g, "_");
}

export function hasReaderAccess(plan: string | null | undefined): boolean {
  const normalized = normalizePlanName(plan);
  return APP_ONLY_PLANS.has(normalized)
    || FULL_ACCESS_PLANS.has(normalized)
    || READ_READER_PLANS.has(normalized)
    || READ_CREATOR_PLANS.has(normalized);
}

export function hasFullAccess(plan: string | null | undefined): boolean {
  const normalized = normalizePlanName(plan);
  return FULL_ACCESS_PLANS.has(normalized) || READ_CREATOR_PLANS.has(normalized);
}

export function hasAppOnlyAccess(plan: string | null | undefined): boolean {
  const normalized = normalizePlanName(plan);
  return APP_ONLY_PLANS.has(normalized) || READ_READER_PLANS.has(normalized);
}

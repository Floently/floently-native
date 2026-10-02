import type { ReadAuthSession } from "./authStore";
import {
  hasAppOnlyAccess,
  hasFullAccess,
  normalizePlanName,
} from "./planAccess";

export type ReadPlan =
  | "free"
  | "app"
  | "full_access"
  | "reader"
  | "creator"
  | "paid";

export interface ReadEntitlement {
  isPaid: boolean;
  plan: ReadPlan;
  canExportAudio: boolean;
  canRecord: boolean;
  canTranscribe: boolean;
  canTranslate: boolean;
  allowedVoiceTiers: string[];
}

const DEFAULT_READ_ENTITLEMENT: ReadEntitlement = {
  isPaid: false,
  plan: "free",
  canExportAudio: false,
  canRecord: false,
  canTranscribe: false,
  canTranslate: true,
  allowedVoiceTiers: ["standard_reader"],
};

function readFeature(
  features: Record<string, boolean> | undefined,
  key: string,
  fallback: boolean,
): boolean {
  return typeof features?.[key] === "boolean" ? features[key] : fallback;
}

export function getReadEntitlement(
  session: ReadAuthSession | null,
): ReadEntitlement {
  if (!session) return DEFAULT_READ_ENTITLEMENT;

  const policy = session.user.readPolicy;
  if (policy) {
    const normalizedTier = normalizePlanName(policy.tier);
    const normalizedPlan = normalizePlanName(policy.plan);
    const isPaid = normalizedTier === "paid" || normalizedPlan !== "free";
    const tiers = policy.voiceAccess.allowedTiers.length > 0
      ? policy.voiceAccess.allowedTiers
      : [policy.voiceAccess.defaultTier];

    return {
      isPaid,
      plan: normalizedPlan === "creator"
        ? "creator"
        : normalizedPlan === "reader"
          ? "reader"
          : isPaid
            ? "paid"
            : "free",
      canExportAudio: readFeature(policy.features, "canExportAudio", isPaid),
      canRecord: readFeature(policy.features, "canRecord", false),
      canTranscribe: readFeature(policy.features, "canTranscribe", false),
      canTranslate: readFeature(policy.features, "canTranslate", true),
      allowedVoiceTiers: tiers,
    };
  }

  const readPlan = normalizePlanName(session.user.readPlan ?? session.user.plan);
  const sharedPlan = normalizePlanName(session.user.plan);

  if (hasFullAccess(readPlan) || hasFullAccess(sharedPlan)) {
    return {
      isPaid: true,
      plan: "full_access",
      canExportAudio: true,
      canRecord: true,
      canTranscribe: true,
      canTranslate: true,
      allowedVoiceTiers: [
        "standard_reader",
        "standard_reader_plus",
        "natural_reader",
        "premium_studio",
      ],
    };
  }

  if (hasAppOnlyAccess(readPlan)) {
    return {
      isPaid: true,
      plan: "app",
      canExportAudio: true,
      canRecord: false,
      canTranscribe: false,
      canTranslate: true,
      allowedVoiceTiers: ["standard_reader", "standard_reader_plus"],
    };
  }

  return DEFAULT_READ_ENTITLEMENT;
}

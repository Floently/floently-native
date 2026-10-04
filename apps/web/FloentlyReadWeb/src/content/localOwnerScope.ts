export function requireLocalReadOwnerId(ownerId: string): string {
  const normalized = ownerId.trim();
  if (!normalized) {
    throw new Error("Authenticated Read owner is required for local storage.");
  }
  return normalized;
}

export function localReadOwnerScope(ownerId: string): string {
  return encodeURIComponent(requireLocalReadOwnerId(ownerId));
}

export function accountScopedLocalName(
  prefix: string,
  ownerId: string,
): string {
  return `${prefix}:${localReadOwnerScope(ownerId)}`;
}

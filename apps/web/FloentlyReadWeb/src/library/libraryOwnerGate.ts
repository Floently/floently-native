export type LibraryOwnerToken = {
  ownerId: string;
  ownerEpoch: number;
};

export type LibraryRefreshToken = LibraryOwnerToken & {
  refreshSequence: number;
};

function normalizeOwner(ownerId: string | null | undefined): string | null {
  const normalized = ownerId?.trim() ?? "";
  return normalized || null;
}

export class LibraryOwnerGate {
  private ownerId: string | null;
  private ownerEpoch = 0;
  private refreshSequence = 0;

  constructor(ownerId: string | null | undefined) {
    this.ownerId = normalizeOwner(ownerId);
  }

  setOwner(ownerId: string | null | undefined): void {
    const normalized = normalizeOwner(ownerId);
    if (normalized === this.ownerId) return;

    this.ownerId = normalized;
    this.ownerEpoch += 1;
    this.refreshSequence += 1;
  }

  capture(ownerId: string | null | undefined): LibraryOwnerToken | null {
    const normalized = normalizeOwner(ownerId);
    if (!normalized || normalized !== this.ownerId) return null;

    return {
      ownerId: normalized,
      ownerEpoch: this.ownerEpoch,
    };
  }

  beginRefresh(
    ownerId: string | null | undefined,
  ): LibraryRefreshToken | null {
    const owner = this.capture(ownerId);
    if (!owner) return null;

    this.refreshSequence += 1;
    return {
      ...owner,
      refreshSequence: this.refreshSequence,
    };
  }

  invalidateRefresh(ownerId: string | null | undefined): void {
    if (this.capture(ownerId)) {
      this.refreshSequence += 1;
    }
  }

  isOwnerCurrent(token: LibraryOwnerToken): boolean {
    return (
      this.ownerId === token.ownerId
      && this.ownerEpoch === token.ownerEpoch
    );
  }

  isRefreshCurrent(token: LibraryRefreshToken): boolean {
    return (
      this.isOwnerCurrent(token)
      && this.refreshSequence === token.refreshSequence
    );
  }
}

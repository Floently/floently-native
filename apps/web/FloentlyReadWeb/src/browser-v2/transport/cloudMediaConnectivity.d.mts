/** true = newly decoded video, false = actual media loss, null = informational. */
export const CLOUD_MEDIA_LOST: readonly string[];
export function cloudMediaConnectivity(status: string): boolean | null;

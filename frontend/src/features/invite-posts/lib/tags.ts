/** Mirrors the backend's invite-post tag rules (InvitePost.MAX_TAGS / MAX_TAG_LENGTH). */
export const MAX_TAGS = 5;
export const MAX_TAG_LENGTH = 30;

/** One-tap topics offered on the new-post page. */
export const SUGGESTED_TAGS = [
  'walk',
  'coffee',
  'food',
  'study',
  'code',
  'fitness',
  'music',
  'games',
] as const;

/**
 * Turns whatever someone typed ("#Board Games", " study ") into the shape the backend stores:
 * lowercase, no leading '#', words joined with '-', only letters/numbers/hyphens/underscores.
 * Returns '' when nothing usable is left.
 */
export function normalizeTag(raw: string): string {
  return raw
    .trim()
    .replace(/^#+/, '')
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\p{L}\p{N}_-]/gu, '')
    .replace(/^[-_]+/, '')
    .slice(0, MAX_TAG_LENGTH);
}

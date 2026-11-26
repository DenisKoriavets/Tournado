// US-05 checklist 2: an unauthenticated player opens /join/{token}, logs in, and must land back on the invite.
// Login returns to "/" (Steam redirect or a magic link opened in a NEW tab), so sessionStorage is not enough:
// the token is kept in localStorage with a short TTL. Storage may be unavailable (private mode), so every
// access is wrapped in try/catch and the feature silently degrades.

const STORAGE_KEY = 'tournado.pendingInvite'
export const PENDING_INVITE_TTL_MS = 30 * 60 * 1000

interface StoredInvite {
  token: string
  savedAt: number
}

export function savePendingInvite(token: string, now: number = Date.now()): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, savedAt: now } satisfies StoredInvite))
  } catch {
    // storage unavailable — the user can reopen the invite link manually
  }
}

/** Returns the saved token if it is younger than the TTL; expired or malformed entries are removed. */
export function readPendingInvite(now: number = Date.now()): string | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<StoredInvite>
    if (typeof parsed.token !== 'string' || typeof parsed.savedAt !== 'number' || now - parsed.savedAt > PENDING_INVITE_TTL_MS) {
      window.localStorage.removeItem(STORAGE_KEY)
      return null
    }
    return parsed.token
  } catch {
    return null
  }
}

export function clearPendingInvite(): void {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }
}

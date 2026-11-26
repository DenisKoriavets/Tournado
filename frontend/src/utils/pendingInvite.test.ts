import { afterEach, describe, expect, it, vi } from 'vitest'
import { PENDING_INVITE_TTL_MS, clearPendingInvite, readPendingInvite, savePendingInvite } from './pendingInvite'

// US-05 checklist 2: the invite survives the login redirect (also a magic link opened in a new tab).
describe('pending invite storage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    window.localStorage.clear()
  })

  it('returns the saved token within the TTL and forgets it after clear', () => {
    savePendingInvite('inv_abc', 1_000)

    expect(readPendingInvite(1_000 + PENDING_INVITE_TTL_MS)).toBe('inv_abc')
    clearPendingInvite()
    expect(readPendingInvite(1_000)).toBeNull()
  })

  it('drops an expired token', () => {
    savePendingInvite('inv_old', 1_000)

    expect(readPendingInvite(1_000 + PENDING_INVITE_TTL_MS + 1)).toBeNull()
    expect(window.localStorage.length).toBe(0)
  })

  it('degrades silently when storage is unavailable (private mode)', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })

    expect(() => savePendingInvite('inv_abc')).not.toThrow()
    expect(readPendingInvite()).toBeNull()
  })
})

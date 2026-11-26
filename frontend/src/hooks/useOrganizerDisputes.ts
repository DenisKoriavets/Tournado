import { useCallback, useEffect, useState } from 'react'
import { getTournamentDisputes } from '../api/disputes'
import { ApiError } from '../api/http'
import type { TournamentDisputes } from '../types/disputes'
import type { ProblemDetail } from '../types/tournament'

export const DISPUTES_POLL_MS = 10000

export interface OrganizerDisputesState {
  data: TournamentDisputes | null
  error: ProblemDetail | null
  /** True when the API refused access (not the organizer) — the tab must stay hidden. */
  forbidden: boolean
  reload: () => void
}

/**
 * US-31: loads the organizer's dispute list as soon as the drawer knows the viewer is the organizer
 * (so the counter is visible before the tab is opened) and refreshes it every DISPUTES_POLL_MS.
 */
export function useOrganizerDisputes(tournamentId: string, enabled: boolean): OrganizerDisputesState {
  const [data, setData] = useState<TournamentDisputes | null>(null)
  const [error, setError] = useState<ProblemDetail | null>(null)
  const [forbidden, setForbidden] = useState(false)
  const [nonce, setNonce] = useState(0)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    const load = async () => {
      try {
        const result = await getTournamentDisputes(tournamentId)
        if (cancelled) return
        setData(result)
        setError(null)
      } catch (err: unknown) {
        if (cancelled) return
        if (err instanceof ApiError && (err.problem.status === 403 || err.problem.status === 401)) {
          setForbidden(true)
          return
        }
        setError(
          err instanceof ApiError
            ? err.problem
            : { title: 'Помилка', status: 0, detail: 'Не вдалося завантажити список спорів.' }
        )
      }
    }
    void load()
    const interval = window.setInterval(() => void load(), DISPUTES_POLL_MS)
    return () => {
      cancelled = true
      window.clearInterval(interval)
    }
  }, [tournamentId, enabled, nonce])

  const reload = useCallback(() => setNonce((n) => n + 1), [])
  return { data, error, forbidden, reload }
}

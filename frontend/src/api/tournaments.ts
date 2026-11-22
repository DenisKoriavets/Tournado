import type { CreateTournamentPayload, Tournament } from '../types/tournament'
import { ApiError, requestJson } from './http'

export { ApiError }

const BASE_URL = '/api/v1/tournaments'

/**
 * Creates a new tournament according to US-07..US-10.
 * POST /api/v1/tournaments
 */
export function createTournament(payload: CreateTournamentPayload): Promise<Tournament> {
  return requestJson<Tournament>(BASE_URL, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

/**
 * Retrieves the list of tournaments.
 * GET /api/v1/tournaments
 */
export function getTournaments(): Promise<Tournament[]> {
  return requestJson<Tournament[]>(BASE_URL, {
    method: 'GET'
  })
}

/**
 * Retrieves full details for a tournament by its ID or public slug.
 * GET /api/v1/tournaments/{id}
 */
export function getTournamentById(id: string): Promise<Tournament> {
  return requestJson<Tournament>(`${BASE_URL}/${encodeURIComponent(id)}`, {
    method: 'GET'
  })
}

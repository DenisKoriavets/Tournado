import type {
  DisputeCard,
  OpenDisputeResult,
  ResolveDisputePayload,
  ResolveDisputeResult,
  TournamentDisputes
} from '../types/disputes'
import { requestJson } from './http'

/** US-30: captain opens a dispute with a scoreboard screenshot (multipart part name: `screenshot`). */
export function openDispute(matchId: string, screenshot: File): Promise<OpenDisputeResult> {
  const body = new FormData()
  body.append('screenshot', screenshot)
  return requestJson<OpenDisputeResult>(`/api/v1/matches/${encodeURIComponent(matchId)}/disputes`, {
    method: 'POST',
    body
  })
}

/** US-31: organizer dashboard list (organizer of the tournament only). */
export function getTournamentDisputes(tournamentId: string): Promise<TournamentDisputes> {
  return requestJson<TournamentDisputes>(
    `/api/v1/tournaments/${encodeURIComponent(tournamentId)}/disputes`,
    { method: 'GET' }
  )
}

/** US-32 / US-33: dispute card with AI hint and nickname check. */
export function getDispute(disputeId: string): Promise<DisputeCard> {
  return requestJson<DisputeCard>(`/api/v1/disputes/${encodeURIComponent(disputeId)}`, {
    method: 'GET'
  })
}

/** US-34: organizer's final ruling — approve the AI hint or enter the score manually. */
export function resolveDispute(
  disputeId: string,
  payload: ResolveDisputePayload
): Promise<ResolveDisputeResult> {
  return requestJson<ResolveDisputeResult>(
    `/api/v1/disputes/${encodeURIComponent(disputeId)}/resolve`,
    { method: 'POST', body: JSON.stringify(payload) }
  )
}

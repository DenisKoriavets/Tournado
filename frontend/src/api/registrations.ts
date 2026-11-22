import type {
  LockRostersResponse,
  RegisterTeamResponse,
  TournamentRegistrations
} from '../types/registrations'
import { requestJson } from './http'

export function registerTeamForTournament(
  tournamentId: string,
  teamId: string
): Promise<RegisterTeamResponse> {
  return requestJson<RegisterTeamResponse>(
    `/api/v1/tournaments/${encodeURIComponent(tournamentId)}/register`,
    {
      method: 'POST',
      body: JSON.stringify({ teamId })
    }
  )
}

export function getTournamentRegistrations(tournamentId: string): Promise<TournamentRegistrations> {
  return requestJson<TournamentRegistrations>(
    `/api/v1/tournaments/${encodeURIComponent(tournamentId)}/registrations`,
    {
      method: 'GET'
    }
  )
}

export function removeTeamRegistration(tournamentId: string, teamId: string): Promise<void> {
  return requestJson<void>(
    `/api/v1/tournaments/${encodeURIComponent(tournamentId)}/registrations/${encodeURIComponent(teamId)}`,
    {
      method: 'DELETE'
    }
  )
}

export function lockTournamentRosters(tournamentId: string): Promise<LockRostersResponse> {
  return requestJson<LockRostersResponse>(
    `/api/v1/tournaments/${encodeURIComponent(tournamentId)}/lock-rosters`,
    {
      method: 'POST'
    }
  )
}

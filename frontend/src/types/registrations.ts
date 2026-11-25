import type { Discipline } from './tournament'

export type RegistrationStatus = 'REGISTERED' | 'CHECKED_IN' | 'EXCLUDED'

export interface RegisteredTeamItem {
  registrationId: string
  teamId: string
  teamName: string
  discipline: Discipline
  status: RegistrationStatus
  memberCount: number
  registeredAt: string
  rosterLocked: boolean
}

export interface TournamentRegistrations {
  tournamentId: string
  organizerId: string
  slotLimit: number
  registeredCount: number
  rostersLocked: boolean
  teams: RegisteredTeamItem[]
}

export interface RegisterTeamResponse {
  registrationId: string
  tournamentId: string
  teamId: string
  status: RegistrationStatus
  registeredAt: string
}

export interface LockRostersResponse {
  status: string
  totalSnapshottedPlayers: number
}

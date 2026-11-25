import type { Discipline } from './tournament'

export interface TeamSummary {
  id: string
  name: string
  discipline: Discipline
  captainId: string
  memberCount: number
  maxMembers: number
  isFull: boolean
}

export interface TeamMember {
  userId: string
  displayName: string
  isCaptain: boolean
  gameIdentifier: string | null
}

export interface TeamDetails extends TeamSummary {
  members: TeamMember[]
}

export interface TeamInvite {
  token: string
  inviteUrl: string
  expiresAt: string
}

export interface TeamInvitePreview {
  token: string
  teamId: string
  teamName: string
  discipline: Discipline
  memberCount: number
  maxMembers: number
  isFull: boolean
}

export interface CreateTeamPayload {
  name: string
  discipline: Discipline
}

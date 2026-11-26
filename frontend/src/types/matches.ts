import type { Discipline } from './tournament'

export type MatchStatus = 'PENDING' | 'READY_CHECK' | 'PICK_BAN' | 'IN_PROGRESS' | 'COMPLETED' | 'DISPUTED'

export interface MatchTeamState {
  id: string
  name: string | null
  isReady: boolean
}

export interface BannedMap {
  mapName: string
  bannedByTeamId: string
  order: number
}

export interface MatchCredentials {
  discipline: Discipline
  cs2ConnectString: string | null
  brawlStarsCode: string | null
  chessGameUrl: string | null
  decidedMap: string | null
}

export interface MatchRoom {
  matchId: string
  status: MatchStatus
  discipline: Discipline
  readyDeadlineAt: string | null
  banDeadlineAt: string | null
  team1: MatchTeamState
  team2: MatchTeamState
  currentBanTurnTeamId: string | null
  bannedMaps: BannedMap[]
  remainingMaps: string[]
  decidedMap: string | null
  credentials: MatchCredentials | null
}

export interface ReadyState {
  isReady: boolean
}

// US-27 / US-28: POST /api/v1/matches/{id}/scores response (api-contracts.md §7.1)
export interface SubmitScoreResult {
  matchStatus: MatchStatus
  winnerTeamId: string | null
  bothSubmitted: boolean
  scoresMatched: boolean
}

// Dispute types mirror docs/contracts/api-contracts.md Epic 8 (§8.1–§8.4) and the core DTO records.
import type { MatchStatus } from './matches'

export type DisputeStatus = 'OPEN' | 'RESOLVED'
export type AiAnalysisStatus = 'PENDING' | 'SUCCESS' | 'UNRECOGNIZED' | 'FAILED'
export type DisputeDecision = 'AI_SUGGESTION' | 'MANUAL_SCORE'

export interface DisputeTeam {
  id: string
  name: string | null
}

export interface OpenDisputeResult {
  disputeId: string
  matchId: string
  status: DisputeStatus
  screenshotUrl: string
  aiAnalysisStatus: AiAnalysisStatus
}

export interface DisputeSummary {
  disputeId: string
  matchId: string
  roundNumber: number
  matchOrder: number
  status: DisputeStatus
  team1: DisputeTeam
  team2: DisputeTeam
  openedByTeamId: string
  aiAnalysisStatus: AiAnalysisStatus
  aiRecognizedScoreTeam1: number | null
  aiRecognizedScoreTeam2: number | null
  /** 0..1 */
  aiConfidence: number | null
  allRosterNicknamesFound: boolean | null
  createdAt: string
  resolvedAt: string | null
}

export interface TournamentDisputes {
  tournamentId: string
  openDisputesCount: number
  disputes: DisputeSummary[]
}

export interface SubmittedScore {
  teamId: string
  team1Score: number
  team2Score: number
  submittedAt: string
}

export interface NicknameCheck {
  nickname: string
  sideTeamId: string | null
  inRoster: boolean
  matchedTeamId: string | null
  matchedRosterName: string | null
}

export interface AiAnalysis {
  status: AiAnalysisStatus
  recognizedScoreTeam1: number | null
  recognizedScoreTeam2: number | null
  /** 0..1 */
  confidence: number | null
  allRosterNicknamesFound: boolean
  detectedNicknames: string[]
  nicknameChecks: NicknameCheck[]
  analyzedAt: string | null
}

export interface DisputeResolution {
  decision: DisputeDecision
  winnerTeamId: string
  team1Score: number | null
  team2Score: number | null
  resolutionNotes: string | null
  resolvedByUserId: string
  resolvedAt: string
}

export interface DisputeCard {
  disputeId: string
  matchId: string
  tournamentId: string
  status: DisputeStatus
  screenshotUrl: string
  roundNumber: number
  matchOrder: number
  team1: DisputeTeam
  team2: DisputeTeam
  openedByTeamId: string
  submittedScores: SubmittedScore[]
  aiAnalysisStatus: AiAnalysisStatus
  aiAnalysis: AiAnalysis | null
  canApproveAiSuggestion: boolean
  resolution: DisputeResolution | null
  createdAt: string
}

export interface ResolveDisputePayload {
  decision: DisputeDecision
  team1Score?: number
  team2Score?: number
  resolutionNotes?: string
}

export interface ResolveDisputeResult {
  disputeId: string
  status: DisputeStatus
  matchId: string
  matchStatus: MatchStatus
  winnerTeamId: string
  team1Score: number
  team2Score: number
  decision: DisputeDecision
  resolvedAt: string
}

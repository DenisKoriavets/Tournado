// Client-side mirror of the score rules (US-27 AC2/AC3, US-34 AC4; backend SubmitScoreRequest /
// ResolveDisputeRequest). The backend stays authoritative; this only gives instant feedback.

export const MAX_SCORE = 999

export interface ScorePairErrors {
  team1?: string
  team2?: string
  form?: string
}

export interface ParsedScorePair {
  team1Score: number
  team2Score: number
}

const INTEGER_PATTERN = /^\d+$/

function validateSingle(raw: string): string | undefined {
  const value = raw.trim()
  if (value === '') return 'Вкажіть рахунок'
  if (!INTEGER_PATTERN.test(value)) return "Лише ціле невід'ємне число"
  if (Number(value) > MAX_SCORE) return `Не більше ${MAX_SCORE}`
  return undefined
}

/** Returns field errors, or null when both values are valid non-negative integers with a winner. */
export function validateScorePair(team1Raw: string, team2Raw: string): ScorePairErrors | null {
  const errors: ScorePairErrors = {}
  const team1Error = validateSingle(team1Raw)
  const team2Error = validateSingle(team2Raw)
  if (team1Error) errors.team1 = team1Error
  if (team2Error) errors.team2 = team2Error
  if (!team1Error && !team2Error && Number(team1Raw.trim()) === Number(team2Raw.trim())) {
    errors.form = 'Нічия неможлива — має бути переможець'
  }
  return Object.keys(errors).length > 0 ? errors : null
}

export function parseScorePair(team1Raw: string, team2Raw: string): ParsedScorePair {
  return { team1Score: Number(team1Raw.trim()), team2Score: Number(team2Raw.trim()) }
}

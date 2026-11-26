import React, { useState } from 'react'
import { ApiError } from '../api/http'
import { submitMatchScore } from '../api/matches'
import type { ProblemDetail } from '../types/tournament'
import { parseScorePair, validateScorePair, type ScorePairErrors } from '../utils/score'
import { AlertCircleIcon, CheckIcon, ClockIcon, ShieldIcon } from './icons/Icons'

export interface ScoreEntryFormProps {
  matchId: string
  team1Name: string
  team2Name: string
  team1Id: string
  team2Id: string
  /** Called when both captains agreed and the match is closed, so the room can reload. */
  onMatchCompleted: () => void
  /** Called when the match is no longer accepting scores (e.g. already disputed), so the room can reload. */
  onRoomOutdated: () => void
  onOpenDispute: () => void
}

type Phase = 'form' | 'waiting' | 'mismatch' | 'completed'

/**
 * US-27: captain score entry. Opponent's submitted values are never shown (US-27 AC5).
 * B1 fallback: the room API does not expose the own submission, so the phase lives in component state
 * and a 409 SCORE_ALREADY_SUBMITTED switches to the waiting state.
 */
export const ScoreEntryForm: React.FC<ScoreEntryFormProps> = ({
  matchId,
  team1Name,
  team2Name,
  team1Id,
  team2Id,
  onMatchCompleted,
  onRoomOutdated,
  onOpenDispute
}) => {
  const [team1Raw, setTeam1Raw] = useState('')
  const [team2Raw, setTeam2Raw] = useState('')
  const [errors, setErrors] = useState<ScorePairErrors | null>(null)
  const [serverError, setServerError] = useState<ProblemDetail | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [phase, setPhase] = useState<Phase>('form')
  const [winnerName, setWinnerName] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setServerError(null)
    const validation = validateScorePair(team1Raw, team2Raw)
    setErrors(validation)
    if (validation) return

    const { team1Score, team2Score } = parseScorePair(team1Raw, team2Raw)
    setIsSubmitting(true)
    try {
      const result = await submitMatchScore(matchId, team1Score, team2Score)
      if (result.matchStatus === 'COMPLETED') {
        setWinnerName(
          result.winnerTeamId === team1Id ? team1Name : result.winnerTeamId === team2Id ? team2Name : null
        )
        setPhase('completed')
        onMatchCompleted()
      } else if (result.bothSubmitted && !result.scoresMatched) {
        setPhase('mismatch')
      } else {
        setPhase('waiting')
      }
    } catch (err: unknown) {
      if (!(err instanceof ApiError)) {
        setServerError({ title: 'Помилка', status: 0, detail: 'Не вдалося надіслати рахунок.' })
        return
      }
      switch (err.problem.code) {
        case 'SCORE_ALREADY_SUBMITTED':
          setPhase('waiting')
          break
        case 'MATCH_NOT_IN_PROGRESS':
        case 'SCORE_ENTRY_NOT_AVAILABLE':
        case 'ONLY_CAPTAIN_ALLOWED':
          onRoomOutdated()
          setServerError(err.problem)
          break
        default:
          setServerError(err.problem)
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const disputeButton = (
    <button
      type="button"
      onClick={onOpenDispute}
      className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold transition flex items-center gap-1.5 cursor-pointer"
    >
      <ShieldIcon className="w-3.5 h-3.5" />
      <span>Відкрити спір</span>
    </button>
  )

  if (phase === 'completed') {
    return (
      <div role="status" className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-200 flex items-center gap-2">
        <CheckIcon className="w-4 h-4 text-emerald-400" />
        <span>
          Рахунки збіглися — матч завершено.{winnerName ? ` Переможець: ${winnerName}` : ''}
        </span>
      </div>
    )
  }

  if (phase === 'mismatch') {
    return (
      <div role="status" className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 space-y-3">
        <div className="flex items-start gap-2 text-xs text-rose-200">
          <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
          <span>
            Рахунки не збігаються. Матч не закрито й нікого не просунуто. Відкрийте спір і додайте
            скріншот табло — організатор ухвалить рішення.
          </span>
        </div>
        {disputeButton}
      </div>
    )
  }

  if (phase === 'waiting') {
    return (
      <div role="status" className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-3">
        <div className="flex items-center gap-2 text-xs text-slate-300">
          <ClockIcon className="w-4 h-4 text-orange-400" />
          <span>Рахунок збережено. Очікується підтвердження суперником.</span>
        </div>
        <div className="flex items-center justify-between gap-3 flex-wrap">
          <span className="text-[11px] text-slate-500">
            Суперник не підтверджує результат? Можна відкрити спір зі скріншотом.
          </span>
          {disputeButton}
        </div>
      </div>
    )
  }

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      aria-label="Внесення рахунку матчу"
      className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-3"
    >
      <div className="text-xs font-mono uppercase tracking-wider text-slate-400">Рахунок матчу</div>
      <div className="grid grid-cols-2 gap-3">
        <ScoreInput
          id="score-team1"
          label={team1Name}
          value={team1Raw}
          error={errors?.team1}
          onChange={setTeam1Raw}
        />
        <ScoreInput
          id="score-team2"
          label={team2Name}
          value={team2Raw}
          error={errors?.team2}
          onChange={setTeam2Raw}
        />
      </div>
      {errors?.form && (
        <p role="alert" className="text-xs text-rose-300">
          {errors.form}
        </p>
      )}
      {serverError && (
        <p role="alert" className="text-xs text-rose-300">
          {serverError.detail}
        </p>
      )}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <button
          type="submit"
          disabled={isSubmitting}
          className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition cursor-pointer"
        >
          {isSubmitting ? 'Надсилаємо...' : 'Надіслати рахунок'}
        </button>
        <button
          type="button"
          onClick={onOpenDispute}
          className="text-[11px] text-slate-400 hover:text-rose-300 underline underline-offset-2 cursor-pointer"
        >
          Суперник уже вніс неправильний рахунок? Відкрити спір
        </button>
      </div>
    </form>
  )
}

const ScoreInput: React.FC<{
  id: string
  label: string
  value: string
  error?: string
  onChange: (value: string) => void
}> = ({ id, label, value, error, onChange }) => (
  <div className="space-y-1">
    <label htmlFor={id} className="block text-xs font-semibold text-white truncate">
      {label}
    </label>
    <input
      id={id}
      type="text"
      inputMode="numeric"
      autoComplete="off"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-invalid={Boolean(error)}
      aria-describedby={error ? `${id}-error` : undefined}
      className={`w-full px-3 py-2 rounded-lg bg-[#121620] border text-sm text-white font-mono focus:outline-none focus:border-orange-500 ${
        error ? 'border-rose-500/60' : 'border-[#1E2536]'
      }`}
    />
    {error && (
      <p id={`${id}-error`} className="text-[11px] text-rose-300">
        {error}
      </p>
    )}
  </div>
)

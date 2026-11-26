import React, { useEffect, useState } from 'react'
import { getDispute, resolveDispute } from '../api/disputes'
import { ApiError } from '../api/http'
import type { DisputeCard, DisputeTeam, ResolveDisputePayload } from '../types/disputes'
import type { ProblemDetail } from '../types/tournament'
import { formatDateTime } from '../utils/date'
import { parseScorePair, validateScorePair, type ScorePairErrors } from '../utils/score'
import { AlertCircleIcon, CheckIcon, CloseIcon, ShieldIcon, SparklesIcon } from './icons/Icons'

export const PENDING_POLL_MS = 3000
const MAX_NOTES = 2000

export interface DisputeCardModalProps {
  disputeId: string
  onClose: () => void
  /** Called after a successful ruling so the dashboard list can refresh. */
  onResolved: () => void
}

const RESOLVE_ERROR_MESSAGES: Record<string, string> = {
  AI_SUGGESTION_UNAVAILABLE: 'ШІ не розпізнав рахунок — введіть підсумковий рахунок вручну.',
  AUTH_FORBIDDEN: 'Лише організатор цього турніру може закрити спір.',
  DISPUTE_ALREADY_RESOLVED: 'Спір уже закрито. Картку оновлено.',
  DISPUTE_NOT_FOUND: 'Спір не знайдено.'
}

/**
 * US-32 (AI score hint), US-33 (nickname check against locked rosters), US-34 (organizer's final ruling).
 * The AI only suggests; nothing is resolved until the organizer confirms an action.
 */
export const DisputeCardModal: React.FC<DisputeCardModalProps> = ({ disputeId, onClose, onResolved }) => {
  const [card, setCard] = useState<DisputeCard | null>(null)
  const [loadError, setLoadError] = useState<ProblemDetail | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [reloadNonce, setReloadNonce] = useState(0)

  const [team1Raw, setTeam1Raw] = useState('')
  const [team2Raw, setTeam2Raw] = useState('')
  const [notes, setNotes] = useState('')
  const [scoreErrors, setScoreErrors] = useState<ScorePairErrors | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [pendingAction, setPendingAction] = useState<ResolveDisputePayload | null>(null)
  const [isResolving, setIsResolving] = useState(false)
  const [resolvedMessage, setResolvedMessage] = useState<string | null>(null)

  // Load the card; while the AI analysis is PENDING, poll every PENDING_POLL_MS and stop once it finishes.
  useEffect(() => {
    let cancelled = false
    let timer: number | undefined

    const load = async () => {
      try {
        const data = await getDispute(disputeId)
        if (cancelled) return
        setCard(data)
        setLoadError(null)
        if (data.aiAnalysisStatus === 'PENDING' && data.status === 'OPEN') {
          timer = window.setTimeout(() => void load(), PENDING_POLL_MS)
        }
      } catch (err: unknown) {
        if (cancelled) return
        setLoadError(
          err instanceof ApiError
            ? err.problem
            : { title: 'Помилка завантаження', status: 0, detail: 'Не вдалося завантажити картку спору.' }
        )
      } finally {
        if (!cancelled) setIsLoading(false)
      }
    }

    void load()
    return () => {
      cancelled = true
      if (timer !== undefined) window.clearTimeout(timer)
    }
  }, [disputeId, reloadNonce])

  const teamName = (team: DisputeTeam | undefined) => team?.name ?? 'Команда'
  const nameById = (teamId: string | null | undefined) => {
    if (!card || !teamId) return null
    if (teamId === card.team1.id) return teamName(card.team1)
    if (teamId === card.team2.id) return teamName(card.team2)
    return null
  }

  const requestAiApproval = () => {
    setActionError(null)
    setPendingAction({ decision: 'AI_SUGGESTION' })
  }

  const requestManualRuling = (e: React.FormEvent) => {
    e.preventDefault()
    setActionError(null)
    const validation = validateScorePair(team1Raw, team2Raw)
    setScoreErrors(validation)
    if (validation) return
    const { team1Score, team2Score } = parseScorePair(team1Raw, team2Raw)
    const trimmedNotes = notes.trim()
    setPendingAction({
      decision: 'MANUAL_SCORE',
      team1Score,
      team2Score,
      ...(trimmedNotes ? { resolutionNotes: trimmedNotes } : {})
    })
  }

  const confirmPendingAction = async () => {
    if (!pendingAction) return
    setIsResolving(true)
    try {
      const result = await resolveDispute(disputeId, pendingAction)
      setResolvedMessage(
        `Спір закрито з рахунком ${result.team1Score}:${result.team2Score}. Переможець: ${nameById(result.winnerTeamId) ?? '—'}.`
      )
      setPendingAction(null)
      setReloadNonce((n) => n + 1)
      onResolved()
    } catch (err: unknown) {
      setPendingAction(null)
      if (err instanceof ApiError) {
        const code = err.problem.code ?? ''
        setActionError(RESOLVE_ERROR_MESSAGES[code] ?? err.problem.detail)
        if (code === 'DISPUTE_ALREADY_RESOLVED') setReloadNonce((n) => n + 1)
      } else {
        setActionError('Не вдалося закрити спір. Спробуйте ще раз.')
      }
    } finally {
      setIsResolving(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center p-4 overflow-y-auto bg-black/80 backdrop-blur-sm animate-fadeIn"
      role="dialog"
      aria-modal="true"
      aria-labelledby="dispute-card-title"
    >
      <div className="absolute inset-0" onClick={onClose} />
      <div className="relative w-full max-w-3xl bg-[#121620] border border-[#1E2536] rounded-2xl shadow-2xl overflow-hidden my-8 animate-scaleIn">
        <div className="p-5 border-b border-[#1E2536] bg-[#0E121A] flex items-center justify-between gap-3">
          <div>
            <h2 id="dispute-card-title" className="text-base font-extrabold text-white flex items-center gap-2">
              <ShieldIcon className="w-4 h-4 text-rose-400" />
              Картка спору
            </h2>
            {card && (
              <p className="text-xs text-slate-400 mt-0.5">
                Раунд {card.roundNumber} · Матч {card.matchOrder} · {teamName(card.team1)} vs {teamName(card.team2)}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition"
            aria-label="Закрити картку спору"
          >
            <CloseIcon className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5 space-y-5 max-h-[78vh] overflow-y-auto">
          {isLoading && <div className="h-40 rounded-xl bg-[#0B0E14] animate-pulse" />}

          {!isLoading && loadError && !card && (
            <div role="alert" className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200">
              {loadError.code === 'AUTH_FORBIDDEN'
                ? 'Картку спору може переглядати лише організатор цього турніру.'
                : loadError.detail}
            </div>
          )}

          {card && (
            <>
              {resolvedMessage && (
                <div role="status" className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-200 flex items-center gap-2">
                  <CheckIcon className="w-4 h-4 text-emerald-400" />
                  <span>{resolvedMessage}</span>
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <h3 className="text-[11px] font-mono uppercase tracking-wider text-slate-400">Скріншот табло</h3>
                  <a href={card.screenshotUrl} target="_blank" rel="noreferrer" className="block">
                    <img
                      src={card.screenshotUrl}
                      alt="Скріншот табло, завантажений капітаном"
                      className="w-full max-h-64 object-contain rounded-xl border border-[#1E2536] bg-black"
                    />
                  </a>
                </div>

                <div className="space-y-2">
                  <h3 className="text-[11px] font-mono uppercase tracking-wider text-slate-400">Рахунки від капітанів</h3>
                  {card.submittedScores.length === 0 ? (
                    <p className="text-xs text-slate-500">Капітани ще не внесли рахунок.</p>
                  ) : (
                    <ul className="space-y-1.5">
                      {card.submittedScores.map((s) => (
                        <li key={s.teamId} className="p-2.5 rounded-lg bg-[#0B0E14] border border-[#1E2536] text-xs text-slate-200 flex justify-between gap-2">
                          <span>Капітан «{nameById(s.teamId) ?? 'Команда'}»</span>
                          <span className="font-mono font-bold">
                            {s.team1Score}:{s.team2Score}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                  <p className="text-[11px] text-slate-500">
                    Рахунок подано як «{teamName(card.team1)} : {teamName(card.team2)}». Спір відкрито{' '}
                    {formatDateTime(card.createdAt)}.
                  </p>
                </div>
              </div>

              <AiHintBlock card={card} nameById={nameById} />
              <NicknameCheckBlock card={card} nameById={nameById} />

              {card.status === 'RESOLVED' && card.resolution && (
                <div className="p-4 rounded-xl bg-[#0B0E14] border border-emerald-900/40 text-xs text-slate-200 space-y-1">
                  <div className="font-bold text-emerald-300">Опрацьовано — рішення організатора остаточне</div>
                  <div>
                    Рахунок: {card.resolution.team1Score ?? '—'}:{card.resolution.team2Score ?? '—'} · Переможець:{' '}
                    {nameById(card.resolution.winnerTeamId) ?? '—'} ·{' '}
                    {card.resolution.decision === 'AI_SUGGESTION' ? 'затверджено підказку ШІ' : 'рахунок введено вручну'}
                  </div>
                  {card.resolution.resolutionNotes && <div className="text-slate-400">{card.resolution.resolutionNotes}</div>}
                </div>
              )}

              {actionError && (
                <div role="alert" className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2">
                  <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                  <span>{actionError}</span>
                </div>
              )}

              {card.status === 'OPEN' && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-3">
                    <h3 className="text-xs font-bold text-white">Рішення в один клік</h3>
                    <button
                      type="button"
                      onClick={requestAiApproval}
                      disabled={!card.canApproveAiSuggestion || isResolving}
                      className="w-full px-4 py-2.5 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 disabled:cursor-not-allowed text-white text-xs font-bold transition cursor-pointer"
                    >
                      Затвердити підказку ШІ
                    </button>
                    {!card.canApproveAiSuggestion && (
                      <p className="text-[11px] text-slate-500">
                        Доступно лише коли ШІ розпізнав рахунок із переможцем.
                      </p>
                    )}
                  </div>

                  <form
                    onSubmit={requestManualRuling}
                    noValidate
                    aria-label="Ручне введення рахунку"
                    className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-3"
                  >
                    <h3 className="text-xs font-bold text-white">Ввести підсумковий рахунок вручну</h3>
                    <div className="grid grid-cols-2 gap-2">
                      <ManualScoreInput id="manual-team1" label={teamName(card.team1)} value={team1Raw} error={scoreErrors?.team1} onChange={setTeam1Raw} />
                      <ManualScoreInput id="manual-team2" label={teamName(card.team2)} value={team2Raw} error={scoreErrors?.team2} onChange={setTeam2Raw} />
                    </div>
                    {scoreErrors?.form && (
                      <p role="alert" className="text-xs text-rose-300">
                        {scoreErrors.form}
                      </p>
                    )}
                    <label htmlFor="manual-notes" className="block text-[11px] text-slate-400">
                      Коментар (необов'язково)
                    </label>
                    <textarea
                      id="manual-notes"
                      value={notes}
                      maxLength={MAX_NOTES}
                      onChange={(e) => setNotes(e.target.value)}
                      rows={2}
                      className="w-full px-3 py-2 rounded-lg bg-[#121620] border border-[#1E2536] text-xs text-white focus:outline-none focus:border-orange-500"
                    />
                    <button
                      type="submit"
                      disabled={isResolving}
                      className="w-full px-4 py-2 rounded-xl bg-slate-700 hover:bg-slate-600 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition cursor-pointer"
                    >
                      Закрити спір із цим рахунком
                    </button>
                  </form>
                </div>
              )}

              {pendingAction && (
                <div role="alertdialog" aria-label="Підтвердження рішення" className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 space-y-3">
                  <p className="text-xs text-amber-100">
                    {pendingAction.decision === 'AI_SUGGESTION'
                      ? 'Затвердити підказку ШІ? Рішення остаточне: матч закриється, переможець пройде далі.'
                      : `Закрити спір із рахунком ${pendingAction.team1Score}:${pendingAction.team2Score}? Рішення остаточне.`}
                  </p>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => void confirmPendingAction()}
                      disabled={isResolving}
                      className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 text-white text-xs font-bold transition cursor-pointer"
                    >
                      {isResolving ? 'Закриваємо...' : 'Підтвердити рішення'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setPendingAction(null)}
                      className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition cursor-pointer"
                    >
                      Скасувати
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}

const AiHintBlock: React.FC<{ card: DisputeCard; nameById: (id: string | null | undefined) => string | null }> = ({
  card,
  nameById
}) => {
  const ai = card.aiAnalysis
  let body: React.ReactNode
  if (card.aiAnalysisStatus === 'PENDING') {
    body = <span className="text-slate-400">ШІ аналізує скріншот…</span>
  } else if (card.aiAnalysisStatus === 'SUCCESS' && ai && ai.recognizedScoreTeam1 !== null && ai.recognizedScoreTeam2 !== null) {
    const winnerId =
      ai.recognizedScoreTeam1 > ai.recognizedScoreTeam2
        ? card.team1.id
        : ai.recognizedScoreTeam2 > ai.recognizedScoreTeam1
          ? card.team2.id
          : null
    const confidence = ai.confidence !== null ? Math.round(ai.confidence * 100) : null
    body = (
      <span className="text-slate-100">
        Розпізнаний рахунок{' '}
        <span className="font-mono font-bold text-orange-300">
          {ai.recognizedScoreTeam1}:{ai.recognizedScoreTeam2}
        </span>
        {winnerId && <> на користь «{nameById(winnerId)}»</>}
        {confidence !== null && <> (впевненість {confidence}%)</>}
      </span>
    )
  } else {
    body = <span className="text-amber-200">Рахунок не розпізнано — введіть підсумковий рахунок вручну.</span>
  }
  return (
    <div className="p-4 rounded-xl bg-[#0B0E14] border border-orange-900/40 space-y-1.5">
      <h3 className="text-[11px] font-mono uppercase tracking-wider text-orange-400 flex items-center gap-1.5">
        <SparklesIcon className="w-3.5 h-3.5" />
        Підказка ШІ
      </h3>
      <p className="text-xs">{body}</p>
      <p className="text-[11px] text-slate-500">ШІ лише підказує — матч закривається тільки після рішення організатора.</p>
    </div>
  )
}

const NicknameCheckBlock: React.FC<{ card: DisputeCard; nameById: (id: string | null | undefined) => string | null }> = ({
  card,
  nameById
}) => {
  const ai = card.aiAnalysis
  if (card.aiAnalysisStatus === 'PENDING' || !ai) return null
  if (ai.detectedNicknames.length === 0) {
    return (
      <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] text-xs text-slate-400">
        Звірка нікнеймів: нікнейми не розпізнано.
      </div>
    )
  }
  const hasRinger = ai.nicknameChecks.some((check) => !check.inRoster)
  return (
    <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-2">
      <h3 className="text-[11px] font-mono uppercase tracking-wider text-slate-400">Звірка нікнеймів із ростерами</h3>
      {hasRinger && (
        <div role="alert" className="p-2.5 rounded-lg bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200">
          Можливий сторонній гравець: частину нікнеймів не знайдено в зафіксованих ростерах.
        </div>
      )}
      <ul className="divide-y divide-[#1E2536]">
        {ai.nicknameChecks.map((check) => (
          <li
            key={`${check.nickname}-${check.sideTeamId ?? 'none'}`}
            className={`py-1.5 flex items-center justify-between gap-2 text-xs ${check.inRoster ? 'text-slate-200' : 'text-rose-200'}`}
          >
            <span className="font-mono">
              {check.nickname}
              {check.sideTeamId && <span className="text-slate-500"> · сторона «{nameById(check.sideTeamId) ?? '—'}»</span>}
            </span>
            <span
              className={`px-2 py-0.5 rounded-md border text-[10px] font-bold ${
                check.inRoster
                  ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30'
                  : 'bg-rose-500/10 text-rose-300 border-rose-500/30'
              }`}
            >
              {check.inRoster ? 'Збіг із ростером' : 'Не знайдено в ростері'}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

const ManualScoreInput: React.FC<{
  id: string
  label: string
  value: string
  error?: string
  onChange: (value: string) => void
}> = ({ id, label, value, error, onChange }) => (
  <div className="space-y-1">
    <label htmlFor={id} className="block text-[11px] font-semibold text-white truncate">
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

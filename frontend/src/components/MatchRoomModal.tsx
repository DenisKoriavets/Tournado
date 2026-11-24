import React, { useCallback, useEffect, useState } from 'react'
import { banMatchMap, getMatchRoom, markMatchReady } from '../api/matches'
import { ApiError } from '../api/http'
import { getTeamById } from '../api/teams'
import { useAuth } from '../context/AuthContext'
import type { MatchRoom } from '../types/matches'
import type { TeamDetails } from '../types/teams'
import { DISCIPLINE_CONFIG, type ProblemDetail } from '../types/tournament'
import { DisciplineLogo } from './DisciplineLogo'
import {
  AlertCircleIcon,
  CheckIcon,
  ClockIcon,
  CloseIcon,
  CopyIcon,
  CrownIcon,
  GamepadIcon,
  ServerIcon,
  ShieldIcon,
  UsersIcon
} from './icons/Icons'

export interface MatchRoomModalProps {
  matchId: string | null
  onClose: () => void
}

const STATUS_LABELS: Record<MatchRoom['status'], string> = {
  PENDING: 'Очікування кімнати',
  READY_CHECK: 'Ready Check',
  PICK_BAN: 'Вибір/бан карт',
  IN_PROGRESS: 'Матч триває',
  COMPLETED: 'Завершено',
  DISPUTED: 'Суперечка'
}

const REFRESH_INTERVAL_MS = 4000

export const MatchRoomModal: React.FC<MatchRoomModalProps> = ({ matchId, onClose }) => {
  if (!matchId) return null
  return <MatchRoomContent key={matchId} matchId={matchId} onClose={onClose} />
}

const MatchRoomContent: React.FC<{ matchId: string; onClose: () => void }> = ({ matchId, onClose }) => {
  const { user, openAuthModal } = useAuth()
  const [room, setRoom] = useState<MatchRoom | null>(null)
  const [team1Details, setTeam1Details] = useState<TeamDetails | null>(null)
  const [team2Details, setTeam2Details] = useState<TeamDetails | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<ProblemDetail | null>(null)
  const [actionError, setActionError] = useState<ProblemDetail | null>(null)
  const [isSubmittingReady, setIsSubmittingReady] = useState(false)
  const [banningMap, setBanningMap] = useState<string | null>(null)
  const [copiedField, setCopiedField] = useState<string | null>(null)

  const loadRoom = useCallback(
    async (showSpinner: boolean) => {
      if (showSpinner) setIsLoading(true)
      try {
        const data = await getMatchRoom(matchId)
        setRoom(data)
        setLoadError(null)
        const [t1, t2] = await Promise.all([getTeamById(data.team1.id), getTeamById(data.team2.id)])
        setTeam1Details(t1)
        setTeam2Details(t2)
      } catch (err: unknown) {
        if (err instanceof ApiError) {
          if (err.problem.status === 401) {
            openAuthModal('Увійдіть, щоб переглянути Match Room.')
          }
          setLoadError(err.problem)
        } else {
          setLoadError({
            title: 'Помилка завантаження',
            status: 500,
            detail: 'Не вдалося завантажити кімнату матчу.'
          })
        }
      } finally {
        if (showSpinner) setIsLoading(false)
      }
    },
    [matchId, openAuthModal]
  )

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  useEffect(() => {
    loadRoom(true)
    const interval = window.setInterval(() => loadRoom(false), REFRESH_INTERVAL_MS)
    return () => window.clearInterval(interval)
  }, [loadRoom])

  const handleReady = async () => {
    setActionError(null)
    setIsSubmittingReady(true)
    try {
      await markMatchReady(matchId)
      await loadRoom(false)
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setActionError(err.problem)
      }
    } finally {
      setIsSubmittingReady(false)
    }
  }

  const handleBan = async (mapName: string) => {
    setActionError(null)
    setBanningMap(mapName)
    try {
      const updated = await banMatchMap(matchId, mapName)
      setRoom(updated)
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setActionError(err.problem)
      }
    } finally {
      setBanningMap(null)
    }
  }

  const handleCopy = async (field: string, value: string) => {
    try {
      await navigator.clipboard.writeText(value)
      setCopiedField(field)
      setTimeout(() => setCopiedField(null), 2000)
    } catch {
      // clipboard fallback ignored
    }
  }

  if (isLoading) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
        <div className="w-full max-w-3xl h-72 rounded-2xl bg-[#121620] border border-[#1E2536] animate-pulse" />
      </div>
    )
  }

  if (loadError || !room) {
    return (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn"
        role="dialog"
        aria-modal="true"
      >
        <div className="absolute inset-0" onClick={onClose} />
        <div className="relative w-full max-w-md bg-[#121620] border border-[#1E2536] rounded-2xl shadow-2xl p-6 space-y-4 text-center animate-scaleIn">
          <AlertCircleIcon className="w-10 h-10 text-rose-400 mx-auto" />
          <h3 className="text-base font-bold text-white">{loadError?.title ?? 'Match Room недоступна'}</h3>
          <p className="text-xs text-slate-400">{loadError?.detail ?? 'Не вдалося знайти цей матч.'}</p>
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition cursor-pointer"
          >
            Закрити
          </button>
        </div>
      </div>
    )
  }

  const discipline = DISCIPLINE_CONFIG[room.discipline]
  const isCaptain1 = Boolean(user && team1Details && user.id === team1Details.captainId)
  const isCaptain2 = Boolean(user && team2Details && user.id === team2Details.captainId)
  const myTeamId = isCaptain1 ? room.team1.id : isCaptain2 ? room.team2.id : null
  const myTeamReady = isCaptain1 ? room.team1.isReady : isCaptain2 ? room.team2.isReady : null
  const canMarkReady =
    Boolean(myTeamId) &&
    !myTeamReady &&
    (room.status === 'PENDING' || room.status === 'READY_CHECK')
  const isMyBanTurn = room.status === 'PICK_BAN' && myTeamId !== null && myTeamId === room.currentBanTurnTeamId
  const vetoApplies = room.discipline !== 'CHESS'
  const showVetoSection = vetoApplies && (room.status === 'PICK_BAN' || room.bannedMaps.length > 0 || Boolean(room.decidedMap))
  const allMapTiles = [...room.bannedMaps.map((b) => b.mapName), ...room.remainingMaps]
  const teamNameById = (teamId: string | null) =>
    teamId === room.team1.id ? room.team1.name : teamId === room.team2.id ? room.team2.name : null
  const cs2ConnectString = room.credentials?.cs2ConnectString ?? null
  const brawlStarsCode = room.credentials?.brawlStarsCode ?? null
  const chessGameUrl = room.credentials?.chessGameUrl ?? null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto bg-black/75 backdrop-blur-sm animate-fadeIn"
      role="dialog"
      aria-modal="true"
    >
      <div className="absolute inset-0" onClick={onClose} />

      <div className="relative w-full max-w-3xl bg-[#121620] border border-[#1E2536] rounded-2xl shadow-2xl overflow-hidden my-8 animate-scaleIn">
        <div className="p-6 border-b border-[#1E2536] bg-[#0E121A] flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <DisciplineLogo discipline={room.discipline} className="w-11 h-11 rounded-xl shrink-0" />
            <div className="min-w-0">
              <h2 className="text-lg font-extrabold text-white tracking-tight">Match Room</h2>
              <p className="text-xs text-slate-400">{discipline.name} | {discipline.formatLabel}</p>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <span className="px-3 py-1 rounded-full border border-orange-500/30 bg-orange-500/10 text-orange-400 text-[11px] font-mono font-bold">
              {STATUS_LABELS[room.status]}
            </span>
            <button
              type="button"
              onClick={onClose}
              className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition"
              aria-label="Закрити Match Room"
            >
              <CloseIcon className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="p-6 space-y-6 max-h-[75vh] overflow-y-auto">
          {actionError && (
            <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2.5">
              <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div>
                <div className="font-bold text-rose-300">{actionError.title}</div>
                <div className="mt-0.5">{actionError.detail}</div>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <TeamPanel
              team={room.team1}
              details={team1Details}
              isCaptainView={isCaptain1}
            />
            <TeamPanel
              team={room.team2}
              details={team2Details}
              isCaptainView={isCaptain2}
            />
          </div>

          {(room.status === 'PENDING' || room.status === 'READY_CHECK') && (
            <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] flex items-center justify-between gap-3 flex-wrap">
              <div className="flex items-center gap-2 text-xs text-slate-400">
                <ClockIcon className="w-4 h-4 text-orange-400" />
                <span>Ready Check: обидва капітани мають підтвердити готовність</span>
              </div>
              {canMarkReady ? (
                <button
                  type="button"
                  onClick={handleReady}
                  disabled={isSubmittingReady}
                  className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition flex items-center gap-1.5 cursor-pointer"
                >
                  <CheckIcon className="w-3.5 h-3.5" />
                  <span>{isSubmittingReady ? 'Підтвердження...' : 'Я готовий'}</span>
                </button>
              ) : myTeamReady ? (
                <span className="text-xs font-mono text-emerald-400">Ваша команда вже готова</span>
              ) : (
                <span className="text-xs font-mono text-slate-500">Лише капітан може підтвердити готовність</span>
              )}
            </div>
          )}

          {showVetoSection && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-mono uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                  <ShieldIcon className="w-3.5 h-3.5 text-orange-400" />
                  <span>Map Veto</span>
                </h3>
                {room.status === 'PICK_BAN' && (
                  <span className="text-xs font-mono text-orange-400">
                    Черга банити: {teamNameById(room.currentBanTurnTeamId) ?? '—'}
                  </span>
                )}
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                {allMapTiles.map((mapName) => {
                  const ban = room.bannedMaps.find((b) => b.mapName === mapName)
                  const isDecided = room.decidedMap === mapName
                  return (
                    <div
                      key={mapName}
                      className={`p-3 rounded-xl border flex flex-col gap-2 ${
                        ban
                          ? 'bg-rose-500/5 border-rose-500/30'
                          : isDecided
                            ? 'bg-emerald-500/10 border-emerald-500/40'
                            : 'bg-[#0B0E14] border-[#1E2536]'
                      }`}
                    >
                      <span
                        className={`text-sm font-bold ${ban ? 'text-rose-300 line-through' : 'text-white'}`}
                      >
                        {mapName}
                      </span>
                      {ban ? (
                        <span className="text-[10px] font-mono text-rose-400">
                          Забанено: {teamNameById(ban.bannedByTeamId) ?? '—'}
                        </span>
                      ) : isDecided ? (
                        <span className="text-[10px] font-mono text-emerald-400">Ігрова карта</span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => handleBan(mapName)}
                          disabled={!isMyBanTurn || banningMap !== null}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-rose-600 disabled:bg-slate-900 disabled:text-slate-600 text-white text-[11px] font-semibold transition cursor-pointer disabled:cursor-not-allowed"
                        >
                          {banningMap === mapName ? 'Банимо...' : 'Викреслити'}
                        </button>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {room.status === 'IN_PROGRESS' && room.credentials && (
            <div className="p-4 rounded-xl bg-[#0B0E14] border border-amber-900/40 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
                  <GamepadIcon className="w-4 h-4" />
                  <span>Підключення до гри</span>
                </span>
                {room.decidedMap && (
                  <span className="text-[11px] font-mono text-slate-400">Карта: {room.decidedMap}</span>
                )}
              </div>

              {room.discipline === 'CS2' && cs2ConnectString && (
                <div className="space-y-2">
                  <div className="p-3 bg-black/80 rounded-lg border border-amber-900/40 font-mono text-xs text-amber-300 select-all break-all flex items-center gap-2">
                    <ServerIcon className="w-3.5 h-3.5 shrink-0" />
                    <span>{cs2ConnectString}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleCopy('cs2', cs2ConnectString)}
                    className="w-full py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    {copiedField === 'cs2' ? (
                      <CheckIcon className="w-3.5 h-3.5 text-emerald-400" />
                    ) : (
                      <CopyIcon className="w-3.5 h-3.5" />
                    )}
                    <span>{copiedField === 'cs2' ? 'Скопійовано!' : 'Копіювати connect IP'}</span>
                  </button>
                </div>
              )}

              {room.discipline === 'BRAWL_STARS' && brawlStarsCode && (
                <div className="space-y-2">
                  <div className="p-3 bg-black/80 rounded-lg border border-amber-900/40 font-mono text-sm text-amber-300 select-all text-center tracking-widest">
                    {brawlStarsCode}
                  </div>
                  <button
                    type="button"
                    onClick={() => handleCopy('brawl', brawlStarsCode)}
                    className="w-full py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    {copiedField === 'brawl' ? (
                      <CheckIcon className="w-3.5 h-3.5 text-emerald-400" />
                    ) : (
                      <CopyIcon className="w-3.5 h-3.5" />
                    )}
                    <span>{copiedField === 'brawl' ? 'Скопійовано!' : 'Копіювати код кімнати'}</span>
                  </button>
                </div>
              )}

              {room.discipline === 'CHESS' && chessGameUrl && (
                <a
                  href={chessGameUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="w-full py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-xs font-bold text-white transition flex items-center justify-center gap-1.5"
                >
                  <span>Перейти до партії на Chess.com</span>
                </a>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

const TeamPanel: React.FC<{
  team: MatchRoom['team1']
  details: TeamDetails | null
  isCaptainView: boolean
}> = ({ team, details, isCaptainView }) => (
  <div
    className={`p-4 rounded-xl border space-y-3 ${
      isCaptainView ? 'bg-[#181F2E] border-orange-500/40' : 'bg-[#0B0E14] border-[#1E2536]'
    }`}
  >
    <div className="flex items-center justify-between gap-2">
      <div className="flex items-center gap-2 min-w-0">
        <UsersIcon className="w-4 h-4 text-orange-400 shrink-0" />
        <span className="text-sm font-bold text-white truncate">{team.name ?? 'Команда'}</span>
      </div>
      <span
        className={`shrink-0 px-2 py-0.5 rounded-md border text-[10px] font-mono font-bold ${
          team.isReady
            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
            : 'bg-slate-800 text-slate-400 border-slate-700'
        }`}
      >
        {team.isReady ? 'Готова' : 'Не готова'}
      </span>
    </div>

    {details && (
      <div className="divide-y divide-[#1E2536] rounded-lg border border-[#1E2536] bg-[#121620] overflow-hidden">
        {details.members.map((member) => (
          <div key={member.userId} className="px-3 py-2 flex items-center justify-between gap-2">
            <span className="text-xs font-semibold text-white truncate flex items-center gap-1.5">
              {member.displayName}
              {member.isCaptain && <CrownIcon className="w-3 h-3 text-orange-400 shrink-0" />}
            </span>
            {member.gameIdentifier && (
              <span className="text-[10px] font-mono text-slate-400 shrink-0">{member.gameIdentifier}</span>
            )}
          </div>
        ))}
      </div>
    )}
  </div>
)

import React, { useEffect, useState } from 'react'
import { ApiError } from '../api/http'
import { joinTeamByInvite, previewTeamInvite } from '../api/teams'
import { useAuth } from '../context/AuthContext'
import { savePendingInvite } from '../utils/pendingInvite'
import type { TeamDetails, TeamInvitePreview } from '../types/teams'
import { DISCIPLINE_CONFIG, type ProblemDetail } from '../types/tournament'
import { DisciplineLogo } from './DisciplineLogo'
import { AlertCircleIcon, CloseIcon, LinkIcon, LogInIcon, UsersIcon } from './icons/Icons'

// US-05 messages per scenario; the backend `detail` is English, so known codes get a Ukrainian explanation.
const JOIN_ERROR_MESSAGES: Record<string, string> = {
  ROSTER_FULL: 'У команді немає вільних місць — ростер уже заповнений.',
  PLAYER_ALREADY_IN_TEAM: 'Ви вже є в ростері цієї команди.',
  INVITE_LINK_EXPIRED: 'Запрошення недійсне: посилання застаріло, деактивоване або команди не існує.',
  TEAM_NOT_FOUND: 'Запрошення недійсне: команди не існує.',
  ROSTER_LOCKED: 'Склад команди заблоковано (Roster Lock) — вступ після дедлайну реєстрації неможливий.'
}

function explainJoinError(problem: ProblemDetail): string {
  return JOIN_ERROR_MESSAGES[problem.code ?? ''] ?? problem.detail
}

export interface JoinInviteModalProps {
  token: string | null
  onClose: () => void
  onJoined?: (team: TeamDetails) => void
}

export const JoinInviteModal: React.FC<JoinInviteModalProps> = ({ token, onClose, onJoined }) => {
  if (!token) return null
  return <JoinInviteContent key={token} token={token} onClose={onClose} onJoined={onJoined} />
}

interface JoinInviteContentProps {
  token: string
  onClose: () => void
  onJoined?: (team: TeamDetails) => void
}

const JoinInviteContent: React.FC<JoinInviteContentProps> = ({ token, onClose, onJoined }) => {
  const { user, openAuthModal } = useAuth()
  const [preview, setPreview] = useState<TeamInvitePreview | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<ProblemDetail | null>(null)
  const [joinError, setJoinError] = useState<ProblemDetail | null>(null)
  const [isJoining, setIsJoining] = useState(false)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  useEffect(() => {
    let cancelled = false

    previewTeamInvite(token)
      .then((data) => {
        if (!cancelled) {
          setPreview(data)
          setLoadError(null)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setLoadError(err.problem)
          } else {
            setLoadError({
              title: 'Помилка завантаження інвайту',
              status: 500,
              detail: 'Не вдалося перевірити інвайт-посилання.'
            })
          }
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [token])

  const handleConfirmJoin = async () => {
    setJoinError(null)
    setIsJoining(true)
    try {
      const joinedTeam = await joinTeamByInvite(token)
      onJoined?.(joinedTeam)
      onClose()
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setJoinError(err.problem)
      } else {
        setJoinError({
          title: 'Помилка вступу до команди',
          status: 500,
          detail: 'Не вдалося приєднатися до команди. Спробуйте пізніше.'
        })
      }
    } finally {
      setIsJoining(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto bg-black/75 backdrop-blur-sm animate-fadeIn"
      role="dialog"
      aria-modal="true"
      aria-labelledby="join-invite-title"
    >
      <div className="relative w-full max-w-md bg-[#121620] border border-[#1E2536] rounded-2xl shadow-2xl overflow-hidden animate-scaleIn">
        <div className="px-6 py-5 border-b border-[#1E2536] flex items-center justify-between bg-[#0E121A]">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-orange-500/10 border border-orange-500/30 text-orange-400">
              <LinkIcon className="w-5 h-5" />
            </div>
            <div>
              <h2 id="join-invite-title" className="text-lg font-bold text-white tracking-tight">
                Запрошення до команди
              </h2>
              <p className="text-xs text-slate-400">Підтвердження вступу в ростер</p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition"
            aria-label="Закрити вікно запрошення"
          >
            <CloseIcon className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-5">
          {isLoading ? (
            <div className="h-32 rounded-xl bg-[#0B0E14] border border-[#1E2536] animate-pulse" />
          ) : loadError ? (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2.5">
                <AlertCircleIcon className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-sm text-rose-300">{loadError.title}</div>
                  <div className="mt-1">{explainJoinError(loadError)}</div>
                </div>
              </div>
              <div className="flex justify-end">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-white transition"
                >
                  Повернутися до турнірів
                </button>
              </div>
            </div>
          ) : (
            preview && (
              <div className="space-y-5">
                <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3 min-w-0">
                    <DisciplineLogo
                      discipline={preview.discipline}
                      className="w-12 h-12 rounded-xl shrink-0"
                    />
                    <div className="min-w-0">
                      <div className="text-base font-extrabold text-white truncate">
                        {preview.teamName}
                      </div>
                      <div className="text-xs font-mono text-slate-400 mt-0.5">
                        {DISCIPLINE_CONFIG[preview.discipline].name} |{' '}
                        {DISCIPLINE_CONFIG[preview.discipline].formatLabel}
                      </div>
                    </div>
                  </div>
                  <div className="shrink-0 text-right">
                    <div className="text-[10px] font-mono uppercase text-slate-500">Слоти</div>
                    <div className="text-sm font-mono font-bold text-orange-400 flex items-center gap-1">
                      <UsersIcon className="w-3.5 h-3.5" />
                      <span>
                        {preview.memberCount} / {preview.maxMembers}
                      </span>
                    </div>
                  </div>
                </div>

                {preview.isFull && (
                  <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-200 text-xs">
                    У команді вже заповнено всі слоти, приєднання неможливе.
                  </div>
                )}

                {joinError && (
                  <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2.5">
                    <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                    <div className="flex-1">
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-bold text-rose-300">{joinError.title}</span>
                        {joinError.code && (
                          <span className="font-mono text-[10px] px-2 py-0.5 bg-rose-950/80 rounded border border-rose-800/80 text-rose-300">
                            {joinError.code}
                          </span>
                        )}
                      </div>
                      <div className="mt-1">{explainJoinError(joinError)}</div>
                    </div>
                  </div>
                )}

                {!user ? (
                  <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-3">
                    <p className="text-xs text-slate-300 leading-relaxed">
                      Щоб прийняти запрошення та приєднатися до команди{' '}
                      <span className="font-bold text-white">{preview.teamName}</span>, спочатку
                      увійдіть до свого профілю.
                    </p>
                    <button
                      type="button"
                      onClick={() => {
                        savePendingInvite(token)
                        openAuthModal('Увійдіть через Steam або пошту для підтвердження вступу до команди.')
                      }}
                      className="w-full py-2.5 px-4 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition flex items-center justify-center gap-2 cursor-pointer"
                    >
                      <LogInIcon className="w-4 h-4" />
                      <span>Увійти для підтвердження вступу</span>
                    </button>
                  </div>
                ) : (
                  <div className="flex items-center justify-end gap-3 pt-2 border-t border-[#1E2536]">
                    <button
                      type="button"
                      onClick={onClose}
                      className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-white transition"
                    >
                      Скасувати
                    </button>
                    <button
                      type="button"
                      disabled={preview.isFull || isJoining}
                      onClick={handleConfirmJoin}
                      className="px-5 py-2.5 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition shadow-lg shadow-orange-500/20 cursor-pointer disabled:cursor-not-allowed"
                    >
                      {isJoining ? 'Приєднання...' : 'Підтвердити вступ до команди'}
                    </button>
                  </div>
                )}
              </div>
            )
          )}
        </div>
      </div>
    </div>
  )
}

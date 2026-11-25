import React, { useEffect, useState } from 'react'
import { ApiError } from '../api/http'
import { createTeam, createTeamInvite, getMyTeams, getTeamById } from '../api/teams'
import { useAuth } from '../context/AuthContext'
import type { TeamDetails, TeamInvite, TeamSummary } from '../types/teams'
import { DISCIPLINE_CONFIG, type Discipline, type ProblemDetail } from '../types/tournament'
import { DisciplineLogo } from './DisciplineLogo'
import {
  AlertCircleIcon,
  CheckIcon,
  CloseIcon,
  CopyIcon,
  CrownIcon,
  LinkIcon,
  PlusIcon,
  UsersIcon
} from './icons/Icons'

export interface TeamsDrawerProps {
  isOpen: boolean
  onClose: () => void
  initialTeamId?: string | null
}

function formatMissingPlayersLabel(missingCount: number): string {
  if (missingCount === 1) {
    return 'Не вистачає 1 гравця'
  }
  return `Не вистачає ${missingCount} гравців`
}

export const TeamsDrawer: React.FC<TeamsDrawerProps> = ({ isOpen, onClose, initialTeamId }) => {
  if (!isOpen) return null
  return <TeamsDrawerContent onClose={onClose} initialTeamId={initialTeamId} />
}

const TeamsDrawerContent: React.FC<Omit<TeamsDrawerProps, 'isOpen'>> = ({
  onClose,
  initialTeamId
}) => {
  const { user, openAuthModal } = useAuth()

  const [teams, setTeams] = useState<TeamSummary[]>([])
  const [isLoadingList, setIsLoadingList] = useState(true)
  const [listError, setListError] = useState<ProblemDetail | null>(null)

  const [selectedTeam, setSelectedTeam] = useState<TeamDetails | null>(null)
  const [isLoadingDetails, setIsLoadingDetails] = useState(false)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [teamName, setTeamName] = useState('')
  const [discipline, setDiscipline] = useState<Discipline>('CS2')
  const [createError, setCreateError] = useState<ProblemDetail | null>(null)
  const [nameError, setNameError] = useState<string | null>(null)
  const [isCreating, setIsCreating] = useState(false)

  const [invite, setInvite] = useState<TeamInvite | null>(null)
  const [inviteError, setInviteError] = useState<ProblemDetail | null>(null)
  const [isGeneratingInvite, setIsGeneratingInvite] = useState(false)
  const [copiedInvite, setCopiedInvite] = useState(false)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  useEffect(() => {
    if (!user) {
      return
    }

    let cancelled = false

    getMyTeams()
      .then((list) => {
        if (cancelled) return
        setTeams(list)
        setListError(null)
        const targetId = initialTeamId || list[0]?.id
        if (targetId) {
          setIsLoadingDetails(true)
          getTeamById(targetId)
            .then((details) => {
              if (!cancelled) {
                setSelectedTeam(details)
              }
            })
            .catch(() => {
              // ignore silent detail preload failure
            })
            .finally(() => {
              if (!cancelled) setIsLoadingDetails(false)
            })
        }
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError) {
          setListError(err.problem)
        } else {
          setListError({
            title: 'Помилка завантаження',
            status: 500,
            detail: 'Не вдалося завантажити список ваших команд.'
          })
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoadingList(false)
      })

    return () => {
      cancelled = true
    }
  }, [user, initialTeamId])

  const handleSelectTeam = async (teamId: string) => {
    setInvite(null)
    setInviteError(null)
    setCopiedInvite(false)
    setIsLoadingDetails(true)
    try {
      const details = await getTeamById(teamId)
      setSelectedTeam(details)
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setListError(err.problem)
      }
    } finally {
      setIsLoadingDetails(false)
    }
  }

  const handleCreateTeam = async (e: React.FormEvent) => {
    e.preventDefault()
    setNameError(null)
    setCreateError(null)

    const trimmed = teamName.trim()
    if (trimmed.length < 2) {
      setNameError('Назва команди має містити щонайменше 2 символи.')
      return
    }

    setIsCreating(true)
    try {
      const created = await createTeam({ name: trimmed, discipline })
      setTeams((prev) => [created, ...prev])
      setTeamName('')
      setShowCreateForm(false)
      setInvite(null)
      setInviteError(null)
      const details = await getTeamById(created.id)
      setSelectedTeam(details)
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setCreateError(err.problem)
        if (err.problem.invalidParams) {
          const nameParam = err.problem.invalidParams.find((p) => p.name === 'name')
          if (nameParam) setNameError(nameParam.reason)
        }
      } else {
        setCreateError({
          title: 'Помилка створення команди',
          status: 500,
          detail: 'Не вдалося створити команду. Спробуйте пізніше.'
        })
      }
    } finally {
      setIsCreating(false)
    }
  }

  const handleGenerateInvite = async () => {
    if (!selectedTeam) return
    setInviteError(null)
    setCopiedInvite(false)
    setIsGeneratingInvite(true)

    try {
      const createdInvite = await createTeamInvite(selectedTeam.id)
      setInvite(createdInvite)
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setInviteError(err.problem)
      } else {
        setInviteError({
          title: 'Помилка генерації інвайту',
          status: 500,
          detail: 'Не вдалося згенерувати інвайт-посилання.'
        })
      }
    } finally {
      setIsGeneratingInvite(false)
    }
  }

  const resolvedInviteUrl = invite ? `${window.location.origin}/join/${invite.token}` : ''

  const handleCopyInvite = async () => {
    if (!resolvedInviteUrl) return
    try {
      await navigator.clipboard.writeText(resolvedInviteUrl)
      setCopiedInvite(true)
      setTimeout(() => setCopiedInvite(false), 2000)
    } catch {
      // clipboard fallback ignored
    }
  }

  const isCaptain = Boolean(user && selectedTeam && user.id === selectedTeam.captainId)
  const missingPlayers = selectedTeam
    ? Math.max(0, selectedTeam.maxMembers - selectedTeam.memberCount)
    : 0
  const fillPercent = selectedTeam
    ? Math.min(100, Math.round((selectedTeam.memberCount / selectedTeam.maxMembers) * 100))
    : 0

  return (
    <div className="fixed inset-0 z-50 overflow-hidden animate-fadeIn" role="dialog" aria-modal="true">
      <div
        className="absolute inset-0 bg-black/70 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-2xl bg-[#121620] border-l border-[#1E2536] shadow-2xl flex flex-col justify-between animate-slideInRight">
          {/* Header */}
          <div className="p-6 border-b border-[#1E2536] bg-[#0E121A] flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-orange-500/10 border border-orange-500/30 text-orange-400">
                <UsersIcon className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-lg font-extrabold text-white tracking-tight">Мої команди</h2>
                <p className="text-xs text-slate-400">
                  Створення ростерів, контроль повноти складу та інвайт-посилання
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {user && (
                <button
                  type="button"
                  onClick={() => {
                    setShowCreateForm((prev) => !prev)
                    setCreateError(null)
                    setNameError(null)
                  }}
                  className="px-3.5 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition flex items-center gap-1.5 shadow-lg shadow-orange-500/20 cursor-pointer"
                >
                  <PlusIcon className="w-3.5 h-3.5" />
                  <span>{showCreateForm ? 'Скасувати' : 'Створити команду'}</span>
                </button>
              )}

              <button
                type="button"
                onClick={onClose}
                className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition"
                aria-label="Закрити панель команд"
              >
                <CloseIcon className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {!user ? (
              <div className="p-6 rounded-2xl bg-[#0B0E14] border border-[#1E2536] text-center space-y-4">
                <UsersIcon className="w-10 h-10 text-orange-400 mx-auto" />
                <div className="space-y-1">
                  <h3 className="text-base font-bold text-white">Авторизуйтесь для роботи з командами</h3>
                  <p className="text-xs text-slate-400 max-w-sm mx-auto">
                    Увійдіть через Steam або електронну пошту, щоб створювати команди та керувати ростерами.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => openAuthModal('Увійдіть, щоб переглядати та створювати команди.')}
                  className="px-5 py-2.5 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition cursor-pointer"
                >
                  Увійти
                </button>
              </div>
            ) : (
              <>
                {/* Create Team Form */}
                {showCreateForm && (
                  <form
                    onSubmit={handleCreateTeam}
                    className="p-5 rounded-2xl bg-[#0B0E14] border border-orange-500/40 space-y-4 animate-fadeIn"
                  >
                    <div className="flex items-center justify-between">
                      <h3 className="text-sm font-bold text-white">Нова команда</h3>
                      <span className="text-[11px] font-mono text-slate-400">
                        Дисципліна фіксується назавжди
                      </span>
                    </div>

                    {createError && (
                      <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2">
                        <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                        <div>
                          <div className="font-semibold text-rose-300">{createError.title}</div>
                          <div className="mt-0.5">{createError.detail}</div>
                        </div>
                      </div>
                    )}

                    <div>
                      <label
                        htmlFor="create-team-name"
                        className="block text-xs font-mono uppercase tracking-wider text-slate-300 mb-1.5"
                      >
                        Назва команди <span className="text-orange-400">*</span>
                      </label>
                      <input
                        id="create-team-name"
                        type="text"
                        value={teamName}
                        onChange={(e) => {
                          setTeamName(e.target.value)
                          if (nameError) setNameError(null)
                        }}
                        placeholder="Наприклад: NaUKMA Five"
                        className={`w-full px-3.5 py-2.5 bg-[#121620] border rounded-xl text-sm text-white placeholder-slate-600 focus:outline-none transition ${
                          nameError
                            ? 'border-rose-500 focus:ring-1 focus:ring-rose-500'
                            : 'border-[#1E2536] focus:border-orange-500'
                        }`}
                      />
                      {nameError && <p className="text-xs text-rose-400 mt-1">{nameError}</p>}
                    </div>

                    <div>
                      <div className="text-xs font-mono uppercase tracking-wider text-slate-300 mb-2">
                        Ігрова дисципліна <span className="text-orange-400">*</span>
                      </div>
                      <div className="grid grid-cols-3 gap-2.5">
                        {(Object.keys(DISCIPLINE_CONFIG) as Discipline[]).map((key) => {
                          const meta = DISCIPLINE_CONFIG[key]
                          const active = discipline === key
                          return (
                            <button
                              key={key}
                              type="button"
                              onClick={() => setDiscipline(key)}
                              className={`p-3 rounded-xl border text-left transition flex flex-col gap-2 cursor-pointer ${
                                active
                                  ? `${meta.theme.badgeBg} border-orange-500 border-2`
                                  : 'bg-[#121620] border-[#1E2536] hover:border-slate-700'
                              }`}
                            >
                              <div className="flex items-center justify-between">
                                <DisciplineLogo discipline={key} className="w-7 h-7 rounded-lg" />
                                <span
                                  className={`text-[11px] font-mono font-bold px-1.5 py-0.5 rounded border ${meta.theme.badgeBg} ${meta.theme.badgeText} ${meta.theme.badgeBorder}`}
                                >
                                  {meta.formatLabel}
                                </span>
                              </div>
                              <div className="text-xs font-bold text-white">{meta.shortName}</div>
                            </button>
                          )
                        })}
                      </div>
                    </div>

                    <div className="flex justify-end gap-2 pt-2">
                      <button
                        type="submit"
                        disabled={isCreating}
                        className="px-5 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition cursor-pointer"
                      >
                        {isCreating ? 'Створення...' : 'Створити команду'}
                      </button>
                    </div>
                  </form>
                )}

                {listError && (
                  <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2.5">
                    <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                    <div>
                      <div className="font-bold text-rose-300">{listError.title}</div>
                      <div className="mt-0.5">{listError.detail}</div>
                    </div>
                  </div>
                )}

                {isLoadingList ? (
                  <div className="space-y-3">
                    {[1, 2].map((idx) => (
                      <div
                        key={idx}
                        className="h-20 rounded-xl bg-[#0B0E14] border border-[#1E2536] animate-pulse"
                      />
                    ))}
                  </div>
                ) : teams.length === 0 ? (
                  <div className="p-6 rounded-2xl bg-[#0B0E14] border border-[#1E2536] text-center space-y-3">
                    <p className="text-sm font-semibold text-slate-300">У вас ще немає жодної команди</p>
                    <p className="text-xs text-slate-500">
                      Створіть команду або перейдіть за інвайт-посиланням від капітана.
                    </p>
                    {!showCreateForm && (
                      <button
                        type="button"
                        onClick={() => setShowCreateForm(true)}
                        className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition cursor-pointer"
                      >
                        Створити першу команду
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="space-y-6">
                    {/* Teams Selector Grid */}
                    <div>
                      <h3 className="text-xs font-mono uppercase tracking-wider text-slate-400 mb-2.5">
                        Ваші команди ({teams.length})
                      </h3>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                        {teams.map((t) => {
                          const disc = DISCIPLINE_CONFIG[t.discipline]
                          const isSelected = selectedTeam?.id === t.id
                          return (
                            <button
                              key={t.id}
                              type="button"
                              onClick={() => handleSelectTeam(t.id)}
                              className={`p-3.5 rounded-xl border text-left transition flex items-center justify-between gap-3 cursor-pointer ${
                                isSelected
                                  ? 'bg-[#181F2E] border-orange-500 shadow-lg shadow-orange-500/10'
                                  : 'bg-[#0B0E14] border-[#1E2536] hover:border-slate-700'
                              }`}
                            >
                              <div className="flex items-center gap-3 min-w-0">
                                <DisciplineLogo
                                  discipline={t.discipline}
                                  className="w-9 h-9 rounded-lg shrink-0"
                                />
                                <div className="min-w-0">
                                  <div className="text-sm font-bold text-white truncate">{t.name}</div>
                                  <div className="text-[11px] font-mono text-slate-400">
                                    {disc.shortName} | {disc.formatLabel}
                                  </div>
                                </div>
                              </div>
                              <span
                                className={`shrink-0 px-2 py-0.5 rounded-md border text-[11px] font-mono font-bold ${
                                  t.isFull
                                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                                    : 'bg-amber-500/10 text-amber-300 border-amber-500/30'
                                }`}
                              >
                                {t.memberCount} із {t.maxMembers}
                              </span>
                            </button>
                          )
                        })}
                      </div>
                    </div>

                    {/* Selected Team Details */}
                    {isLoadingDetails ? (
                      <div className="h-48 rounded-2xl bg-[#0B0E14] border border-[#1E2536] animate-pulse" />
                    ) : (
                      selectedTeam && (
                        <div className="p-5 rounded-2xl bg-[#0B0E14] border border-[#1E2536] space-y-5">
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <div className="flex items-center gap-3">
                              <DisciplineLogo
                                discipline={selectedTeam.discipline}
                                className="w-11 h-11 rounded-xl"
                              />
                              <div>
                                <h3 className="text-base font-extrabold text-white">
                                  {selectedTeam.name}
                                </h3>
                                <div className="text-xs font-mono text-slate-400">
                                  {DISCIPLINE_CONFIG[selectedTeam.discipline].name} | Формат{' '}
                                  {DISCIPLINE_CONFIG[selectedTeam.discipline].formatLabel}
                                </div>
                              </div>
                            </div>

                            <span
                              className={`px-3 py-1 rounded-full border text-xs font-mono font-bold ${
                                selectedTeam.isFull
                                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                                  : 'bg-amber-500/10 text-amber-300 border-amber-500/30'
                              }`}
                            >
                              {selectedTeam.isFull
                                ? 'Склад повний'
                                : formatMissingPlayersLabel(missingPlayers)}
                            </span>
                          </div>

                          {/* Visual Roster Fullness Indicator */}
                          <div className="p-3.5 rounded-xl bg-[#121620] border border-[#1E2536] space-y-2">
                            <div className="flex items-center justify-between text-xs">
                              <span className="text-slate-400">Заповненість ростеру</span>
                              <span className="font-mono font-bold text-white">
                                {selectedTeam.memberCount} із {selectedTeam.maxMembers} гравців
                              </span>
                            </div>
                            <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                              <div
                                className={`h-2 rounded-full transition-all duration-300 ${
                                  selectedTeam.isFull ? 'bg-emerald-500' : 'bg-orange-500'
                                }`}
                                style={{ width: `${fillPercent}%` }}
                              />
                            </div>
                          </div>

                          {/* Roster Members */}
                          <div>
                            <h4 className="text-xs font-mono uppercase tracking-wider text-slate-400 mb-2">
                              Учасники команди
                            </h4>
                            <div className="divide-y divide-[#1E2536] rounded-xl border border-[#1E2536] bg-[#121620] overflow-hidden">
                              {selectedTeam.members.map((member) => (
                                <div
                                  key={member.userId}
                                  className="px-4 py-3 flex items-center justify-between gap-3"
                                >
                                  <div className="flex items-center gap-2.5 min-w-0">
                                    <span className="text-sm font-semibold text-white truncate">
                                      {member.displayName}
                                    </span>
                                    {member.isCaptain && (
                                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-orange-500/10 border border-orange-500/30 text-orange-400 text-[10px] font-mono font-bold">
                                        <CrownIcon className="w-3 h-3" />
                                        <span>Капітан</span>
                                      </span>
                                    )}
                                  </div>

                                  {member.gameIdentifier ? (
                                    <span className="text-xs font-mono text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-1 rounded-lg">
                                      {member.gameIdentifier}
                                    </span>
                                  ) : (
                                    <span className="text-[11px] font-mono text-amber-300 bg-amber-500/10 border border-amber-500/30 px-2.5 py-1 rounded-lg">
                                      Ідентифікатор відсутній
                                    </span>
                                  )}
                                </div>
                              ))}
                            </div>
                          </div>

                          {/* Captain Invite Link Section (US-04) */}
                          {isCaptain && (
                            <div className="p-4 rounded-xl bg-[#121620] border border-[#1E2536] space-y-3">
                              <div className="flex items-center justify-between gap-3">
                                <div>
                                  <div className="text-xs font-bold text-white flex items-center gap-1.5">
                                    <LinkIcon className="w-3.5 h-3.5 text-orange-400" />
                                    <span>Запрошення гравців у ростер</span>
                                  </div>
                                  <p className="text-[11px] text-slate-400 mt-0.5">
                                    {selectedTeam.isFull
                                      ? 'Склад команди вже укомплектовано — нові інвайти не потрібні.'
                                      : 'Згенеруйте посилання та надішліть його тіммейтам для вступу.'}
                                  </p>
                                </div>

                                <button
                                  type="button"
                                  onClick={handleGenerateInvite}
                                  disabled={selectedTeam.isFull || isGeneratingInvite}
                                  className="shrink-0 px-3.5 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition flex items-center gap-1.5 cursor-pointer disabled:cursor-not-allowed"
                                >
                                  <LinkIcon className="w-3.5 h-3.5" />
                                  <span>
                                    {isGeneratingInvite
                                      ? 'Генерація...'
                                      : 'Згенерувати інвайт-посилання'}
                                  </span>
                                </button>
                              </div>

                              {inviteError && (
                                <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200">
                                  {inviteError.detail}
                                </div>
                              )}

                              {invite && (
                                <div className="flex items-center gap-2 pt-1 animate-fadeIn">
                                  <input
                                    type="text"
                                    readOnly
                                    value={resolvedInviteUrl}
                                    aria-label="Інвайт-посилання команди"
                                    className="flex-1 bg-[#0B0E14] border border-[#263147] rounded-lg px-3 py-2 text-xs font-mono text-slate-200 select-all focus:outline-none"
                                  />
                                  <button
                                    type="button"
                                    onClick={handleCopyInvite}
                                    className={`px-3.5 py-2 rounded-lg text-xs font-semibold transition flex items-center gap-1.5 cursor-pointer ${
                                      copiedInvite
                                        ? 'bg-emerald-600 text-white'
                                        : 'bg-slate-800 hover:bg-slate-700 text-slate-200'
                                    }`}
                                  >
                                    {copiedInvite ? (
                                      <CheckIcon className="w-3.5 h-3.5" />
                                    ) : (
                                      <CopyIcon className="w-3.5 h-3.5" />
                                    )}
                                    <span>{copiedInvite ? 'Скопійовано' : 'Копіювати'}</span>
                                  </button>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      )
                    )}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

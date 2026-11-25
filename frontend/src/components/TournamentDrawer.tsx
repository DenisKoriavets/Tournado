import React, { useEffect, useMemo, useState } from 'react'
import { ApiError } from '../api/http'
import {
  getTournamentRegistrations,
  lockTournamentRosters,
  registerTeamForTournament,
  removeTeamRegistration
} from '../api/registrations'
import { getMyTeams } from '../api/teams'
import { useAuth } from '../context/AuthContext'
import type { TournamentRegistrations } from '../types/registrations'
import type { TeamSummary } from '../types/teams'
import type { ProblemDetail, Tournament } from '../types/tournament'
import { DISCIPLINE_CONFIG, STATUS_CONFIG } from '../types/tournament'
import { formatDateTime } from '../utils/date'
import { DisciplineLogo } from './DisciplineLogo'
import {
  AlertCircleIcon,
  CheckIcon,
  CloseIcon,
  CopyIcon,
  LockIcon,
  LogInIcon,
  ServerIcon,
  TrashIcon,
  UsersIcon
} from './icons/Icons'

interface TournamentDrawerProps {
  tournament: Tournament | null
  isOpen: boolean
  onClose: () => void
  onRegistrationsChanged?: () => void
}

type TabKey = 'overview' | 'schedule' | 'server' | 'participants'

export const TournamentDrawer: React.FC<TournamentDrawerProps> = ({
  tournament,
  isOpen,
  onClose,
  onRegistrationsChanged
}) => {
  if (!isOpen || !tournament) return null
  return (
    <TournamentDrawerContent
      key={tournament.id}
      tournament={tournament}
      onClose={onClose}
      onRegistrationsChanged={onRegistrationsChanged}
    />
  )
}

interface TournamentDrawerContentProps {
  tournament: Tournament
  onClose: () => void
  onRegistrationsChanged?: () => void
}

const TournamentDrawerContent: React.FC<TournamentDrawerContentProps> = ({
  tournament,
  onClose,
  onRegistrationsChanged
}) => {
  const { user, openAuthModal } = useAuth()
  const [activeTab, setActiveTab] = useState<TabKey>('overview')
  const [copiedLink, setCopiedLink] = useState(false)
  const [copiedConnect, setCopiedConnect] = useState(false)

  const [registrations, setRegistrations] = useState<TournamentRegistrations | null>(null)
  const [isLoadingRegistrations, setIsLoadingRegistrations] = useState(false)
  const [registrationsError, setRegistrationsError] = useState<ProblemDetail | null>(null)

  const [myTeams, setMyTeams] = useState<TeamSummary[]>([])
  const [selectedTeamId, setSelectedTeamId] = useState<string>('')
  const [isRegistering, setIsRegistering] = useState(false)
  const [actionError, setActionError] = useState<ProblemDetail | null>(null)
  const [actionSuccess, setActionSuccess] = useState<string | null>(null)

  const [pendingRemoveTeamId, setPendingRemoveTeamId] = useState<string | null>(null)
  const [removingTeamId, setRemovingTeamId] = useState<string | null>(null)
  const [isLockingRosters, setIsLockingRosters] = useState(false)
  const [nowMs] = useState(() => Date.now())

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  // Load registrations (and user teams if logged in) when participants tab is active
  useEffect(() => {
    if (activeTab !== 'participants') return

    let cancelled = false

    const loadParticipantsData = async () => {
      setIsLoadingRegistrations(true)
      setRegistrationsError(null)
      try {
        const regData = await getTournamentRegistrations(tournament.id)
        if (!cancelled) {
          setRegistrations(regData)
        }
      } catch (err: unknown) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setRegistrationsError(err.problem)
          } else {
            setRegistrationsError({
              title: 'Помилка завантаження',
              status: 500,
              detail: 'Не вдалося завантажити список зареєстрованих команд.'
            })
          }
        }
      } finally {
        if (!cancelled) setIsLoadingRegistrations(false)
      }

      if (user) {
        try {
          const teams = await getMyTeams()
          if (!cancelled) {
            setMyTeams(teams)
          }
        } catch {
          if (!cancelled) setMyTeams([])
        }
      } else if (!cancelled) {
        setMyTeams([])
      }
    }

    void loadParticipantsData()

    return () => {
      cancelled = true
    }
  }, [activeTab, tournament.id, user])

  const eligibleTeams = useMemo(() => {
    if (!user) return []
    const registeredIds = new Set(registrations?.teams.map((t) => t.teamId) ?? [])
    return myTeams.filter(
      (t) =>
        t.discipline === tournament.discipline &&
        t.captainId === user.id &&
        !registeredIds.has(t.id)
    )
  }, [user, myTeams, registrations, tournament.discipline])

  const effectiveSelectedTeamId = useMemo(() => {
    if (eligibleTeams.some((t) => t.id === selectedTeamId)) {
      return selectedTeamId
    }
    return eligibleTeams[0]?.id ?? ''
  }, [eligibleTeams, selectedTeamId])

  const refreshParticipants = async () => {
    const regData = await getTournamentRegistrations(tournament.id)
    setRegistrations(regData)
    onRegistrationsChanged?.()
  }

  const handleRegisterTeam = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!effectiveSelectedTeamId) return
    setActionError(null)
    setActionSuccess(null)
    setIsRegistering(true)

    try {
      await registerTeamForTournament(tournament.id, effectiveSelectedTeamId)
      await refreshParticipants()
      setActionSuccess('Команду успішно зареєстровано на турнір.')
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setActionError(err.problem)
      } else {
        setActionError({
          title: 'Помилка реєстрації',
          status: 500,
          detail: 'Не вдалося зареєструвати команду. Спробуйте пізніше.'
        })
      }
    } finally {
      setIsRegistering(false)
    }
  }

  const handleConfirmRemoveTeam = async (teamId: string) => {
    setActionError(null)
    setActionSuccess(null)
    setRemovingTeamId(teamId)

    try {
      await removeTeamRegistration(tournament.id, teamId)
      setPendingRemoveTeamId(null)
      await refreshParticipants()
      setActionSuccess('Команду видалено зі списку учасників турніру.')
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setActionError(err.problem)
      } else {
        setActionError({
          title: 'Помилка видалення',
          status: 500,
          detail: 'Не вдалося видалити команду з турніру.'
        })
      }
    } finally {
      setRemovingTeamId(null)
    }
  }

  const handleLockRosters = async () => {
    setActionError(null)
    setActionSuccess(null)
    setIsLockingRosters(true)

    try {
      const result = await lockTournamentRosters(tournament.id)
      await refreshParticipants()
      setActionSuccess(
        `Склади команд зафіксовано (Roster Lock). Заморожено гравців: ${result.totalSnapshottedPlayers}.`
      )
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setActionError(err.problem)
      } else {
        setActionError({
          title: 'Помилка фіксації складів',
          status: 500,
          detail: 'Не вдалося виконати Roster Lock.'
        })
      }
    } finally {
      setIsLockingRosters(false)
    }
  }

  const discMeta = DISCIPLINE_CONFIG[tournament.discipline] || {
    name: tournament.discipline,
    shortName: tournament.discipline,
    teamSize: tournament.teamSize || 1,
    formatLabel: `${tournament.teamSize}v${tournament.teamSize}`,
    category: '',
    description: '',
    defaultMaps: [],
    theme: {
      accentColor: '#94A3B8',
      badgeBg: 'bg-slate-800',
      badgeText: 'text-slate-300',
      badgeBorder: 'border-slate-700',
      glowBorder: 'hover:border-slate-600'
    }
  }

  const statusMeta = STATUS_CONFIG[tournament.status] || {
    label: tournament.status,
    badgeClass: 'bg-slate-800 text-slate-300 border-slate-700',
    dotClass: 'bg-slate-400'
  }

  const publicLink = `${window.location.origin}/tournaments/${tournament.publicSlug || tournament.id}`

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(publicLink)
      setCopiedLink(true)
      setTimeout(() => setCopiedLink(false), 2000)
    } catch {
      // fallback
    }
  }

  const handleCopyConnect = async () => {
    if (!tournament.cs2ServerConnect) return
    try {
      await navigator.clipboard.writeText(tournament.cs2ServerConnect)
      setCopiedConnect(true)
      setTimeout(() => setCopiedConnect(false), 2000)
    } catch {
      // fallback
    }
  }

  const registeredCount = registrations?.registeredCount ?? tournament.registeredTeamsCount ?? 0
  const slotLimit = registrations?.slotLimit ?? tournament.slotLimit ?? 16
  const fillPercent = Math.min(100, Math.round((registeredCount / slotLimit) * 100))

  const deadlinePassed = new Date(tournament.registrationDeadlineAt).getTime() <= nowMs
  const isSlotsFull = registeredCount >= slotLimit
  const canRegister =
    tournament.status === 'REGISTRATION_OPEN' && !deadlinePassed && !isSlotsFull
  const isOrganizer = Boolean(
    user && registrations && registrations.organizerId && user.id === registrations.organizerId
  )
  const canModerate =
    isOrganizer && tournament.status !== 'IN_PROGRESS' && tournament.status !== 'COMPLETED'

  const maps =
    tournament.mapPool && tournament.mapPool.length > 0
      ? tournament.mapPool
      : tournament.discipline !== 'CHESS'
      ? discMeta.defaultMaps
      : []

  return (
    <div className="fixed inset-0 z-50 overflow-hidden animate-fadeIn" role="dialog" aria-modal="true">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/70 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-xl bg-[#121620] border-l border-[#1E2536] shadow-2xl flex flex-col justify-between animate-slideInRight">
          {/* Top Header */}
          <div className="p-6 border-b border-[#1E2536] bg-[#0E121A]">
            <div className="flex items-center justify-between gap-3 mb-3">
              <div className="flex items-center gap-2">
                <span
                  className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-lg border text-xs font-semibold ${discMeta.theme.badgeBg} ${discMeta.theme.badgeText} ${discMeta.theme.badgeBorder}`}
                >
                  <DisciplineLogo
                    discipline={tournament.discipline}
                    className="w-4 h-4 rounded-sm object-cover"
                  />
                  <span>{discMeta.shortName}</span>
                </span>
                <span className="text-xs font-mono font-medium text-slate-400 bg-slate-900 px-2 py-1 rounded border border-slate-800">
                  {discMeta.formatLabel} (Single Elimination)
                </span>
              </div>

              <div className="flex items-center gap-3">
                <span
                  className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border text-[11px] font-mono font-bold tracking-wider ${statusMeta.badgeClass}`}
                >
                  <span className={`h-1.5 w-1.5 rounded-full ${statusMeta.dotClass}`} />
                  <span>{statusMeta.label}</span>
                </span>

                <button
                  type="button"
                  onClick={onClose}
                  className="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800 transition"
                  aria-label="Закрити панель"
                >
                  <CloseIcon className="w-5 h-5" />
                </button>
              </div>
            </div>

            <h1 className="text-xl md:text-2xl font-extrabold text-white tracking-tight">
              {tournament.name}
            </h1>

            {/* Navigation Tabs */}
            <div className="flex flex-wrap items-center gap-1.5 mt-5 border-b border-[#1E2536] -mb-6 pb-2">
              <button
                type="button"
                onClick={() => setActiveTab('overview')}
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition cursor-pointer ${
                  activeTab === 'overview'
                    ? 'bg-orange-500/10 text-orange-400 border border-orange-500/30'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Огляд турніру
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('participants')}
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition cursor-pointer ${
                  activeTab === 'participants'
                    ? 'bg-orange-500/10 text-orange-400 border border-orange-500/30'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Учасники та заявка
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('schedule')}
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition cursor-pointer ${
                  activeTab === 'schedule'
                    ? 'bg-orange-500/10 text-orange-400 border border-orange-500/30'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Розклад та слоти
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('server')}
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition cursor-pointer ${
                  activeTab === 'server'
                    ? 'bg-orange-500/10 text-orange-400 border border-orange-500/30'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {tournament.discipline === 'CS2' ? 'Сервер і карти' : 'Пул карт та правила'}
              </button>
            </div>
          </div>

          {/* Tab Content Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {activeTab === 'overview' && (
              <div className="space-y-6">
                {/* Description */}
                <div>
                  <h3 className="text-xs uppercase font-mono tracking-wider text-slate-400 mb-2">
                    Опис турніру
                  </h3>
                  {tournament.description ? (
                    <div className="text-sm text-slate-300 bg-[#0B0E14] border border-[#1E2536] rounded-xl p-4 leading-relaxed whitespace-pre-line">
                      {tournament.description}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 italic">
                      Організатор не залишив додаткового опису.
                    </p>
                  )}
                </div>

                {/* Public Link Share */}
                <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                      <CopyIcon className="w-3.5 h-3.5 text-orange-400" />
                      <span>Публічне посилання для учасників</span>
                    </span>
                    <span className="text-[11px] text-slate-500 font-mono">
                      Доступно без авторизації
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <input
                      type="text"
                      readOnly
                      value={publicLink}
                      className="flex-1 bg-[#121620] border border-[#263147] rounded-lg px-3 py-2 text-xs font-mono text-slate-200 select-all focus:outline-none"
                    />
                    <button
                      type="button"
                      onClick={handleCopyLink}
                      className={`px-3.5 py-2 rounded-lg text-xs font-semibold transition flex items-center gap-1.5 ${
                        copiedLink
                          ? 'bg-emerald-600 text-white'
                          : 'bg-orange-500 hover:bg-orange-400 text-white'
                      }`}
                    >
                      {copiedLink ? (
                        <CheckIcon className="w-3.5 h-3.5" />
                      ) : (
                        <CopyIcon className="w-3.5 h-3.5" />
                      )}
                      <span>{copiedLink ? 'Скопійовано' : 'Копіювати'}</span>
                    </button>
                  </div>
                </div>

                {/* Key Metrics Grid */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                    <div className="text-[11px] text-slate-500 uppercase font-mono">
                      Формат матчів
                    </div>
                    <div className="text-sm font-bold text-white mt-1">
                      {tournament.teamSize === 1
                        ? '1v1 (Соло)'
                        : `${tournament.teamSize}v${tournament.teamSize} (${tournament.teamSize} гравців)`}
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                    <div className="text-[11px] text-slate-500 uppercase font-mono">
                      Турнірна сітка
                    </div>
                    <div className="text-sm font-bold text-white mt-1">Single Elimination</div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                    <div className="text-[11px] text-slate-500 uppercase font-mono">
                      Слоти команд
                    </div>
                    <div className="text-sm font-mono font-bold text-orange-400 mt-1">
                      {registeredCount} / {slotLimit} команд
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                    <div className="text-[11px] text-slate-500 uppercase font-mono">
                      Правила овертайму
                    </div>
                    <div className="text-sm font-bold text-slate-300 mt-1">
                      {tournament.discipline === 'CS2' ? 'MR3 $10k' : 'Standard Rules'}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'participants' && (
              <div className="space-y-5">
                {/* Summary Header */}
                <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] flex items-center justify-between gap-3">
                  <div>
                    <div className="text-[11px] font-mono uppercase text-slate-500">
                      Заповненість турніру
                    </div>
                    <div className="text-sm font-mono font-bold text-white mt-0.5">
                      {registeredCount} / {slotLimit} команд
                    </div>
                  </div>

                  {registrations?.rostersLocked && (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs font-mono font-semibold">
                      <LockIcon className="w-3.5 h-3.5" />
                      <span>Склади зафіксовано</span>
                    </span>
                  )}
                </div>

                {/* Action Feedback Banners */}
                {actionSuccess && (
                  <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2.5 animate-fadeIn">
                    <CheckIcon className="w-4 h-4 text-emerald-400 shrink-0" />
                    <span>{actionSuccess}</span>
                  </div>
                )}

                {actionError && (
                  <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-200 text-xs space-y-2 animate-fadeIn">
                    <div className="flex items-start gap-2.5">
                      <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                      <div className="flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-bold text-rose-300">{actionError.title}</span>
                          {actionError.code && (
                            <span className="font-mono text-[10px] px-2 py-0.5 bg-rose-950/80 rounded border border-rose-800/80 text-rose-300">
                              {actionError.code}
                            </span>
                          )}
                        </div>
                        <p className="mt-1 text-slate-300">{actionError.detail}</p>
                      </div>
                    </div>
                    {actionError.invalidParams && actionError.invalidParams.length > 0 && (
                      <ul className="pl-6 list-disc space-y-1 text-rose-300 font-mono">
                        {actionError.invalidParams.map((param, idx) => (
                          <li key={`${param.name}-${idx}`}>
                            <span className="font-semibold">{param.name}</span>: {param.reason}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}

                {/* Captain Registration Section (US-12) */}
                <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-3">
                  <h3 className="text-xs font-mono uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
                    <UsersIcon className="w-4 h-4 text-orange-400" />
                    <span>Реєстрація команди на турнір</span>
                  </h3>

                  {!canRegister ? (
                    <p className="text-xs text-slate-400">
                      {isSlotsFull
                        ? 'Усі слоти турніру вже заповнено.'
                        : 'Прийом заявок на цей турнір закрито або дедлайн минув.'}
                    </p>
                  ) : !user ? (
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <p className="text-xs text-slate-400">
                        Авторизуйтесь як капітан команди, щоб подати заявку у дисципліні{' '}
                        {discMeta.shortName}.
                      </p>
                      <button
                        type="button"
                        onClick={() =>
                          openAuthModal('Увійдіть через Steam або пошту, щоб зареєструвати команду.')
                        }
                        className="shrink-0 px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition flex items-center justify-center gap-1.5 cursor-pointer"
                      >
                        <LogInIcon className="w-3.5 h-3.5" />
                        <span>Увійдіть, щоб зареєструвати команду</span>
                      </button>
                    </div>
                  ) : eligibleTeams.length === 0 ? (
                    <p className="text-xs text-slate-400">
                      У вас немає доступних команд капітана у дисципліні {discMeta.shortName}.
                      Створіть команду в розділі «Мої команди».
                    </p>
                  ) : (
                    <form onSubmit={handleRegisterTeam} className="flex flex-col sm:flex-row gap-2.5">
                      <select
                        aria-label="Оберіть команду для реєстрації"
                        value={effectiveSelectedTeamId}
                        onChange={(e) => setSelectedTeamId(e.target.value)}
                        className="flex-1 bg-[#121620] border border-[#263147] focus:border-orange-500 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none"
                      >
                        {eligibleTeams.map((team) => (
                          <option key={team.id} value={team.id}>
                            {team.name} ({team.memberCount}/{team.maxMembers})
                          </option>
                        ))}
                      </select>
                      <button
                        type="submit"
                        disabled={isRegistering || !effectiveSelectedTeamId}
                        className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition shrink-0 cursor-pointer"
                      >
                        {isRegistering ? 'Реєстрація...' : 'Зареєструвати команду'}
                      </button>
                    </form>
                  )}
                </div>

                {/* Organizer Moderation Toolbar (US-14) */}
                {isOrganizer && registrations && (
                  <div className="p-4 rounded-xl bg-[#0B0E14] border border-orange-500/30 flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <div className="text-xs font-bold text-white flex items-center gap-1.5">
                        <LockIcon className="w-3.5 h-3.5 text-orange-400" />
                        <span>Панель організатора (Roster Lock)</span>
                      </div>
                      <p className="text-[11px] text-slate-400 mt-0.5">
                        Фіксація створює незмінні зліпки ігрових ID усіх учасників турніру.
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={handleLockRosters}
                      disabled={
                        isLockingRosters ||
                        registrations.rostersLocked ||
                        registrations.teams.length === 0
                      }
                      className="px-3.5 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 disabled:bg-slate-800 disabled:text-slate-500 text-slate-950 text-xs font-bold transition flex items-center gap-1.5 cursor-pointer disabled:cursor-not-allowed"
                    >
                      <LockIcon className="w-3.5 h-3.5" />
                      <span>
                        {registrations.rostersLocked
                          ? 'Склади вже зафіксовано'
                          : isLockingRosters
                          ? 'Фіксація...'
                          : 'Зафіксувати склади (Roster Lock)'}
                      </span>
                    </button>
                  </div>
                )}

                {/* Registered Teams List */}
                <div className="space-y-2.5">
                  <h3 className="text-xs font-mono uppercase tracking-wider text-slate-400">
                    Зареєстровані команди
                  </h3>

                  {isLoadingRegistrations ? (
                    <div className="space-y-2">
                      {[1, 2].map((idx) => (
                        <div
                          key={idx}
                          className="h-14 rounded-xl bg-[#0B0E14] border border-[#1E2536] animate-pulse"
                        />
                      ))}
                    </div>
                  ) : registrationsError ? (
                    <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200">
                      {registrationsError.detail}
                    </div>
                  ) : !registrations || registrations.teams.length === 0 ? (
                    <div className="p-5 rounded-xl bg-[#0B0E14] border border-[#1E2536] text-center text-xs text-slate-500">
                      Поки що жодна команда не зареєструвалася на цей турнір.
                    </div>
                  ) : (
                    <div className="divide-y divide-[#1E2536] rounded-xl border border-[#1E2536] bg-[#0B0E14] overflow-hidden">
                      {registrations.teams.map((item) => (
                        <div
                          key={item.registrationId}
                          className="px-4 py-3 flex flex-wrap items-center justify-between gap-3"
                        >
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="text-sm font-bold text-white truncate">
                                {item.teamName}
                              </span>
                              {item.rosterLocked && (
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-amber-500/10 border border-amber-500/30 text-amber-300 text-[10px] font-mono">
                                  <LockIcon className="w-3 h-3" />
                                  <span>Locked</span>
                                </span>
                              )}
                            </div>
                            <div className="text-[11px] font-mono text-slate-400 mt-0.5">
                              {item.memberCount} гравців | {item.status}
                            </div>
                          </div>

                          {canModerate && (
                            <div className="flex items-center gap-2">
                              {pendingRemoveTeamId === item.teamId ? (
                                <>
                                  <button
                                    type="button"
                                    disabled={removingTeamId === item.teamId}
                                    onClick={() => handleConfirmRemoveTeam(item.teamId)}
                                    className="px-2.5 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white text-[11px] font-bold transition cursor-pointer"
                                  >
                                    {removingTeamId === item.teamId
                                      ? 'Видалення...'
                                      : 'Підтвердити видалення'}
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => setPendingRemoveTeamId(null)}
                                    className="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-[11px] font-semibold transition cursor-pointer"
                                  >
                                    Скасувати
                                  </button>
                                </>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() => setPendingRemoveTeamId(item.teamId)}
                                  className="px-2.5 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-300 text-[11px] font-semibold transition flex items-center gap-1 cursor-pointer"
                                >
                                  <TrashIcon className="w-3.5 h-3.5" />
                                  <span>Видалити</span>
                                </button>
                              )}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'schedule' && (
              <div className="space-y-6">
                {/* Slots Fill Bar */}
                <div className="p-4 bg-[#0B0E14] rounded-xl border border-[#1E2536] space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-400 font-medium">
                      Заповненість турнірної сітки:
                    </span>
                    <span className="font-mono font-semibold text-slate-200">
                      {registeredCount} / {slotLimit} слотів ({fillPercent}%)
                    </span>
                  </div>
                  <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                    <div
                      className="bg-orange-500 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${fillPercent}%` }}
                    />
                  </div>
                </div>

                {/* Timeline */}
                <div>
                  <h3 className="text-xs uppercase font-mono tracking-wider text-slate-400 mb-3">
                    Хронологія етапів
                  </h3>
                  <div className="space-y-3 relative border-l-2 border-slate-800 pl-4 ml-2">
                    <div className="relative">
                      <div className="absolute -left-[23px] top-1 h-3 w-3 rounded-full bg-slate-600 border-2 border-[#121620]" />
                      <div className="text-xs text-slate-500 font-mono">Етап 1</div>
                      <div className="text-sm font-bold text-slate-200 mt-0.5">
                        Дедлайн подачі заявок
                      </div>
                      <div className="text-xs font-mono text-slate-400 mt-0.5">
                        {formatDateTime(tournament.registrationDeadlineAt)}
                      </div>
                    </div>

                    <div className="relative pt-3">
                      <div className="absolute -left-[23px] top-4 h-3 w-3 rounded-full bg-amber-500 border-2 border-[#121620]" />
                      <div className="text-xs text-amber-500 font-mono">Етап 2</div>
                      <div className="text-sm font-bold text-slate-200 mt-0.5">
                        Відкриття вікна Check-in
                      </div>
                      <div className="text-xs font-mono text-amber-300 mt-0.5">
                        {formatDateTime(tournament.checkInOpensAt)}
                      </div>
                    </div>

                    <div className="relative pt-3">
                      <div className="absolute -left-[23px] top-4 h-3 w-3 rounded-full bg-orange-500 border-2 border-[#121620]" />
                      <div className="text-xs text-orange-400 font-mono">Етап 3</div>
                      <div className="text-sm font-bold text-slate-100 mt-0.5">
                        Початок перших матчів
                      </div>
                      <div className="text-xs font-mono text-orange-400 font-bold mt-0.5">
                        {formatDateTime(tournament.startTimeAt)}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'server' && (
              <div className="space-y-6">
                {/* CS2 Server Connect Section */}
                {tournament.discipline === 'CS2' && (
                  <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
                        <ServerIcon className="w-4 h-4" />
                        <span>Підключення до CS2 сервера</span>
                      </span>
                      <span className="text-[11px] text-slate-500 font-mono">Dedicated Server</span>
                    </div>

                    {tournament.cs2ServerConnect ? (
                      <div className="space-y-2">
                        <div className="p-3 bg-black/80 rounded-lg border border-amber-900/40 font-mono text-xs text-amber-300 select-all break-all">
                          {tournament.cs2ServerConnect}
                        </div>
                        <button
                          type="button"
                          onClick={handleCopyConnect}
                          className="w-full py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition flex items-center justify-center gap-1.5"
                        >
                          {copiedConnect ? (
                            <CheckIcon className="w-3.5 h-3.5 text-emerald-400" />
                          ) : (
                            <CopyIcon className="w-3.5 h-3.5" />
                          )}
                          <span>
                            {copiedConnect ? 'Команду connect скопійовано!' : 'Копіювати рядок connect'}
                          </span>
                        </button>
                      </div>
                    ) : (
                      <p className="text-xs text-slate-500 italic">
                        Сервер ще не налаштовано. Організатор опублікує IP після початку матчів.
                      </p>
                    )}
                  </div>
                )}

                {/* Map Pool Section */}
                <div>
                  <h3 className="text-xs uppercase font-mono tracking-wider text-slate-400 mb-2">
                    Офіційний пул карт змагань
                  </h3>
                  {maps.length > 0 ? (
                    <div className="grid grid-cols-2 gap-2">
                      {maps.map((mapName, idx) => (
                        <div
                          key={mapName}
                          className="p-3 rounded-lg bg-[#0B0E14] border border-[#1E2536] flex items-center justify-between"
                        >
                          <span className="font-mono text-xs text-slate-200 font-semibold">
                            {mapName}
                          </span>
                          <span className="text-[10px] font-mono text-slate-500">#{idx + 1}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] text-xs text-slate-400">
                      Для дисципліни Chess.com класичний мап-пул відсутній — змагання проходять на
                      дошці 8х8 за регламентом FIDE / Chess.com Rapid.
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Drawer Footer CTA */}
          <div className="p-5 border-t border-[#1E2536] bg-[#0E121A] flex items-center justify-between">
            <span className="text-xs text-slate-400">
              Поділіться турніром або перевірте статус реєстрації
            </span>
            <button
              type="button"
              onClick={handleCopyLink}
              className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition flex items-center gap-1.5 shadow-lg shadow-orange-500/20"
            >
              <CopyIcon className="w-3.5 h-3.5" />
              <span>{copiedLink ? 'Посилання скопійовано!' : 'Поділитися'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

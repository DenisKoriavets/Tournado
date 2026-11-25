import { useCallback, useEffect, useMemo, useState } from 'react'
import { getTournamentById, getTournaments } from './api/tournaments'
import { AuthModal } from './components/AuthModal'
import { DisciplineLogo } from './components/DisciplineLogo'
import { JoinInviteModal } from './components/JoinInviteModal'
import { MatchRoomModal } from './components/MatchRoomModal'
import { ProfileModal } from './components/ProfileModal'
import { TeamsDrawer } from './components/TeamsDrawer'
import { TournamentDrawer } from './components/TournamentDrawer'
import { TournamentFormModal } from './components/TournamentFormModal'
import { TournamentList } from './components/TournamentList'
import {
  AlertCircleIcon,
  CheckIcon,
  ChevronRightIcon,
  ClockIcon,
  CloseIcon,
  GamepadIcon,
  LogInIcon,
  LogOutIcon,
  PlusIcon,
  TrophyIcon,
  UserIcon,
  UsersIcon,
  ZapIcon
} from './components/icons/Icons'
import { useAuth } from './context/AuthContext'
import { AuthProvider } from './context/AuthProvider'
import type { TeamDetails } from './types/teams'
import { DISCIPLINE_CONFIG, type Tournament } from './types/tournament'
import { formatRelativeTimeUntil } from './utils/date'

function readJoinTokenFromPath(): string | null {
  if (typeof window === 'undefined') return null
  const match = window.location.pathname.match(/^\/join\/([^/]+)\/?$/)
  return match ? decodeURIComponent(match[1]) : null
}

// Deterministic Match Room fixtures from V4__seed_match_room_qa_data.sql, used for manual QA
// of Ready Check / Map Veto (US-22, US-23, US-25, US-26) ahead of bracket generation (US-18..21).
const QA_MATCH_OPTIONS: { label: string; matchId: string }[] = [
  { label: 'CS2 — Map Veto', matchId: '22222222-0005-4000-8000-000000000001' },
  { label: 'Brawl Stars — Map Veto', matchId: '22222222-0005-4000-8000-000000000002' },
  { label: 'Chess.com — авто-пропуск', matchId: '22222222-0005-4000-8000-000000000003' }
]

export default function App() {
  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  )
}

function AppShell() {
  const {
    user,
    isLoading: isAuthLoading,
    isAuthModalOpen,
    authModalPrompt,
    authErrorNotice,
    openAuthModal,
    closeAuthModal,
    refreshUser,
    logout,
    clearAuthErrorNotice
  } = useAuth()

  const [tournaments, setTournaments] = useState<Tournament[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [apiWarning, setApiWarning] = useState<string | null>(null)

  const [isFormOpen, setIsFormOpen] = useState<boolean>(false)
  const [isProfileOpen, setIsProfileOpen] = useState<boolean>(false)
  const [isTeamsOpen, setIsTeamsOpen] = useState<boolean>(false)
  const [initialTeamId, setInitialTeamId] = useState<string | null>(null)
  const [joinToken, setJoinToken] = useState<string | null>(() => readJoinTokenFromPath())
  const [viewingMatchId, setViewingMatchId] = useState<string | null>(null)
  const [isMatchMenuOpen, setIsMatchMenuOpen] = useState<boolean>(false)

  const [selectedTournament, setSelectedTournament] = useState<Tournament | null>(null)
  const [notification, setNotification] = useState<{
    message: string
    type: 'success' | 'info'
  } | null>(null)

  const showToast = useCallback((message: string, type: 'success' | 'info' = 'success') => {
    setNotification({ message, type })
    setTimeout(() => setNotification(null), 6000)
  }, [])

  // Load tournaments from real PostgreSQL database
  const loadTournaments = useCallback(async () => {
    setIsLoading(true)
    setApiWarning(null)
    try {
      const data = await getTournaments()
      setTournaments(Array.isArray(data) ? data : [])
    } catch {
      setApiWarning('Не вдалося завантажити список турнірів. Перевірте статус з’єднання з Core API.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    getTournaments()
      .then((data) => {
        if (!cancelled) {
          setTournaments(Array.isArray(data) ? data : [])
          setApiWarning(null)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setApiWarning(
            'Не вдалося завантажити список турнірів. Перевірте статус з’єднання з Core API.'
          )
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const handleSelectTournament = async (t: Tournament) => {
    setSelectedTournament(t)
    try {
      const full = await getTournamentById(t.id)
      setSelectedTournament(full)
    } catch {
      // If fetching full details fails, retain summary
    }
  }

  const handleRegistrationsChanged = useCallback(async () => {
    try {
      const list = await getTournaments()
      if (Array.isArray(list)) {
        setTournaments(list)
      }
      if (selectedTournament) {
        const updated = await getTournamentById(selectedTournament.id)
        setSelectedTournament(updated)
      }
    } catch {
      // silent background sync failure
    }
  }, [selectedTournament])

  const handleCreateTournamentClick = () => {
    if (!user) {
      openAuthModal('Увійдіть через Steam або пошту, щоб створити турнір.')
      return
    }
    setIsFormOpen(true)
  }

  const handleOpenTeamsClick = () => {
    if (!user) {
      openAuthModal('Увійдіть через Steam або пошту, щоб переглядати та створювати команди.')
      return
    }
    setInitialTeamId(null)
    setIsTeamsOpen(true)
  }

  const handleOpenQaMatch = (matchId: string) => {
    if (!user) {
      openAuthModal('Увійдіть, щоб переглянути Match Room.')
      return
    }
    setViewingMatchId(matchId)
    setIsMatchMenuOpen(false)
  }

  const handleCloseJoinInvite = () => {
    setJoinToken(null)
    if (typeof window !== 'undefined' && window.location.pathname.startsWith('/join/')) {
      window.history.replaceState({}, '', '/')
    }
  }

  const handleJoinedTeam = (team: TeamDetails) => {
    handleCloseJoinInvite()
    setInitialTeamId(team.id)
    setIsTeamsOpen(true)
    showToast(`Ви успішно приєдналися до команди «${team.name}»!`)
  }

  const handleTournamentCreated = (newTournament: Tournament) => {
    setTournaments((prev) => [newTournament, ...prev])
    setSelectedTournament(newTournament)
    showToast(`Турнір «${newTournament.name}» успішно створено та збережено в базі даних!`)
  }

  const handleFormUnauthorized = async () => {
    setIsFormOpen(false)
    await refreshUser()
    openAuthModal('Сесія завершилась. Увійдіть повторно, щоб створити турнір.')
  }

  const handleLogout = async () => {
    await logout()
    setIsProfileOpen(false)
    setIsTeamsOpen(false)
    showToast('Ви вийшли з облікового запису.', 'info')
  }

  const nextTournament = useMemo(() => {
    const activeOrUpcoming = tournaments
      .filter((t) => t.status !== 'COMPLETED')
      .sort((a, b) => new Date(a.startTimeAt).getTime() - new Date(b.startTimeAt).getTime())
    return activeOrUpcoming[0] ?? null
  }, [tournaments])

  return (
    <div className="relative min-h-screen bg-[#0B0E14] text-slate-100 flex flex-col justify-between selection:bg-orange-500 selection:text-white overflow-x-clip">
      {/* Subtle Ambient Broadcast Backdrop */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-[420px] arena-grid-bg opacity-70"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-[420px] bg-[radial-gradient(ellipse_at_top,rgba(249,115,22,0.12),transparent_60%)]"
      />

      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-40 bg-[#0E121A]/90 backdrop-blur-md border-b border-[#1E2536]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          {/* Brand Logo & Navigation */}
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-orange-500 flex items-center justify-center text-white shadow-lg shadow-orange-500/25">
                <TrophyIcon className="w-5 h-5" />
              </div>
              <div className="flex flex-col">
                <span className="font-extrabold text-lg tracking-tight text-white leading-none">
                  TOURNADO
                </span>
                <span className="text-[10px] font-mono tracking-widest text-orange-400 font-semibold uppercase mt-0.5">
                  Esports Platform
                </span>
              </div>
            </div>

            {/* Navigation links */}
            <nav className="hidden md:flex items-center gap-1 text-xs font-semibold">
              <a
                href="#tournaments"
                className="px-3 py-1.5 rounded-lg bg-[#181F2E] text-orange-400 border border-[#263147]"
              >
                Турніри
              </a>
              <button
                type="button"
                onClick={handleOpenTeamsClick}
                className="px-3 py-1.5 rounded-lg text-slate-300 hover:text-white hover:bg-[#181F2E] transition flex items-center gap-1.5 cursor-pointer"
              >
                <UsersIcon className="w-3.5 h-3.5 text-orange-400" />
                <span>Мої команди</span>
              </button>
              <div className="hidden lg:block relative">
                <button
                  type="button"
                  onClick={() => setIsMatchMenuOpen((prev) => !prev)}
                  className="px-3 py-1.5 rounded-lg text-slate-300 hover:text-white hover:bg-[#181F2E] transition flex items-center gap-1.5 cursor-pointer"
                >
                  <GamepadIcon className="w-3.5 h-3.5 text-orange-400" />
                  <span>Матчі</span>
                  <span className="text-[9px] font-mono bg-slate-800 text-slate-400 px-1.5 py-0.2 rounded">
                    QA
                  </span>
                </button>
                {isMatchMenuOpen && (
                  <>
                    <div className="fixed inset-0 z-40" onClick={() => setIsMatchMenuOpen(false)} />
                    <div className="absolute z-50 top-full left-0 mt-1 w-56 rounded-xl bg-[#121620] border border-[#1E2536] shadow-2xl overflow-hidden animate-fadeIn">
                      <div className="px-3 py-2 text-[10px] font-mono uppercase tracking-wider text-slate-500 border-b border-[#1E2536]">
                        Швидкий перегляд Match Room
                      </div>
                      {QA_MATCH_OPTIONS.map((option) => (
                        <button
                          key={option.matchId}
                          type="button"
                          onClick={() => handleOpenQaMatch(option.matchId)}
                          className="w-full text-left px-3 py-2 text-xs text-slate-200 hover:bg-[#181F2E] hover:text-orange-400 transition cursor-pointer"
                        >
                          {option.label}
                        </button>
                      ))}
                    </div>
                  </>
                )}
              </div>
              <span className="hidden lg:inline-block px-3 py-1.5 text-slate-500 cursor-not-allowed">
                Рейтинги
              </span>
            </nav>
          </div>

          {/* Header Action Buttons */}
          <div className="flex items-center gap-2.5">
            <button
              type="button"
              onClick={handleOpenTeamsClick}
              className="md:hidden px-3 py-2 rounded-xl bg-[#181F2E] border border-[#263147] text-slate-200 text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
              aria-label="Мої команди"
            >
              <UsersIcon className="w-4 h-4 text-orange-400" />
              <span className="hidden sm:inline">Команди</span>
            </button>

            {user ? (
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsProfileOpen(true)}
                  className="px-3 py-2 rounded-xl bg-[#181F2E] hover:bg-[#20293D] border border-[#263147] text-slate-100 text-xs font-semibold transition flex items-center gap-2 cursor-pointer"
                >
                  <UserIcon className="w-4 h-4 text-orange-400" />
                  <span className="max-w-[130px] truncate">{user.displayName}</span>
                </button>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="p-2 rounded-xl bg-[#121620] hover:bg-slate-800 border border-[#1E2536] text-slate-400 hover:text-white transition cursor-pointer"
                  aria-label="Вийти з акаунту"
                  title="Вийти"
                >
                  <LogOutIcon className="w-4 h-4" />
                </button>
              </div>
            ) : (
              !isAuthLoading && (
                <button
                  type="button"
                  onClick={() => openAuthModal()}
                  className="px-3.5 py-2 rounded-xl bg-[#181F2E] hover:bg-[#20293D] border border-[#263147] text-slate-100 text-xs font-bold transition flex items-center gap-1.5 cursor-pointer"
                >
                  <LogInIcon className="w-4 h-4 text-orange-400" />
                  <span>Увійти</span>
                </button>
              )
            )}

            <button
              type="button"
              onClick={handleCreateTournamentClick}
              className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition flex items-center gap-2 shadow-lg shadow-orange-500/25 hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
            >
              <PlusIcon className="w-4 h-4" />
              <span>Створити турнір</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Body */}
      <main className="relative z-10 flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Steam Cancelled / Auth Error Notice Banner */}
        {authErrorNotice && (
          <div
            role="alert"
            className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-200 flex items-center justify-between gap-3 animate-fadeIn"
          >
            <div className="flex items-center gap-3 text-xs">
              <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0" />
              <span className="font-medium">{authErrorNotice}</span>
            </div>
            <button
              type="button"
              onClick={clearAuthErrorNotice}
              className="text-rose-400 hover:text-rose-200 transition"
              aria-label="Закрити попередження авторизації"
            >
              <CloseIcon className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Notification Toast */}
        {notification && (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 flex items-center justify-between animate-fadeIn">
            <div className="flex items-center gap-3 text-xs">
              <div className="p-1 rounded-full bg-emerald-500/20 text-emerald-400">
                <CheckIcon className="w-4 h-4" />
              </div>
              <span className="font-medium">{notification.message}</span>
            </div>
            <button
              type="button"
              onClick={() => setNotification(null)}
              className="text-emerald-400 hover:text-emerald-200 transition"
              aria-label="Закрити сповіщення"
            >
              <CloseIcon className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Page Hero Banner */}
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-2">
          <div>
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-lg bg-orange-500/10 border border-orange-500/20 text-orange-400 text-[11px] font-mono font-semibold mb-2">
              <ZapIcon className="w-3.5 h-3.5" />
              <span>Офіційні змагання 2026</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Кіберспортивні турніри
            </h1>
            <p className="text-xs sm:text-sm text-slate-400 mt-1 max-w-2xl leading-relaxed">
              Автоматизована платформа проведення змагань для Counter-Strike 2, Brawl Stars та
              Chess.com. Створюйте сітки Single Elimination та відкривайте доступ для реєстрації
              команд.
            </p>
          </div>

          {nextTournament ? (
            <button
              type="button"
              onClick={() => handleSelectTournament(nextTournament)}
              className="group text-left bg-[#121620]/90 hover:bg-[#161B28] border border-[#1E2536] hover:border-orange-500/40 rounded-2xl p-3.5 transition-all duration-300 flex items-center gap-3.5 shadow-lg shadow-black/25 hover:-translate-y-0.5 cursor-pointer lg:min-w-[330px] max-w-md"
            >
              <DisciplineLogo
                discipline={nextTournament.discipline}
                className="w-11 h-11 rounded-xl shrink-0 transition-transform duration-300 group-hover:scale-105"
              />
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <span className="inline-flex items-center gap-1.5 text-[10px] font-mono uppercase tracking-wider text-orange-400 font-semibold">
                    <span className="h-1.5 w-1.5 rounded-full bg-orange-400 animate-pulse" />
                    Найближчий старт
                  </span>
                  <span className="inline-flex items-center gap-1 text-[10px] font-mono text-slate-400">
                    <ClockIcon className="w-3 h-3" />
                    {formatRelativeTimeUntil(nextTournament.startTimeAt)}
                  </span>
                </div>
                <div className="text-sm font-bold text-white truncate group-hover:text-orange-400 transition-colors mt-0.5">
                  {nextTournament.name}
                </div>
                <div className="text-[11px] font-mono text-slate-400 mt-0.5">
                  {DISCIPLINE_CONFIG[nextTournament.discipline].shortName} |{' '}
                  {nextTournament.slotLimit} слотів
                </div>
              </div>
              <ChevronRightIcon className="w-4 h-4 text-slate-500 group-hover:text-orange-400 group-hover:translate-x-0.5 transition-all shrink-0" />
            </button>
          ) : (
            <div className="flex items-center gap-2 text-xs font-mono text-slate-400 bg-[#121620] border border-[#1E2536] px-3.5 py-2 rounded-xl self-start lg:self-auto">
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
              <span>Всього турнірів:</span>
              <span className="font-bold text-white">{tournaments.length}</span>
            </div>
          )}
        </div>

        {/* Tournament Browsing Grid & Filter */}
        <section id="tournaments" className="space-y-6">
          <TournamentList
            tournaments={tournaments}
            isLoading={isLoading}
            error={apiWarning}
            onSelectTournament={handleSelectTournament}
            onCreateClick={handleCreateTournamentClick}
            onRetry={loadTournaments}
          />
        </section>
      </main>

      {/* Clean Minimalist Footer */}
      <footer className="border-t border-[#1E2536] bg-[#0E121A] py-6 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex items-center justify-between">
          <div>
            <span className="font-bold text-slate-400">Tournado</span> — Платформа кіберспортивних
            турнірів © 2026
          </div>
        </div>
      </footer>

      {/* Auth Modal */}
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={closeAuthModal}
        promptMessage={authModalPrompt}
      />

      {/* Profile Modal */}
      <ProfileModal
        isOpen={isProfileOpen}
        onClose={() => setIsProfileOpen(false)}
        onProfileUpdated={() => showToast('Ігрові ідентифікатори профілю оновлено.')}
      />

      {/* Teams Slide-Over Drawer */}
      <TeamsDrawer
        isOpen={isTeamsOpen}
        onClose={() => setIsTeamsOpen(false)}
        initialTeamId={initialTeamId}
      />

      {/* Join Invite Modal */}
      <JoinInviteModal
        token={joinToken}
        onClose={handleCloseJoinInvite}
        onJoined={handleJoinedTeam}
      />

      {/* Creation Modal Wizard */}
      <TournamentFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSuccess={handleTournamentCreated}
        onUnauthorized={handleFormUnauthorized}
      />

      {/* Slide-over Details Drawer */}
      <TournamentDrawer
        tournament={selectedTournament}
        isOpen={Boolean(selectedTournament)}
        onClose={() => setSelectedTournament(null)}
        onRegistrationsChanged={handleRegistrationsChanged}
      />

      {/* Match Room Modal (Ready Check / Map Veto) */}
      <MatchRoomModal matchId={viewingMatchId} onClose={() => setViewingMatchId(null)} />
    </div>
  )
}

import { useCallback, useEffect, useState } from 'react'
import { getTournamentById, getTournaments } from './api/tournaments'
import { TournamentCard } from './components/TournamentCard'
import { TournamentDrawer } from './components/TournamentDrawer'
import { TournamentFormModal } from './components/TournamentFormModal'
import { TournamentList } from './components/TournamentList'
import {
  AlertCircleIcon,
  CheckIcon,
  CloseIcon,
  PlusIcon,
  TrophyIcon,
  ZapIcon
} from './components/icons/Icons'
import type { Tournament } from './types/tournament'

export default function App() {
  const [tournaments, setTournaments] = useState<Tournament[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [apiWarning, setApiWarning] = useState<string | null>(null)

  const [isFormOpen, setIsFormOpen] = useState<boolean>(false)
  const [selectedTournament, setSelectedTournament] = useState<Tournament | null>(null)
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'info' } | null>(null)

  // Load tournaments from real PostgreSQL database
  const loadTournaments = useCallback(async () => {
    setIsLoading(true)
    setApiWarning(null)
    try {
      const data = await getTournaments()
      if (Array.isArray(data)) {
        setTournaments(data)
      } else {
        setTournaments([])
      }
    } catch {
      setApiWarning('Не вдалося завантажити список турнірів. Перевірте статус з’єднання з Core API.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadTournaments()
  }, [loadTournaments])

  // Select tournament and optionally fetch full details
  const handleSelectTournament = async (t: Tournament) => {
    setSelectedTournament(t)
    try {
      const full = await getTournamentById(t.id)
      setSelectedTournament(full)
    } catch {
      // If fetching full details fails, retain summary
    }
  }

  // Real-time refresh & handler upon tournament creation
  const handleTournamentCreated = (newTournament: Tournament) => {
    setTournaments((prev) => [newTournament, ...prev])
    setSelectedTournament(newTournament)
    setNotification({
      message: `Турнір «${newTournament.name}» успішно створено та збережено в базі даних!`,
      type: 'success'
    })
    setTimeout(() => setNotification(null), 6000)
  }

  return (
    <div className="min-h-screen bg-[#0B0E14] text-slate-100 flex flex-col justify-between selection:bg-orange-500 selection:text-white">
      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-40 bg-[#0E121A]/90 backdrop-blur-md border-b border-[#1E2536]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
          {/* Brand Logo & Navigation */}
          <div className="flex items-center gap-8">
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
              <span className="px-3 py-1.5 text-slate-500 cursor-not-allowed flex items-center gap-1.5">
                <span>Матчі</span>
                <span className="text-[9px] font-mono bg-slate-800 text-slate-400 px-1.5 py-0.2 rounded">
                  Незабаром
                </span>
              </span>
              <span className="px-3 py-1.5 text-slate-500 cursor-not-allowed">
                Рейтинги
              </span>
            </nav>
          </div>

          {/* Header Action Button */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setIsFormOpen(true)}
              className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition flex items-center gap-2 shadow-lg shadow-orange-500/25 hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
            >
              <PlusIcon className="w-4 h-4" />
              <span>Створити турнір</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Body */}
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8 space-y-8">
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
              onClick={() => setNotification(null)}
              className="text-emerald-400 hover:text-emerald-200 transition"
              aria-label="Закрити сповіщення"
            >
              <CloseIcon className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Page Hero Banner */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-2">
          <div>
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-lg bg-orange-500/10 border border-orange-500/20 text-orange-400 text-[11px] font-mono font-semibold mb-2">
              <ZapIcon className="w-3.5 h-3.5" />
              <span>Офіційні змагання 2026</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Кіберспортивні турніри
            </h1>
            <p className="text-xs sm:text-sm text-slate-400 mt-1 max-w-2xl leading-relaxed">
              Автоматизована платформа проведення змагань для Counter-Strike 2, Brawl Stars та Chess.com.
              Створюйте сітки Single Elimination та відкривайте доступ для реєстрації команд.
            </p>
          </div>

          <div className="flex items-center gap-2 text-xs font-mono text-slate-400 bg-[#121620] border border-[#1E2536] px-3.5 py-2 rounded-xl">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Всього турнірів:</span>
            <span className="font-bold text-white">{tournaments.length}</span>
          </div>
        </div>

        {/* Tournament Browsing Grid & Filter */}
        <section id="tournaments" className="space-y-6">
          <TournamentList
            tournaments={tournaments}
            isLoading={isLoading}
            error={apiWarning}
            onSelectTournament={handleSelectTournament}
            onCreateClick={() => setIsFormOpen(true)}
            onRetry={loadTournaments}
          />
        </section>
      </main>

      {/* Clean Minimalist Footer */}
      <footer className="border-t border-[#1E2536] bg-[#0E121A] py-6 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex items-center justify-between">
          <div>
            <span className="font-bold text-slate-400">Tournado</span> — Платформа кіберспортивних турнірів © 2026
          </div>
        </div>
      </footer>

      {/* Creation Modal Wizard */}
      <TournamentFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSuccess={handleTournamentCreated}
      />

      {/* Slide-over Details Drawer */}
      <TournamentDrawer
        tournament={selectedTournament}
        isOpen={Boolean(selectedTournament)}
        onClose={() => setSelectedTournament(null)}
      />
    </div>
  )
}

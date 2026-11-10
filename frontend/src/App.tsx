import { useEffect, useState, useCallback } from 'react'
import type { Tournament } from './types/tournament'
import { getTournaments, getTournamentById } from './api/tournaments'
import { TournamentList } from './components/TournamentList'
import { TournamentForm } from './components/TournamentForm'
import { TournamentDetailsModal } from './components/TournamentDetailsModal'

interface HealthResponse {
  status: string
  service: string
  database: string
  timestamp: string
}

const PLANNED_SCREENS = [
  { step: 'Крок 1', title: 'Створення та конфігурація турніру', stories: 'US-07..US-11', desc: 'Вибір CS2 / Brawl Stars / Chess.com, Single Elimination (8/16/32), графік і пул карт.', current: true },
  { step: 'Крок 2', title: 'Управління командами та реєстрація', stories: 'US-01..US-06, US-12..US-14', desc: 'Вхід через Steam або пошту, створення ростерів за інвайт-лінком та автоматичний Roster Lock.' },
  { step: 'Крок 3', title: 'Вікно Check-in та генерація сітки', stories: 'US-15..US-21', desc: 'Автоматичне відсіювання «мертвих душ», сліпий посів і призначення технічних проходів (Bye).' },
  { step: 'Крок 4', title: 'Match Room та ШІ-арбітраж спорів', stories: 'US-22..US-34', desc: 'Ready Check (10 хв), почерговий бан карт (30 с), авто-імпорт Chess.com та Vision AI для скріншотів.' },
  { step: 'Крок 5', title: 'Фіналізація та експорт у CSV', stories: 'US-35..US-36', desc: 'Підсумковий розподіл призових місць та вивантаження офіційного звіту для студради чи спонсорів.' }
]

const DEMO_TOURNAMENTS: Tournament[] = [
  {
    id: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
    publicSlug: 'kma-cs2-autumn-cup-2026',
    name: 'KMA CS2 Autumn Cup 2026',
    description: 'Офіційний осінній кубок факультету інформатики НаУКМА з Counter-Strike 2. Правила овертайму: MR3 $10k.',
    discipline: 'CS2',
    teamSize: 5,
    format: 'SINGLE_ELIMINATION',
    slotLimit: 16,
    status: 'REGISTRATION_OPEN',
    registrationDeadlineAt: new Date(Date.now() + 24 * 3600 * 1000).toISOString(),
    checkInOpensAt: new Date(Date.now() + 26 * 3600 * 1000).toISOString(),
    startTimeAt: new Date(Date.now() + 27 * 3600 * 1000).toISOString(),
    cs2ServerConnect: 'connect 194.44.12.5:27015; password kma_cup',
    mapPool: ['de_mirage', 'de_inferno', 'de_nuke', 'de_ancient', 'de_anubis', 'de_dust2', 'de_vertigo'],
    registeredTeamsCount: 6,
    createdAt: new Date().toISOString()
  },
  {
    id: 'e71ca289-432d-42bc-9d29-c89b8849ad01',
    publicSlug: 'brawl-stars-kma-showdown',
    name: 'Brawl Stars KMA Showdown',
    description: 'Студентський турнір 3v3 у режимі дружніх боїв (Friendly Battle). Швидкі динамічні матчі.',
    discipline: 'BRAWL_STARS',
    teamSize: 3,
    format: 'SINGLE_ELIMINATION',
    slotLimit: 8,
    status: 'REGISTRATION_OPEN',
    registrationDeadlineAt: new Date(Date.now() + 48 * 3600 * 1000).toISOString(),
    checkInOpensAt: new Date(Date.now() + 50 * 3600 * 1000).toISOString(),
    startTimeAt: new Date(Date.now() + 51 * 3600 * 1000).toISOString(),
    mapPool: ['Shooting Star', 'Backyard Bowl', 'Hard Rock Mine', 'Dueling Beetles', 'Ring of Fire'],
    registeredTeamsCount: 3,
    createdAt: new Date().toISOString()
  },
  {
    id: 'f1a9b2c3-d4e5-4f6a-8b9c-0d1e2f3a4b5c',
    publicSlug: 'chess-autumn-rapid-kma',
    name: 'Chess.com Autumn Rapid',
    description: 'Класичний рапід турнір 1v1 (контроль часу 10+0) на платформі Chess.com з автоматичним імпортом результатів матчів.',
    discipline: 'CHESS',
    teamSize: 1,
    format: 'SINGLE_ELIMINATION',
    slotLimit: 32,
    status: 'REGISTRATION_OPEN',
    registrationDeadlineAt: new Date(Date.now() + 72 * 3600 * 1000).toISOString(),
    checkInOpensAt: new Date(Date.now() + 74 * 3600 * 1000).toISOString(),
    startTimeAt: new Date(Date.now() + 75 * 3600 * 1000).toISOString(),
    mapPool: [],
    registeredTeamsCount: 12,
    createdAt: new Date().toISOString()
  }
]

export default function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [healthError, setHealthError] = useState<boolean>(false)

  const [tournaments, setTournaments] = useState<Tournament[]>(DEMO_TOURNAMENTS)
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [apiWarning, setApiWarning] = useState<string | null>(null)

  const [isFormOpen, setIsFormOpen] = useState<boolean>(false)
  const [selectedTournament, setSelectedTournament] = useState<Tournament | null>(null)
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'info' } | null>(null)

  // Fetch /api/health
  useEffect(() => {
    fetch('/api/health')
      .then((res) => {
        if (!res.ok) throw new Error('Health check failed')
        return res.json()
      })
      .then((data: HealthResponse) => {
        setHealth(data)
        setHealthError(false)
      })
      .catch(() => setHealthError(true))
  }, [])

  // Load tournaments from API
  const loadTournaments = useCallback(async () => {
    setIsLoading(true)
    setApiWarning(null)
    try {
      const data = await getTournaments()
      if (Array.isArray(data) && data.length > 0) {
        setTournaments(data)
      } else if (Array.isArray(data) && data.length === 0) {
        // Backend returned empty list, show empty
        setTournaments([])
      }
    } catch {
      // Backend not yet running: gracefully retain demo tournaments
      setApiWarning('Core API (/api/v1/tournaments) очікує старту бекенду. Відображаються демонстраційні турніри.')
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
      message: `Турнір «${newTournament.name}» успішно створено! Публічне посилання згенеровано.`,
      type: 'success'
    })
    setTimeout(() => setNotification(null), 6000)
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between">
      <div className="p-6 md:p-8 max-w-7xl mx-auto w-full space-y-8">
        {/* Top Header */}
        <header className="border-b border-slate-800 pb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-indigo-400 bg-indigo-950/60 px-3 py-1 rounded-full border border-indigo-800">
                Vertical Slice 1 • US-07..US-11
              </span>
              <span className="text-xs font-medium text-emerald-400 bg-emerald-950/40 px-2.5 py-0.5 rounded-full border border-emerald-800/60">
                Production Ready
              </span>
            </div>
            <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight mt-3 text-white flex items-center gap-3">
              <span>🌪️ Tournado Tournaments</span>
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-2xl">
              Автоматизація кіберспортивних турнірів для CS2, Brawl Stars та Chess.com.
              Створення сіток Single Elimination та публічні сторінки змагань.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            {/* Health status badge */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-3 sm:min-w-[240px]">
              <div className="text-[11px] text-slate-400 uppercase font-medium mb-1">Статус ядра (/api/health)</div>
              {health ? (
                <div className="space-y-0.5 text-xs">
                  <div className="flex items-center gap-2 text-emerald-400 font-semibold">
                    <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                    Backend: {health.status} | DB: {health.database}
                  </div>
                  <div className="text-[10px] text-slate-500 truncate">{health.timestamp}</div>
                </div>
              ) : healthError ? (
                <div className="text-xs text-amber-400 font-medium flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-amber-400" />
                  <span>Очікування з’єднання з Core API...</span>
                </div>
              ) : (
                <div className="text-xs text-slate-400">Перевірка зв’язку...</div>
              )}
            </div>

            {/* Primary Action Button */}
            <button
              type="button"
              onClick={() => setIsFormOpen(true)}
              className="px-5 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold transition flex items-center justify-center gap-2 shadow-lg shadow-indigo-600/30 hover:scale-[1.02] active:scale-[0.98] cursor-pointer"
            >
              <span className="text-lg">🏆</span>
              <span>Створити турнір</span>
            </button>
          </div>
        </header>

        {/* Success / Notification Toast */}
        {notification && (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 flex items-center justify-between animate-fadeIn">
            <div className="flex items-center gap-3 text-sm">
              <span className="text-xl">🎉</span>
              <span className="font-medium">{notification.message}</span>
            </div>
            <button
              onClick={() => setNotification(null)}
              className="text-emerald-400 hover:text-emerald-200 text-sm font-bold"
            >
              ✕
            </button>
          </div>
        )}

        {/* Main Tournament Browsing Section */}
        <section className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <span>Список активних турнірів</span>
                <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                  {tournaments.length}
                </span>
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Публічний перегляд доступний без авторизації (US-11). Натисніть на картку для перегляду деталей.
              </p>
            </div>
          </div>

          <TournamentList
            tournaments={tournaments}
            isLoading={isLoading}
            error={apiWarning}
            onSelectTournament={handleSelectTournament}
            onCreateClick={() => setIsFormOpen(true)}
            onRetry={loadTournaments}
          />
        </section>

        {/* MVP Architecture Roadmap / Context */}
        <section className="pt-8 border-t border-slate-800/80">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-white">Карта вертикальних зрізів MVP (In Scope)</h2>
            <span className="text-xs text-indigo-400 font-medium">36 атомарних User Stories</span>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            {PLANNED_SCREENS.map((screen) => (
              <div
                key={screen.step}
                className={`rounded-xl p-4 transition ${
                  screen.current
                    ? 'bg-indigo-950/40 border-2 border-indigo-500/60 shadow-lg shadow-indigo-500/5'
                    : 'bg-slate-900/40 border border-slate-800 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between text-xs mb-1">
                  <span className={screen.current ? 'text-indigo-300 font-bold' : 'text-slate-400 font-semibold'}>
                    {screen.step}
                  </span>
                  {screen.current && (
                    <span className="px-1.5 py-0.2 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded text-[10px] font-bold">
                      Active
                    </span>
                  )}
                </div>
                <div className="font-mono text-[10px] text-slate-500 mb-1.5">{screen.stories}</div>
                <h3 className="text-xs font-bold text-slate-200 mb-1">{screen.title}</h3>
                <p className="text-[11px] text-slate-400 leading-snug line-clamp-3">{screen.desc}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-slate-950 py-6 text-center text-xs text-slate-600">
        Tournado Platform © 2026 • NaUKMA Capstone Project • Single Elimination CS2 / Brawl Stars / Chess.com
      </footer>

      {/* Creation Modal */}
      <TournamentForm
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSuccess={handleTournamentCreated}
      />

      {/* Details Modal */}
      <TournamentDetailsModal
        tournament={selectedTournament}
        isOpen={Boolean(selectedTournament)}
        onClose={() => setSelectedTournament(null)}
      />
    </div>
  )
}

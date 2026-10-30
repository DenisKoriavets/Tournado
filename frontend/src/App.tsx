import { useEffect, useState } from 'react'

interface HealthResponse {
  status: string
  service: string
  database: string
  timestamp: string
}

const PLANNED_SCREENS = [
  { step: 'Крок 1', title: 'Створення та конфігурація турніру', stories: 'US-07..US-11', desc: 'Вибір CS2 / Brawl Stars / Chess.com, Single Elimination (8/16/32), графік і пул карт.' },
  { step: 'Крок 2', title: 'Управління командами та реєстрація', stories: 'US-01..US-06, US-12..US-14', desc: 'Вхід через Steam або пошту, створення ростерів за інвайт-лінком та автоматичний Roster Lock.' },
  { step: 'Крок 3', title: 'Вікно Check-in та генерація сітки', stories: 'US-15..US-21', desc: 'Автоматичне відсіювання «мертвих душ», сліпий посів і призначення технічних проходів (Bye).' },
  { step: 'Крок 4', title: 'Match Room та ШІ-арбітраж спорів', stories: 'US-22..US-34', desc: 'Ready Check (10 хв), почерговий бан карт (30 с), авто-імпорт Chess.com та Vision AI для скріншотів.' },
  { step: 'Крок 5', title: 'Фіналізація та експорт у CSV', stories: 'US-35..US-36', desc: 'Підсумковий розподіл призових місць та вивантаження офіційного звіту для студради чи спонсорів.' }
]

export default function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<boolean>(false)

  useEffect(() => {
    fetch('/api/health')
      .then((res) => {
        if (!res.ok) throw new Error('Failed')
        return res.json()
      })
      .then((data: HealthResponse) => {
        setHealth(data)
        setError(false)
      })
      .catch(() => setError(true))
  }, [])

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-8">
      <div className="max-w-5xl mx-auto space-y-8">
        <header className="border-b border-slate-800 pb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-indigo-400 bg-indigo-950/60 px-3 py-1 rounded-full border border-indigo-800">
              Walking Skeleton • MVP v0.1
            </span>
            <h1 className="text-4xl font-extrabold tracking-tight mt-3 text-white">
              🌪️ Tournado
            </h1>
            <p className="text-slate-400 mt-1">
              Вебплатформа для автоматизації студентських та аматорських кіберспортивних турнірів
            </p>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 min-w-[260px]">
            <div className="text-xs text-slate-400 uppercase font-medium mb-2">Статус ядра (/api/health)</div>
            {health ? (
              <div className="space-y-1 text-sm">
                <div className="flex items-center gap-2 text-emerald-400 font-semibold">
                  <span className="h-2.5 w-2.5 rounded-full bg-emerald-400 animate-pulse" />
                  Backend: {health.status} | DB: {health.database}
                </div>
                <div className="text-xs text-slate-500 truncate">{health.timestamp}</div>
              </div>
            ) : error ? (
              <div className="text-sm text-amber-400 font-medium">
                Очікування з’єднання з Core API...
              </div>
            ) : (
              <div className="text-sm text-slate-400">Перевірка зв’язку...</div>
            )}
          </div>
        </header>

        <section>
          <h2 className="text-xl font-bold text-white mb-4">Заплановані екрани MVP (In Scope)</h2>
          <div className="grid gap-4 md:grid-cols-2">
            {PLANNED_SCREENS.map((screen) => (
              <div key={screen.step} className="bg-slate-900/70 border border-slate-800 rounded-xl p-5 hover:border-slate-700 transition">
                <div className="flex items-center justify-between text-xs text-indigo-400 font-semibold mb-1">
                  <span>{screen.step}</span>
                  <span className="font-mono text-slate-500">{screen.stories}</span>
                </div>
                <h3 className="text-lg font-bold text-slate-100 mb-2">{screen.title}</h3>
                <p className="text-sm text-slate-400 leading-relaxed">{screen.desc}</p>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
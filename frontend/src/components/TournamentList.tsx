import React, { useMemo, useState } from 'react'
import type { Discipline, Tournament } from '../types/tournament'
import { DISCIPLINE_CONFIG } from '../types/tournament'
import { DisciplineLogo } from './DisciplineLogo'
import { TournamentCard } from './TournamentCard'
import {
  AlertCircleIcon,
  CloseIcon,
  FilterIcon,
  PlusIcon,
  SearchIcon,
  TrophyIcon
} from './icons/Icons'

interface TournamentListProps {
  tournaments: Tournament[]
  isLoading: boolean
  error: string | null
  onSelectTournament: (tournament: Tournament) => void
  onCreateClick: () => void
  onRetry: () => void
}

type DisciplineFilter = 'ALL' | Discipline

export const TournamentList: React.FC<TournamentListProps> = ({
  tournaments,
  isLoading,
  error,
  onSelectTournament,
  onCreateClick,
  onRetry
}) => {
  const [selectedDiscipline, setSelectedDiscipline] = useState<DisciplineFilter>('ALL')
  const [searchQuery, setSearchQuery] = useState('')

  // Count per discipline
  const counts = useMemo(() => {
    const res: Record<DisciplineFilter, number> = {
      ALL: tournaments.length,
      CS2: 0,
      BRAWL_STARS: 0,
      CHESS: 0
    }
    for (const t of tournaments) {
      if (t.discipline && res[t.discipline] !== undefined) {
        res[t.discipline]++
      }
    }
    return res
  }, [tournaments])

  // Filtered list
  const filtered = useMemo(() => {
    return tournaments.filter((t) => {
      const matchesDiscipline =
        selectedDiscipline === 'ALL' || t.discipline === selectedDiscipline
      const matchesSearch =
        !searchQuery.trim() ||
        t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (t.description && t.description.toLowerCase().includes(searchQuery.toLowerCase()))
      return matchesDiscipline && matchesSearch
    })
  }, [tournaments, selectedDiscipline, searchQuery])

  return (
    <div className="space-y-6">
      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 bg-[#121620] border border-[#1E2536] p-3 rounded-2xl">
        {/* Discipline Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
          <button
            type="button"
            onClick={() => setSelectedDiscipline('ALL')}
            className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition whitespace-nowrap flex items-center gap-2 ${
              selectedDiscipline === 'ALL'
                ? 'bg-orange-500/10 text-orange-400 border border-orange-500/30 shadow-sm'
                : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
            }`}
          >
            <FilterIcon className="w-3.5 h-3.5" />
            <span>Всі дисципліни</span>
            <span className="px-2 py-0.5 rounded-full bg-[#0B0E14] text-[10px] font-mono text-slate-300">
              {counts.ALL}
            </span>
          </button>

          {(Object.keys(DISCIPLINE_CONFIG) as Discipline[]).map((key) => {
            const disc = DISCIPLINE_CONFIG[key]
            const isSelected = selectedDiscipline === key
            return (
              <button
                key={key}
                type="button"
                onClick={() => setSelectedDiscipline(key)}
                className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition whitespace-nowrap flex items-center gap-2 ${
                  isSelected
                    ? `${disc.theme.badgeBg} ${disc.theme.badgeText} border ${disc.theme.badgeBorder} shadow-sm`
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                }`}
              >
                <DisciplineLogo discipline={key} className="w-4 h-4 rounded-sm object-cover" />
                <span>{disc.shortName}</span>
                <span className="px-2 py-0.5 rounded-full bg-[#0B0E14] text-[10px] font-mono text-slate-300">
                  {counts[key]}
                </span>
              </button>
            )
          })}
        </div>

        {/* Search Input */}
        <div className="relative min-w-[240px]">
          <span className="absolute left-3 top-2.5 text-slate-500">
            <SearchIcon className="w-4 h-4" />
          </span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Пошук за назвою або регламентом..."
            className="w-full pl-9 pr-8 py-2 bg-[#0B0E14] border border-[#1E2536] rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-orange-500 transition"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-2.5 text-slate-500 hover:text-white"
              aria-label="Очистити пошук"
            >
              <CloseIcon className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-200 flex items-center justify-between animate-fadeIn">
          <div className="flex items-center gap-3">
            <AlertCircleIcon className="w-5 h-5 text-amber-400 flex-shrink-0" />
            <div className="text-xs">
              <span className="font-semibold">Зверніть увагу: </span>
              <span>{error}</span>
            </div>
          </div>
          <button
            type="button"
            onClick={onRetry}
            className="px-3.5 py-1.5 bg-amber-600/20 hover:bg-amber-600/40 border border-amber-500/40 rounded-lg text-xs font-semibold text-amber-200 transition"
          >
            Спробувати знову
          </button>
        </div>
      )}

      {/* Loading Skeletons */}
      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="bg-[#121620] border border-[#1E2536] rounded-2xl p-5 space-y-4 animate-pulse"
            >
              <div className="flex justify-between">
                <div className="h-6 w-24 bg-slate-800 rounded-lg" />
                <div className="h-6 w-20 bg-slate-800 rounded-full" />
              </div>
              <div className="h-5 w-3/4 bg-slate-800 rounded" />
              <div className="h-3 w-full bg-slate-800/60 rounded" />
              <div className="h-10 w-full bg-slate-800/40 rounded-xl" />
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {!isLoading && filtered.length === 0 && (
        <div className="bg-[#121620] border border-dashed border-[#1E2536] rounded-2xl p-12 text-center space-y-4">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-orange-500/10 border border-orange-500/20 flex items-center justify-center text-orange-400">
            <TrophyIcon className="w-7 h-7" />
          </div>
          <div className="max-w-md mx-auto space-y-1.5">
            <h3 className="text-base font-bold text-white">
              {searchQuery || selectedDiscipline !== 'ALL'
                ? 'Турнірів за заданими критеріями не знайдено'
                : 'Поки що немає створених турнірів'}
            </h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              {searchQuery || selectedDiscipline !== 'ALL'
                ? 'Спробуйте змінити фільтр дисципліни або очистити пошуковий запит.'
                : 'Створіть перший змагальний турнір для Counter-Strike 2, Brawl Stars або Chess.com.'}
            </p>
          </div>
          <div className="pt-2">
            <button
              type="button"
              onClick={onCreateClick}
              className="px-5 py-2.5 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition inline-flex items-center gap-2 shadow-lg shadow-orange-500/20"
            >
              <PlusIcon className="w-4 h-4" />
              <span>Створити турнір</span>
            </button>
          </div>
        </div>
      )}

      {/* Tournament Cards Grid */}
      {!isLoading && filtered.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filtered.map((tournament) => (
            <TournamentCard
              key={tournament.id}
              tournament={tournament}
              onSelect={onSelectTournament}
            />
          ))}
        </div>
      )}
    </div>
  )
}

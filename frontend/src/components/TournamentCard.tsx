import React from 'react'
import type { Discipline, Tournament } from '../types/tournament'
import { DISCIPLINE_CONFIG, STATUS_CONFIG } from '../types/tournament'
import { formatDateTime, formatRelativeTimeUntil, formatShortDateTime } from '../utils/date'
import { DisciplineLogo } from './DisciplineLogo'
import {
  CalendarIcon,
  ChevronRightIcon,
  ClockIcon,
  UsersIcon
} from './icons/Icons'

interface TournamentCardProps {
  tournament: Tournament
  onSelect: (tournament: Tournament) => void
  index?: number
}

export const DisciplineIcon: React.FC<{ discipline: Discipline; className?: string }> = ({
  discipline,
  className = 'w-4 h-4'
}) => {
  return <DisciplineLogo discipline={discipline} className={className} />
}

export const TournamentCard: React.FC<TournamentCardProps> = ({
  tournament,
  onSelect,
  index = 0
}) => {
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
      glowBorder: 'hover:border-slate-600',
      topStripClass: 'from-slate-500 to-slate-600',
      hoverShadowClass: 'hover:shadow-slate-500/10'
    }
  }

  const statusMeta = STATUS_CONFIG[tournament.status] || {
    label: tournament.status,
    badgeClass: 'bg-slate-800 text-slate-300 border-slate-700',
    dotClass: 'bg-slate-400'
  }

  const registeredCount = tournament.registeredTeamsCount ?? 0
  const slotLimit = tournament.slotLimit || 16
  const fillPercent = Math.min(100, Math.round((registeredCount / slotLimit) * 100))

  const relativeStart =
    tournament.status === 'COMPLETED'
      ? 'Завершено'
      : tournament.status === 'IN_PROGRESS'
      ? 'Матчі наживо'
      : formatRelativeTimeUntil(tournament.startTimeAt)

  return (
    <div
      onClick={() => onSelect(tournament)}
      style={{ animationDelay: `${Math.min(index, 8) * 55}ms` }}
      className={`animate-fadeUp group relative overflow-hidden bg-[#121620] hover:bg-[#161C2A] border border-[#1E2536] ${discMeta.theme.glowBorder} rounded-2xl p-5 transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl ${discMeta.theme.hoverShadowClass} cursor-pointer flex flex-col justify-between`}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onSelect(tournament)
        }
      }}
    >
      {/* Top Discipline Accent Line */}
      <div
        className={`absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r ${discMeta.theme.topStripClass} opacity-45 group-hover:opacity-100 transition-opacity duration-300`}
      />

      <div>
        {/* Top Badges */}
        <div className="flex items-center justify-between gap-2 mb-3.5">
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex items-center gap-2 px-2.5 py-1 rounded-lg border text-xs font-semibold ${discMeta.theme.badgeBg} ${discMeta.theme.badgeText} ${discMeta.theme.badgeBorder}`}
            >
              <DisciplineLogo
                discipline={tournament.discipline}
                className="w-4 h-4 rounded-sm object-cover transition-transform duration-300 group-hover:scale-110"
              />
              <span>{discMeta.shortName}</span>
            </span>
            <span className="text-[11px] font-mono font-medium text-slate-400 bg-slate-900/90 px-2 py-0.5 rounded border border-slate-800">
              {discMeta.formatLabel}
            </span>
          </div>

          <span
            className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border text-[11px] font-mono font-bold tracking-wider ${statusMeta.badgeClass}`}
          >
            <span className={`h-1.5 w-1.5 rounded-full ${statusMeta.dotClass}`} />
            <span>{statusMeta.label}</span>
          </span>
        </div>

        {/* Tournament Name & Description */}
        <h3 className="text-base font-bold text-white group-hover:text-orange-400 transition-colors line-clamp-1">
          {tournament.name}
        </h3>
        {tournament.description ? (
          <p className="text-xs text-slate-400 mt-1.5 line-clamp-2 leading-relaxed min-h-[2.4rem]">
            {tournament.description}
          </p>
        ) : (
          <p className="text-xs text-slate-600 italic mt-1.5 min-h-[2.4rem]">Опис відсутній</p>
        )}

        {/* Slots & Progress */}
        <div className="mt-4 p-3 bg-[#0B0E14]/80 rounded-xl border border-[#1E2536]/80">
          <div className="flex items-center justify-between text-xs mb-1.5">
            <span className="text-slate-400 flex items-center gap-1.5">
              <UsersIcon className="w-3.5 h-3.5 text-slate-500" />
              <span>Слоти команд</span>
            </span>
            <span className="font-mono font-semibold text-slate-200">
              {registeredCount} / {slotLimit}
            </span>
          </div>
          <div className="w-full bg-slate-800/80 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-gradient-to-r from-orange-500 to-amber-400 h-1.5 rounded-full transition-all duration-500"
              style={{ width: `${Math.max(fillPercent, registeredCount > 0 ? 4 : 0)}%` }}
            />
          </div>
        </div>

        {/* Schedule preview */}
        <div className="mt-3.5 space-y-1.5 text-xs text-slate-400">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 text-slate-500">
              <CalendarIcon className="w-3.5 h-3.5" />
              <span>Старт</span>
            </span>
            <span className="font-mono font-medium text-slate-200">
              {formatDateTime(tournament.startTimeAt)}
            </span>
          </div>

          <div className="flex items-center justify-between text-[11px] text-slate-500">
            <span className="flex items-center gap-1.5">
              <ClockIcon className="w-3.5 h-3.5 text-slate-600" />
              <span>Дедлайн подачі</span>
            </span>
            <span className="font-mono text-slate-400">
              {formatShortDateTime(tournament.registrationDeadlineAt)}
            </span>
          </div>
        </div>
      </div>

      {/* Footer link hint */}
      <div className="mt-4 pt-3 border-t border-[#1E2536] flex items-center justify-between text-xs">
        <span className="font-mono text-[11px] text-slate-400 bg-[#0B0E14] px-2 py-0.5 rounded-md border border-[#1E2536]">
          {relativeStart}
        </span>
        <span className="text-orange-400 group-hover:text-orange-300 font-semibold flex items-center gap-1 transition-transform duration-200 group-hover:translate-x-1">
          <span>Деталі</span>
          <ChevronRightIcon className="w-3.5 h-3.5" />
        </span>
      </div>
    </div>
  )
}

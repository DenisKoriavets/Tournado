import React from 'react'
import type { Discipline, Tournament } from '../types/tournament'
import { DISCIPLINE_CONFIG, STATUS_CONFIG } from '../types/tournament'
import { formatDateTime, formatShortDateTime } from '../utils/date'
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
}

export const DisciplineIcon: React.FC<{ discipline: Discipline; className?: string }> = ({
  discipline,
  className = 'w-4 h-4'
}) => {
  return <DisciplineLogo discipline={discipline} className={className} />
}

export const TournamentCard: React.FC<TournamentCardProps> = ({ tournament, onSelect }) => {
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

  const registeredCount = tournament.registeredTeamsCount ?? 0
  const slotLimit = tournament.slotLimit || 16
  const fillPercent = Math.min(100, Math.round((registeredCount / slotLimit) * 100))

  return (
    <div
      onClick={() => onSelect(tournament)}
      className="group relative bg-[#121620] hover:bg-[#161C2A] border border-[#1E2536] hover:border-orange-500/50 rounded-2xl p-5 transition-all duration-200 hover:shadow-xl hover:shadow-orange-500/5 cursor-pointer flex flex-col justify-between"
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onSelect(tournament)
        }
      }}
    >
      <div>
        {/* Top Badges */}
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg border text-xs font-semibold ${discMeta.theme.badgeBg} ${discMeta.theme.badgeText} ${discMeta.theme.badgeBorder}`}
            >
              <DisciplineLogo discipline={tournament.discipline} className="w-4 h-4 rounded-sm object-cover" />
              <span>{discMeta.shortName}</span>
            </span>
            <span className="text-[11px] font-mono font-medium text-slate-400 bg-slate-900/80 px-2 py-0.5 rounded border border-slate-800">
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
          <p className="text-xs text-slate-400 mt-1 line-clamp-2 leading-relaxed">
            {tournament.description}
          </p>
        ) : (
          <p className="text-xs text-slate-600 italic mt-1">Опис відсутній</p>
        )}

        {/* Slots & Progress */}
        <div className="mt-4 p-3 bg-[#0B0E14]/70 rounded-xl border border-[#1E2536]/80">
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
              className="bg-orange-500 h-1.5 rounded-full transition-all duration-300"
              style={{ width: `${fillPercent}%` }}
            />
          </div>
        </div>

        {/* Schedule preview */}
        <div className="mt-3 space-y-1.5 text-xs text-slate-400">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 text-slate-500">
              <CalendarIcon className="w-3.5 h-3.5" />
              <span>Старт змагань:</span>
            </span>
            <span className="font-mono font-medium text-slate-200">
              {formatDateTime(tournament.startTimeAt)}
            </span>
          </div>

          <div className="flex items-center justify-between text-[11px] text-slate-500">
            <span className="flex items-center gap-1.5">
              <ClockIcon className="w-3.5 h-3.5 text-slate-600" />
              <span>Дедлайн подачі:</span>
            </span>
            <span className="font-mono text-slate-400">
              {formatShortDateTime(tournament.registrationDeadlineAt)}
            </span>
          </div>
        </div>
      </div>

      {/* Footer link hint */}
      <div className="mt-4 pt-3 border-t border-[#1E2536] flex items-center justify-between text-xs">
        <span className="font-mono text-[11px] text-slate-500 truncate max-w-[180px]">
          /{tournament.publicSlug || tournament.id.slice(0, 8)}
        </span>
        <span className="text-orange-400 group-hover:text-orange-300 font-semibold flex items-center gap-1 transition-transform group-hover:translate-x-0.5">
          <span>Деталі</span>
          <ChevronRightIcon className="w-3.5 h-3.5" />
        </span>
      </div>
    </div>
  )
}

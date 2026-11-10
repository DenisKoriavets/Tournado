import React from 'react'
import type { Tournament } from '../types/tournament'
import { DISCIPLINE_CONFIG, STATUS_CONFIG } from '../types/tournament'
import { formatDateTime, formatShortDateTime } from '../utils/date'

interface TournamentCardProps {
  tournament: Tournament
  onSelect: (tournament: Tournament) => void
}

export const TournamentCard: React.FC<TournamentCardProps> = ({ tournament, onSelect }) => {
  const discMeta = DISCIPLINE_CONFIG[tournament.discipline] || {
    name: tournament.discipline,
    shortName: tournament.discipline,
    teamSize: tournament.teamSize || 1,
    formatLabel: `${tournament.teamSize}v${tournament.teamSize}`,
    icon: '🎮',
    defaultMaps: [],
    theme: {
      badgeBg: 'bg-slate-800',
      badgeText: 'text-slate-300',
      badgeBorder: 'border-slate-700'
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

  const maps = tournament.mapPool && tournament.mapPool.length > 0
    ? tournament.mapPool
    : (tournament.discipline !== 'CHESS' ? discMeta.defaultMaps : [])

  return (
    <div
      onClick={() => onSelect(tournament)}
      className="group relative bg-slate-900/80 border border-slate-800 hover:border-indigo-500/50 rounded-2xl p-5 transition-all duration-200 hover:shadow-xl hover:shadow-indigo-500/10 cursor-pointer flex flex-col justify-between"
    >
      <div>
        {/* Top Badges */}
        <div className="flex items-center justify-between gap-2 mb-3">
          <div className="flex items-center gap-2">
            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg border text-xs font-semibold ${discMeta.theme.badgeBg} ${discMeta.theme.badgeText} ${discMeta.theme.badgeBorder}`}>
              <span>{discMeta.icon}</span>
              <span>{discMeta.shortName}</span>
            </span>
            <span className="text-xs font-mono font-medium text-slate-400 bg-slate-800/80 px-2 py-0.5 rounded border border-slate-700">
              {discMeta.formatLabel}
            </span>
          </div>

          <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border text-xs font-medium ${statusMeta.badgeClass}`}>
            <span className={`h-1.5 w-1.5 rounded-full ${statusMeta.dotClass}`} />
            <span>{statusMeta.label}</span>
          </span>
        </div>

        {/* Tournament Name & Description */}
        <h3 className="text-lg font-bold text-white group-hover:text-indigo-400 transition line-clamp-1">
          {tournament.name}
        </h3>
        {tournament.description ? (
          <p className="text-xs text-slate-400 mt-1 line-clamp-2 leading-relaxed">
            {tournament.description}
          </p>
        ) : (
          <p className="text-xs text-slate-500 italic mt-1">Опис відсутній</p>
        )}

        {/* Slots & Progress */}
        <div className="mt-4 p-3 bg-slate-950/60 rounded-xl border border-slate-800/80">
          <div className="flex items-center justify-between text-xs mb-1.5">
            <span className="text-slate-400">Заповненість сітки:</span>
            <span className="font-semibold text-slate-200">
              {registeredCount} / {slotLimit} команд
            </span>
          </div>
          <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-indigo-500 h-1.5 rounded-full transition-all duration-300"
              style={{ width: `${fillPercent}%` }}
            />
          </div>
        </div>

        {/* Schedule preview */}
        <div className="mt-3 space-y-1.5 text-xs text-slate-400">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 text-slate-400">
              <span>📅</span>
              <span>Старт:</span>
            </span>
            <span className="font-medium text-slate-200">
              {formatDateTime(tournament.startTimeAt)}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 text-slate-500">
              <span>⏳</span>
              <span>Дедлайн заявок:</span>
            </span>
            <span className="text-slate-400">
              {formatShortDateTime(tournament.registrationDeadlineAt)}
            </span>
          </div>
        </div>
      </div>

      {/* Map pool tags & Footer action */}
      <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between">
        <div className="flex items-center gap-1 overflow-hidden">
          {maps.length > 0 ? (
            <>
              {maps.slice(0, 2).map(mapName => (
                <span
                  key={mapName}
                  className="px-1.5 py-0.5 bg-slate-800 text-slate-400 rounded text-[10px] font-mono truncate max-w-[80px]"
                >
                  {mapName}
                </span>
              ))}
              {maps.length > 2 && (
                <span className="px-1.5 py-0.5 bg-slate-800 text-slate-400 rounded text-[10px] font-mono">
                  +{maps.length - 2}
                </span>
              )}
            </>
          ) : (
            <span className="text-[11px] text-slate-500">
              {tournament.discipline === 'CHESS' ? '♟️ Chess.com Live' : 'Без карт'}
            </span>
          )}
        </div>

        <span className="text-xs font-semibold text-indigo-400 group-hover:text-indigo-300 flex items-center gap-1 transition">
          <span>Деталі</span>
          <span>➔</span>
        </span>
      </div>
    </div>
  )
}

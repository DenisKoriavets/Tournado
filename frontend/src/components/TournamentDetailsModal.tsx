import React, { useState } from 'react'
import type { Tournament } from '../types/tournament'
import { DISCIPLINE_CONFIG, STATUS_CONFIG } from '../types/tournament'
import { formatDateTime } from '../utils/date'

interface TournamentDetailsModalProps {
  tournament: Tournament | null
  isOpen: boolean
  onClose: () => void
}

export const TournamentDetailsModal: React.FC<TournamentDetailsModalProps> = ({
  tournament,
  isOpen,
  onClose
}) => {
  const [copiedLink, setCopiedLink] = useState(false)
  const [copiedConnect, setCopiedConnect] = useState(false)

  if (!isOpen || !tournament) return null

  const discMeta = DISCIPLINE_CONFIG[tournament.discipline] || {
    name: tournament.discipline,
    shortName: tournament.discipline,
    teamSize: tournament.teamSize || 1,
    formatLabel: `${tournament.teamSize}v${tournament.teamSize}`,
    description: '',
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

  const publicLink = `${window.location.origin}/tournaments/${tournament.publicSlug || tournament.id}`

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(publicLink)
      setCopiedLink(true)
      setTimeout(() => setCopiedLink(false), 2500)
    } catch {
      // fallback
    }
  }

  const handleCopyConnect = async () => {
    if (!tournament.cs2ServerConnect) return
    try {
      await navigator.clipboard.writeText(tournament.cs2ServerConnect)
      setCopiedConnect(true)
      setTimeout(() => setCopiedConnect(false), 2500)
    } catch {
      // fallback
    }
  }

  const registeredCount = tournament.registeredTeamsCount ?? 0
  const slotLimit = tournament.slotLimit || 16
  const fillPercent = Math.min(100, Math.round((registeredCount / slotLimit) * 100))

  const maps = tournament.mapPool && tournament.mapPool.length > 0
    ? tournament.mapPool
    : (tournament.discipline !== 'CHESS' ? discMeta.defaultMaps : [])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden my-8"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tournament-details-title"
      >
        {/* Header with badges & Close button */}
        <div className="px-6 py-5 border-b border-slate-800 flex items-center justify-between bg-slate-900/90">
          <div className="flex items-center gap-2.5">
            <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-lg border text-xs font-semibold ${discMeta.theme.badgeBg} ${discMeta.theme.badgeText} ${discMeta.theme.badgeBorder}`}>
              <span>{discMeta.icon}</span>
              <span>{discMeta.shortName}</span>
            </span>
            <span className="text-xs font-mono font-medium text-slate-300 bg-slate-800 px-2 py-1 rounded border border-slate-700">
              {discMeta.formatLabel} • Single Elimination
            </span>
            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-xs font-medium ${statusMeta.badgeClass}`}>
              <span className={`h-1.5 w-1.5 rounded-full ${statusMeta.dotClass}`} />
              <span>{statusMeta.label}</span>
            </span>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition"
            aria-label="Закрити"
          >
            ✕
          </button>
        </div>

        <div className="p-6 space-y-6 max-h-[80vh] overflow-y-auto">
          {/* Tournament Title & Description */}
          <div>
            <h1 id="tournament-details-title" className="text-2xl font-extrabold text-white tracking-tight">
              {tournament.name}
            </h1>
            {tournament.description ? (
              <p className="text-sm text-slate-300 mt-2 leading-relaxed whitespace-pre-line">
                {tournament.description}
              </p>
            ) : (
              <p className="text-xs text-slate-500 italic mt-2">Організатор не додав детального опису.</p>
            )}
          </div>

          {/* Public shareable link (US-11) */}
          <div className="p-4 rounded-xl bg-indigo-950/40 border border-indigo-900/60 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-indigo-300 flex items-center gap-1.5">
                <span>🔗</span>
                <span>Публічне посилання на турнір (US-11)</span>
              </span>
              <span className="text-[11px] text-indigo-400">Доступно без авторизації</span>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={publicLink}
                className="flex-1 bg-slate-950/80 border border-indigo-800/60 rounded-lg px-3 py-1.5 text-xs font-mono text-indigo-200 select-all focus:outline-none"
              />
              <button
                type="button"
                onClick={handleCopyLink}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-1.5 ${
                  copiedLink
                    ? 'bg-emerald-600 text-white'
                    : 'bg-indigo-600 hover:bg-indigo-500 text-white'
                }`}
              >
                <span>{copiedLink ? '✓' : '📋'}</span>
                <span>{copiedLink ? 'Скопійовано!' : 'Копіювати'}</span>
              </button>
            </div>
          </div>

          {/* Key Parameters Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800">
              <div className="text-[11px] text-slate-400 uppercase font-medium">Дисципліна</div>
              <div className="text-sm font-bold text-white mt-1 flex items-center gap-1.5">
                <span>{discMeta.icon}</span>
                <span>{discMeta.shortName}</span>
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800">
              <div className="text-[11px] text-slate-400 uppercase font-medium">Формат команд</div>
              <div className="text-sm font-bold text-slate-200 mt-1">
                {tournament.teamSize === 1 ? '1v1 (Соло)' : `${tournament.teamSize}v${tournament.teamSize} (${tournament.teamSize} ос.)`}
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800">
              <div className="text-[11px] text-slate-400 uppercase font-medium">Сітка змагань</div>
              <div className="text-sm font-bold text-slate-200 mt-1">
                Single Elimination
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800">
              <div className="text-[11px] text-slate-400 uppercase font-medium">Слоти команд</div>
              <div className="text-sm font-bold text-emerald-400 mt-1">
                {registeredCount} / {slotLimit}
              </div>
            </div>
          </div>

          {/* Slots Fill Progress */}
          <div className="p-4 bg-slate-950/60 rounded-xl border border-slate-800 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-400 font-medium">Статус заповненості слотів:</span>
              <span className="font-semibold text-slate-200">
                Зайнято {registeredCount} із {slotLimit} слотів ({fillPercent}%)
              </span>
            </div>
            <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
              <div
                className="bg-indigo-500 h-2 rounded-full transition-all duration-300"
                style={{ width: `${fillPercent}%` }}
              />
            </div>
          </div>

          {/* Tournament Schedule (US-10) */}
          <div className="space-y-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <span>⏰</span>
              <span>Розклад турніру (Київський час)</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800">
                <div className="text-xs text-slate-400 mb-1 flex items-center gap-1.5">
                  <span className="text-amber-400">1.</span>
                  <span>Дедлайн заявок</span>
                </div>
                <div className="text-xs font-bold text-slate-100">
                  {formatDateTime(tournament.registrationDeadlineAt)}
                </div>
                <div className="text-[10px] text-slate-500 mt-1">Авто-блокування ростерів (US-14)</div>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800">
                <div className="text-xs text-slate-400 mb-1 flex items-center gap-1.5">
                  <span className="text-indigo-400">2.</span>
                  <span>Відкриття Check-in</span>
                </div>
                <div className="text-xs font-bold text-slate-100">
                  {formatDateTime(tournament.checkInOpensAt)}
                </div>
                <div className="text-[10px] text-slate-500 mt-1">Підтвердження присутності (US-15)</div>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-950/60 border border-slate-800">
                <div className="text-xs text-slate-400 mb-1 flex items-center gap-1.5">
                  <span className="text-emerald-400">3.</span>
                  <span>Початок змагань</span>
                </div>
                <div className="text-xs font-bold text-emerald-300">
                  {formatDateTime(tournament.startTimeAt)}
                </div>
                <div className="text-[10px] text-slate-500 mt-1">Генерація сітки та Match Rooms</div>
              </div>
            </div>
          </div>

          {/* Map Pool (US-08) */}
          {maps.length > 0 ? (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                  <span>🗺️</span>
                  <span>Офіційний пул карт ({maps.length} карт)</span>
                </h3>
                <span className="text-[11px] text-slate-500">Використовується для вето (US-25)</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {maps.map(mapName => (
                  <span
                    key={mapName}
                    className="px-3 py-1 bg-slate-950 border border-slate-800 rounded-lg text-xs font-mono text-slate-200"
                  >
                    {mapName}
                  </span>
                ))}
              </div>
            </div>
          ) : (
            tournament.discipline === 'CHESS' && (
              <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-xs text-slate-400 flex items-center gap-2">
                <span>♟️</span>
                <span>Матчі проходять на платформі Chess.com (режим Rapid 10+0). Пул карт не застосовується.</span>
              </div>
            )
          )}

          {/* CS2 Server Connect (if configured) */}
          {tournament.cs2ServerConnect && (
            <div className="p-4 rounded-xl bg-amber-950/30 border border-amber-900/50 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-amber-300 flex items-center gap-1.5">
                  <span>🖥️</span>
                  <span>Виділений сервер CS2 (Connect string)</span>
                </span>
                <span className="text-[11px] text-amber-400">Консоль гри</span>
              </div>
              <div className="flex items-center gap-2">
                <code className="flex-1 bg-slate-950 px-3 py-1.5 rounded-lg text-xs font-mono text-amber-200 border border-amber-800/50 select-all truncate">
                  {tournament.cs2ServerConnect}
                </code>
                <button
                  type="button"
                  onClick={handleCopyConnect}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                    copiedConnect
                      ? 'bg-emerald-600 text-white'
                      : 'bg-amber-600 hover:bg-amber-500 text-white'
                  }`}
                >
                  {copiedConnect ? 'Скопійовано! ✓' : 'Копіювати'}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="px-6 py-4 border-t border-slate-800 bg-slate-900/90 flex items-center justify-between">
          <div className="text-xs text-slate-400">
            ID: <span className="font-mono text-slate-500">{tournament.id}</span>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-800 text-slate-300 hover:bg-slate-800 text-sm font-medium transition"
            >
              Закрити
            </button>
            <button
              type="button"
              disabled
              title="Реєстрація команд буде доступна у вертикальному зрізі 2 (US-12..US-14)"
              className="px-5 py-2 rounded-xl bg-slate-800 text-slate-400 border border-slate-700 text-sm font-semibold cursor-not-allowed flex items-center gap-2"
            >
              <span>Подати заявку команди</span>
              <span className="text-[10px] bg-slate-700 px-1.5 py-0.5 rounded text-slate-300">US-12</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

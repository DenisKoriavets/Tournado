import React, { useEffect, useState } from 'react'
import type { Tournament } from '../types/tournament'
import { DISCIPLINE_CONFIG, STATUS_CONFIG } from '../types/tournament'
import { formatDateTime } from '../utils/date'
import { DisciplineLogo } from './DisciplineLogo'
import {
  CalendarIcon,
  CheckIcon,
  ClockIcon,
  CloseIcon,
  CopyIcon,
  CrosshairIcon,
  ServerIcon,
  ShieldIcon,
  UsersIcon
} from './icons/Icons'

interface TournamentDrawerProps {
  tournament: Tournament | null
  isOpen: boolean
  onClose: () => void
}

type TabKey = 'overview' | 'schedule' | 'server'

export const TournamentDrawer: React.FC<TournamentDrawerProps> = ({
  tournament,
  isOpen,
  onClose
}) => {
  const [activeTab, setActiveTab] = useState<TabKey>('overview')
  const [copiedLink, setCopiedLink] = useState(false)
  const [copiedConnect, setCopiedConnect] = useState(false)

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown)
    }
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen || !tournament) return null

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

  const registeredCount = tournament.registeredTeamsCount ?? 0
  const slotLimit = tournament.slotLimit || 16
  const fillPercent = Math.min(100, Math.round((registeredCount / slotLimit) * 100))

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
        <div className="w-screen max-w-xl bg-[#121620] border-l border-[#1E2536] shadow-2xl flex flex-col justify-between">
          {/* Top Header */}
          <div className="p-6 border-b border-[#1E2536] bg-[#0E121A]">
            <div className="flex items-center justify-between gap-3 mb-3">
              <div className="flex items-center gap-2">
                <span
                  className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-lg border text-xs font-semibold ${discMeta.theme.badgeBg} ${discMeta.theme.badgeText} ${discMeta.theme.badgeBorder}`}
                >
                  <DisciplineLogo discipline={tournament.discipline} className="w-4 h-4 rounded-sm object-cover" />
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
            <p className="font-mono text-xs text-slate-500 mt-1 truncate">
              ID: {tournament.id}
            </p>

            {/* Navigation Tabs */}
            <div className="flex items-center gap-2 mt-5 border-b border-[#1E2536] -mb-6 pb-2">
              <button
                type="button"
                onClick={() => setActiveTab('overview')}
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition ${
                  activeTab === 'overview'
                    ? 'bg-orange-500/10 text-orange-400 border border-orange-500/30'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Огляд турніру
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('schedule')}
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition ${
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
                className={`px-3 py-2 text-xs font-semibold rounded-lg transition ${
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
                    <p className="text-xs text-slate-500 italic">Організатор не залишив додаткового опису.</p>
                  )}
                </div>

                {/* Public Link Share */}
                <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                      <CopyIcon className="w-3.5 h-3.5 text-orange-400" />
                      <span>Публічне посилання для учасників</span>
                    </span>
                    <span className="text-[11px] text-slate-500 font-mono">Доступно без авторизації</span>
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
                      {copiedLink ? <CheckIcon className="w-3.5 h-3.5" /> : <CopyIcon className="w-3.5 h-3.5" />}
                      <span>{copiedLink ? 'Скопійовано' : 'Копіювати'}</span>
                    </button>
                  </div>
                </div>

                {/* Key Metrics Grid */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                    <div className="text-[11px] text-slate-500 uppercase font-mono">Формат матчів</div>
                    <div className="text-sm font-bold text-white mt-1">
                      {tournament.teamSize === 1
                        ? '1v1 (Соло)'
                        : `${tournament.teamSize}v${tournament.teamSize} (${tournament.teamSize} гравців)`}
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                    <div className="text-[11px] text-slate-500 uppercase font-mono">Турнірна сітка</div>
                    <div className="text-sm font-bold text-white mt-1">
                      Single Elimination
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                    <div className="text-[11px] text-slate-500 uppercase font-mono">Слоти команд</div>
                    <div className="text-sm font-mono font-bold text-orange-400 mt-1">
                      {registeredCount} / {slotLimit} команд
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                    <div className="text-[11px] text-slate-500 uppercase font-mono">Правила овертайму</div>
                    <div className="text-sm font-bold text-slate-300 mt-1">
                      {tournament.discipline === 'CS2' ? 'MR3 $10k' : 'Standard Rules'}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'schedule' && (
              <div className="space-y-6">
                {/* Slots Fill Bar */}
                <div className="p-4 bg-[#0B0E14] rounded-xl border border-[#1E2536] space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-400 font-medium">Заповненість турнірної сітки:</span>
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
                      <div className="text-sm font-bold text-slate-200 mt-0.5">Дедлайн подачі заявок</div>
                      <div className="text-xs font-mono text-slate-400 mt-0.5">
                        {formatDateTime(tournament.registrationDeadlineAt)}
                      </div>
                    </div>

                    <div className="relative pt-3">
                      <div className="absolute -left-[23px] top-4 h-3 w-3 rounded-full bg-amber-500 border-2 border-[#121620]" />
                      <div className="text-xs text-amber-500 font-mono">Етап 2</div>
                      <div className="text-sm font-bold text-slate-200 mt-0.5">Відкриття вікна Check-in</div>
                      <div className="text-xs font-mono text-amber-300 mt-0.5">
                        {formatDateTime(tournament.checkInOpensAt)}
                      </div>
                    </div>

                    <div className="relative pt-3">
                      <div className="absolute -left-[23px] top-4 h-3 w-3 rounded-full bg-orange-500 border-2 border-[#121620]" />
                      <div className="text-xs text-orange-400 font-mono">Етап 3</div>
                      <div className="text-sm font-bold text-slate-100 mt-0.5">Початок перших матчів</div>
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
                          {copiedConnect ? <CheckIcon className="w-3.5 h-3.5 text-emerald-400" /> : <CopyIcon className="w-3.5 h-3.5" />}
                          <span>{copiedConnect ? 'Команду connect скопійовано!' : 'Копіювати рядок connect'}</span>
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
                          <span className="font-mono text-xs text-slate-200 font-semibold">{mapName}</span>
                          <span className="text-[10px] font-mono text-slate-500">#{idx + 1}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] text-xs text-slate-400">
                      Для дисципліни Chess.com класичний мап-пул відсутній — змагання проходять на дошці 8х8 за регламентом FIDE / Chess.com Rapid.
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

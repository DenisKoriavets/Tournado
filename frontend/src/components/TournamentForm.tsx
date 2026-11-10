import React, { useState, useEffect } from 'react'
import type { CreateTournamentPayload, Discipline, ProblemDetail, SlotLimit, Tournament } from '../types/tournament'
import { DISCIPLINE_CONFIG } from '../types/tournament'
import { createTournament, ApiError } from '../api/tournaments'
import { generateDemoSchedule, localInputToIso, validateSchedule } from '../utils/date'

interface TournamentFormProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (tournament: Tournament) => void
}

const SLOT_OPTIONS: SlotLimit[] = [8, 16, 32]

export const TournamentForm: React.FC<TournamentFormProps> = ({ isOpen, onClose, onSuccess }) => {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [discipline, setDiscipline] = useState<Discipline | null>(null)
  const [slotLimit, setSlotLimit] = useState<SlotLimit | null>(null)
  const [deadlineInput, setDeadlineInput] = useState('')
  const [checkInInput, setCheckInInput] = useState('')
  const [startInput, setStartInput] = useState('')
  const [cs2ServerConnect, setCs2ServerConnect] = useState('')

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [apiError, setApiError] = useState<ProblemDetail | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  // Reset or preset state when modal opens
  useEffect(() => {
    if (isOpen) {
      setApiError(null)
      setFieldErrors({})
    }
  }, [isOpen])

  if (!isOpen) return null

  const handleFillDemoSchedule = () => {
    const demo = generateDemoSchedule()
    setDeadlineInput(demo.deadlineInput)
    setCheckInInput(demo.checkInInput)
    setStartInput(demo.startInput)
    // Clear schedule-related field errors
    setFieldErrors(prev => {
      const next = { ...prev }
      delete next.registrationDeadlineAt
      delete next.checkInOpensAt
      delete next.startTimeAt
      delete next.schedule
      return next
    })
  }

  const handleSelectDiscipline = (disc: Discipline) => {
    setDiscipline(disc)
    setFieldErrors(prev => {
      const next = { ...prev }
      delete next.discipline
      return next
    })
  }

  const handleSelectSlotLimit = (slots: SlotLimit) => {
    setSlotLimit(slots)
    setFieldErrors(prev => {
      const next = { ...prev }
      delete next.slotLimit
      return next
    })
  }

  // Live schedule validation indicator
  const scheduleValidation = validateSchedule(deadlineInput, checkInInput, startInput)
  const hasScheduleInputs = Boolean(deadlineInput && checkInInput && startInput)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setApiError(null)
    const errors: Record<string, string> = {}

    if (!name.trim()) {
      errors.name = 'Вкажіть назву турніру (мінімум 3 символи).'
    } else if (name.trim().length < 3) {
      errors.name = 'Назва турніру занадто коротка (мінімум 3 символи).'
    }

    if (!discipline) {
      errors.discipline = 'Оберіть ігрову дисципліну.'
    }

    if (!slotLimit) {
      errors.slotLimit = 'Оберіть максимальну кількість слотів для команд.'
    }

    if (!deadlineInput) errors.registrationDeadlineAt = 'Вкажіть дедлайн подачі заявок.'
    if (!checkInInput) errors.checkInOpensAt = 'Вкажіть час відкриття Check-in.'
    if (!startInput) errors.startTimeAt = 'Вкажіть час початку змагань.'

    if (deadlineInput && checkInInput && startInput) {
      const val = validateSchedule(deadlineInput, checkInInput, startInput)
      if (!val.isValid) {
        errors.schedule = val.errors.join(' ')
      }
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    const payload: CreateTournamentPayload = {
      name: name.trim(),
      description: description.trim() || undefined,
      discipline: discipline!,
      slotLimit: slotLimit!,
      registrationDeadlineAt: localInputToIso(deadlineInput),
      checkInOpensAt: localInputToIso(checkInInput),
      startTimeAt: localInputToIso(startInput),
      cs2ServerConnect: discipline === 'CS2' && cs2ServerConnect.trim() ? cs2ServerConnect.trim() : undefined
    }

    setIsSubmitting(true)
    try {
      const created = await createTournament(payload)
      onSuccess(created)
      onClose()
    } catch (err) {
      if (err instanceof ApiError) {
        setApiError(err.problem)
        if (err.problem.invalidParams && err.problem.invalidParams.length > 0) {
          const mappedErrors: Record<string, string> = {}
          for (const param of err.problem.invalidParams) {
            mappedErrors[param.name] = param.reason
          }
          setFieldErrors(prev => ({ ...prev, ...mappedErrors }))
        }
      } else {
        setApiError({
          title: 'Помилка створення турніру',
          status: 500,
          detail: err instanceof Error ? err.message : 'Невідома помилка.',
          code: 'CLIENT_ERROR'
        })
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedDisciplineMeta = discipline ? DISCIPLINE_CONFIG[discipline] : null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div 
        className="relative w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden my-8"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tournament-form-title"
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-slate-800 flex items-center justify-between bg-slate-900/80">
          <div className="flex items-center gap-3">
            <span className="text-2xl">🏆</span>
            <div>
              <h2 id="tournament-form-title" className="text-xl font-bold text-white tracking-tight">
                Створити новий турнір
              </h2>
              <p className="text-xs text-slate-400">
                Конфігурація дисципліни, ліміту слотів та автоматичного розкладу (US-07..US-10)
              </p>
            </div>
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

        {/* RFC 7807 API Error Banner */}
        {apiError && (
          <div className="mx-6 mt-5 p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-200">
            <div className="flex items-start gap-3">
              <span className="text-xl">⚠️</span>
              <div className="flex-1 text-sm space-y-1">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-rose-300">{apiError.title}</span>
                  {apiError.code && (
                    <span className="text-xs font-mono px-2 py-0.5 rounded bg-rose-950 border border-rose-800 text-rose-400">
                      {apiError.code}
                    </span>
                  )}
                </div>
                <p className="text-rose-200/90 text-xs leading-relaxed">{apiError.detail}</p>
                {apiError.invalidParams && apiError.invalidParams.length > 0 && (
                  <ul className="mt-2 list-disc list-inside text-xs space-y-0.5 text-rose-300">
                    {apiError.invalidParams.map((p, idx) => (
                      <li key={idx}>
                        <span className="font-mono font-medium">{p.name}</span>: {p.reason}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Step 1: Discipline selection (US-07) */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-sm font-semibold text-slate-200 flex items-center gap-2">
                <span>1. Ігрова дисципліна</span>
                <span className="text-rose-400">*</span>
              </label>
              <span className="text-xs text-slate-400">Автоматичний формат команд</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {(Object.keys(DISCIPLINE_CONFIG) as Discipline[]).map(key => {
                const item = DISCIPLINE_CONFIG[key]
                const isSelected = discipline === key
                return (
                  <button
                    key={key}
                    type="button"
                    onClick={() => handleSelectDiscipline(key)}
                    className={`relative p-3.5 rounded-xl border text-left transition flex flex-col justify-between ${
                      isSelected
                        ? `${item.theme.badgeBg} ${item.theme.badgeBorder} border-2 shadow-lg shadow-indigo-500/5`
                        : 'bg-slate-950/60 border-slate-800 hover:border-slate-700 hover:bg-slate-800/40'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-2xl">{item.icon}</span>
                      <span className={`text-xs font-bold px-2 py-0.5 rounded-md border ${item.theme.badgeBg} ${item.theme.badgeText} ${item.theme.badgeBorder}`}>
                        {item.formatLabel}
                      </span>
                    </div>
                    <div>
                      <div className="font-bold text-sm text-white">{item.shortName}</div>
                      <div className="text-xs text-slate-400 mt-0.5 line-clamp-1">{item.name}</div>
                    </div>
                    {isSelected && (
                      <div className="absolute top-2 right-2 h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
                    )}
                  </button>
                )
              })}
            </div>

            {fieldErrors.discipline && (
              <p className="text-xs text-rose-400 mt-1">{fieldErrors.discipline}</p>
            )}

            {/* US-07 & US-08: Automatic Team Size and Map Pool Indicator */}
            {selectedDisciplineMeta && (
              <div className="mt-3 p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 text-xs space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400">Встановлений розмір команди (US-07):</span>
                  <span className="font-semibold text-slate-200">
                    {selectedDisciplineMeta.formatLabel} ({selectedDisciplineMeta.teamSize} {selectedDisciplineMeta.teamSize === 1 ? 'гравець' : 'гравців'} у складі)
                  </span>
                </div>

                {/* US-08: Map pool display for CS2 & Brawl Stars; hidden for Chess */}
                {selectedDisciplineMeta.defaultMaps.length > 0 ? (
                  <div className="pt-2 border-t border-slate-800">
                    <div className="text-slate-400 mb-1.5 flex items-center justify-between">
                      <span>Офіційний пул карт (US-08, призначається автоматично):</span>
                      <span className="text-slate-500">{selectedDisciplineMeta.defaultMaps.length} карт</span>
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {selectedDisciplineMeta.defaultMaps.map(mapName => (
                        <span
                          key={mapName}
                          className="px-2 py-0.5 bg-slate-800/80 border border-slate-700 rounded text-slate-300 font-mono text-[11px]"
                        >
                          {mapName}
                        </span>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="pt-2 border-t border-slate-800 text-slate-400 flex items-center gap-1.5">
                    <span>ℹ️ Для Chess.com пул карт не застосовується (US-08).</span>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Tournament Name & Description */}
          <div className="space-y-4">
            <div>
              <label htmlFor="tournament-name" className="block text-sm font-semibold text-slate-200 mb-1">
                2. Назва турніру <span className="text-rose-400">*</span>
              </label>
              <input
                id="tournament-name"
                type="text"
                value={name}
                onChange={e => {
                  setName(e.target.value)
                  if (fieldErrors.name) {
                    setFieldErrors(prev => {
                      const next = { ...prev }
                      delete next.name
                      return next
                    })
                  }
                }}
                placeholder="Наприклад: KMA CS2 Autumn Cup 2026"
                className={`w-full px-3.5 py-2.5 bg-slate-950 border rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none transition ${
                  fieldErrors.name ? 'border-rose-500 focus:ring-1 focus:ring-rose-500' : 'border-slate-800 focus:border-indigo-500'
                }`}
              />
              {fieldErrors.name && (
                <p className="text-xs text-rose-400 mt-1">{fieldErrors.name}</p>
              )}
            </div>

            <div>
              <label htmlFor="tournament-desc" className="block text-sm font-semibold text-slate-200 mb-1">
                Опис та регламент <span className="text-xs font-normal text-slate-400">(опціонально)</span>
              </label>
              <textarea
                id="tournament-desc"
                value={description}
                onChange={e => setDescription(e.target.value)}
                rows={2}
                placeholder="Правила, розклад овертаймів, додаткові примітки для учасників..."
                className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition resize-none"
              />
            </div>
          </div>

          {/* Step 3: Format & Slot limit (US-09) */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-sm font-semibold text-slate-200 flex items-center gap-2">
                <span>3. Формат сітки та ліміт слотів</span>
                <span className="text-rose-400">*</span>
              </label>
              <span className="text-xs text-slate-400">Формат: Single Elimination</span>
            </div>

            <div className="grid grid-cols-3 gap-3">
              {SLOT_OPTIONS.map(slots => {
                const isSelected = slotLimit === slots
                return (
                  <button
                    key={slots}
                    type="button"
                    onClick={() => handleSelectSlotLimit(slots)}
                    className={`py-3 px-4 rounded-xl border text-center transition ${
                      isSelected
                        ? 'bg-indigo-600/20 border-indigo-500 border-2 text-white font-bold shadow-md shadow-indigo-500/10'
                        : 'bg-slate-950/60 border-slate-800 text-slate-300 hover:border-slate-700 hover:bg-slate-800/40'
                    }`}
                  >
                    <div className="text-base font-bold">{slots} команд</div>
                    <div className="text-[11px] text-slate-400 mt-0.5">
                      {slots === 8 ? '3 раунди' : slots === 16 ? '4 раунди' : '5 раундів'}
                    </div>
                  </button>
                )
              })}
            </div>
            {fieldErrors.slotLimit && (
              <p className="text-xs text-rose-400 mt-1">{fieldErrors.slotLimit}</p>
            )}
          </div>

          {/* Step 4: Schedule inputs & validation indicator (US-10) */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-sm font-semibold text-slate-200 flex items-center gap-2">
                <span>4. Графік турніру (Київський час)</span>
                <span className="text-rose-400">*</span>
              </label>
              <button
                type="button"
                onClick={handleFillDemoSchedule}
                className="text-xs text-indigo-400 hover:text-indigo-300 hover:underline flex items-center gap-1"
              >
                <span>⚡ Заповнити рекомендованим розкладом</span>
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label htmlFor="reg-deadline" className="block text-xs text-slate-400 mb-1">
                  1. Дедлайн заявок
                </label>
                <input
                  id="reg-deadline"
                  type="datetime-local"
                  value={deadlineInput}
                  onChange={e => {
                    setDeadlineInput(e.target.value)
                    setFieldErrors(prev => {
                      const next = { ...prev }
                      delete next.registrationDeadlineAt
                      delete next.schedule
                      return next
                    })
                  }}
                  className={`w-full px-3 py-2 bg-slate-950 border rounded-xl text-xs text-white focus:outline-none transition ${
                    fieldErrors.registrationDeadlineAt ? 'border-rose-500' : 'border-slate-800 focus:border-indigo-500'
                  }`}
                />
              </div>

              <div>
                <label htmlFor="checkin-open" className="block text-xs text-slate-400 mb-1">
                  2. Відкриття Check-in
                </label>
                <input
                  id="checkin-open"
                  type="datetime-local"
                  value={checkInInput}
                  onChange={e => {
                    setCheckInInput(e.target.value)
                    setFieldErrors(prev => {
                      const next = { ...prev }
                      delete next.checkInOpensAt
                      delete next.schedule
                      return next
                    })
                  }}
                  className={`w-full px-3 py-2 bg-slate-950 border rounded-xl text-xs text-white focus:outline-none transition ${
                    fieldErrors.checkInOpensAt ? 'border-rose-500' : 'border-slate-800 focus:border-indigo-500'
                  }`}
                />
              </div>

              <div>
                <label htmlFor="start-time" className="block text-xs text-slate-400 mb-1">
                  3. Початок змагань
                </label>
                <input
                  id="start-time"
                  type="datetime-local"
                  value={startInput}
                  onChange={e => {
                    setStartInput(e.target.value)
                    setFieldErrors(prev => {
                      const next = { ...prev }
                      delete next.startTimeAt
                      delete next.schedule
                      return next
                    })
                  }}
                  className={`w-full px-3 py-2 bg-slate-950 border rounded-xl text-xs text-white focus:outline-none transition ${
                    fieldErrors.startTimeAt ? 'border-rose-500' : 'border-slate-800 focus:border-indigo-500'
                  }`}
                />
              </div>
            </div>

            {/* Schedule validation timeline */}
            {hasScheduleInputs && (
              <div className={`p-3 rounded-xl border text-xs flex items-center justify-between ${
                scheduleValidation.isValid
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                  : 'bg-amber-500/10 border-amber-500/30 text-amber-300'
              }`}>
                <div className="flex items-center gap-2">
                  <span>{scheduleValidation.isValid ? '✓' : '⚠️'}</span>
                  <span>
                    {scheduleValidation.isValid
                      ? 'Хронологія правильна: Дедлайн < Check-in < Старт'
                      : scheduleValidation.errors.join(' ')}
                  </span>
                </div>
              </div>
            )}

            {fieldErrors.schedule && (
              <p className="text-xs text-rose-400 mt-1">{fieldErrors.schedule}</p>
            )}
          </div>

          {/* Step 5: Optional CS2 Server Connect (US-25 / ADR-0008) */}
          {discipline === 'CS2' && (
            <div className="space-y-1.5 p-4 rounded-xl bg-slate-950/60 border border-slate-800">
              <label htmlFor="cs2-connect" className="block text-sm font-semibold text-amber-400">
                Рядок підключення до CS2 сервера <span className="text-xs text-slate-400 font-normal">(опціонально)</span>
              </label>
              <p className="text-xs text-slate-400">
                Команда консолі для гравців під час матчу.
              </p>
              <input
                id="cs2-connect"
                type="text"
                value={cs2ServerConnect}
                onChange={e => setCs2ServerConnect(e.target.value)}
                placeholder="connect 194.44.12.5:27015; password kma_cup"
                className="w-full px-3.5 py-2 bg-slate-900 border border-slate-800 rounded-xl text-sm font-mono text-amber-200 placeholder-slate-600 focus:outline-none focus:border-amber-500 transition"
              />
            </div>
          )}

          {/* Action buttons */}
          <div className="pt-4 border-t border-slate-800 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 rounded-xl border border-slate-800 text-slate-300 hover:bg-slate-800 text-sm font-medium transition disabled:opacity-50"
            >
              Скасувати
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold transition flex items-center gap-2 shadow-lg shadow-indigo-600/30 disabled:opacity-60 cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <span className="h-4 w-4 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                  <span>Створення турніру...</span>
                </>
              ) : (
                <>
                  <span>🌪️ Створити турнір</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

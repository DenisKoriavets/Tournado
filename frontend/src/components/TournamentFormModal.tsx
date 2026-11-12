import React, { useEffect, useState } from 'react'
import { ApiError, createTournament } from '../api/tournaments'
import type {
  CreateTournamentPayload,
  Discipline,
  ProblemDetail,
  SlotLimit,
  Tournament
} from '../types/tournament'
import { DISCIPLINE_CONFIG } from '../types/tournament'
import { generateDemoSchedule, localInputToIso, validateSchedule } from '../utils/date'
import { DisciplineLogo } from './DisciplineLogo'
import {
  AlertCircleIcon,
  ArrowLeftIcon,
  CalendarIcon,
  CheckIcon,
  ChevronRightIcon,
  ClockIcon,
  CloseIcon,
  PlusIcon,
  ServerIcon,
  SparklesIcon,
  TrophyIcon,
  UsersIcon
} from './icons/Icons'

export interface TournamentFormModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (tournament: Tournament) => void
}

const SLOT_OPTIONS: SlotLimit[] = [8, 16, 32]

export const TournamentFormModal: React.FC<TournamentFormModalProps> = ({
  isOpen,
  onClose,
  onSuccess
}) => {
  const [step, setStep] = useState<1 | 2>(1)

  // Step 1 state
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [discipline, setDiscipline] = useState<Discipline | null>(null)

  // Step 2 state
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
      setStep(1)
      setApiError(null)
      setFieldErrors({})
    }
  }, [isOpen])

  // Escape to close
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown)
    }
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  const handleFillDemoSchedule = () => {
    const demo = generateDemoSchedule()
    setDeadlineInput(demo.deadlineInput)
    setCheckInInput(demo.checkInInput)
    setStartInput(demo.startInput)
    setFieldErrors((prev) => {
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
    setFieldErrors((prev) => {
      const next = { ...prev }
      delete next.discipline
      return next
    })
  }

  const handleSelectSlotLimit = (slots: SlotLimit) => {
    setSlotLimit(slots)
    setFieldErrors((prev) => {
      const next = { ...prev }
      delete next.slotLimit
      return next
    })
  }

  // Live schedule validation indicator
  const scheduleValidation = validateSchedule(deadlineInput, checkInInput, startInput)
  const hasScheduleInputs = Boolean(deadlineInput && checkInInput && startInput)

  const validateStep1 = (): boolean => {
    const errors: Record<string, string> = {}
    if (!discipline) {
      errors.discipline = 'Оберіть ігрову дисципліну.'
    }
    if (!name.trim()) {
      errors.name = 'Вкажіть назву турніру (мінімум 3 символи).'
    } else if (name.trim().length < 3) {
      errors.name = 'Назва турніру занадто коротка (мінімум 3 символи).'
    }

    setFieldErrors((prev) => ({ ...prev, ...errors }))
    return Object.keys(errors).length === 0
  }

  const handleGoToStep2 = () => {
    if (validateStep1()) {
      setStep(2)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setApiError(null)

    // Validate step 1 again just in case
    if (!validateStep1()) {
      setStep(1)
      return
    }

    const errors: Record<string, string> = {}

    if (!slotLimit) {
      errors.slotLimit = 'Оберіть максимальну кількість слотів для команд.'
    }

    if (!deadlineInput) errors.registrationDeadlineAt = 'Вкажіть дедлайн подачі заявок.'
    if (!checkInInput) errors.checkInOpensAt = 'Вкажіть час відкриття Check-in.'
    if (!startInput) errors.startTimeAt = 'Вкажіть час початку змагань.'

    if (deadlineInput && checkInInput && startInput) {
      const validation = validateSchedule(deadlineInput, checkInInput, startInput)
      if (!validation.isValid) {
        errors.schedule =
          validation.errors[0] ||
          'Помилка валідації графіку турніру.'
      }
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setIsSubmitting(true)

    try {
      const payload: CreateTournamentPayload = {
        name: name.trim(),
        description: description.trim() ? description.trim() : undefined,
        discipline: discipline!,
        slotLimit: slotLimit!,
        registrationDeadlineAt: localInputToIso(deadlineInput),
        checkInOpensAt: localInputToIso(checkInInput),
        startTimeAt: localInputToIso(startInput),
        cs2ServerConnect:
          discipline === 'CS2' && cs2ServerConnect.trim()
            ? cs2ServerConnect.trim()
            : undefined
      }

      const created = await createTournament(payload)
      onSuccess(created)
      onClose()
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setApiError(err.problem)
        if (err.problem.invalidParams && err.problem.invalidParams.length > 0) {
          const mapped: Record<string, string> = {}
          for (const p of err.problem.invalidParams) {
            mapped[p.name] = p.reason
          }
          setFieldErrors((prev) => ({ ...prev, ...mapped }))
        }
      } else {
        setApiError({
          title: 'Помилка мережі',
          status: 500,
          detail: 'Не вдалося створити турнір. Перевірте зв’язок із Core API.'
        })
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedDisciplineMeta = discipline ? DISCIPLINE_CONFIG[discipline] : null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto bg-black/75 backdrop-blur-sm animate-fadeIn"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-2xl bg-[#121620] border border-[#1E2536] rounded-2xl shadow-2xl overflow-hidden my-8">
        {/* Top Header */}
        <div className="px-6 py-5 border-b border-[#1E2536] flex items-center justify-between bg-[#0E121A]">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-orange-500/10 border border-orange-500/30 text-orange-400">
              <TrophyIcon className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white tracking-tight">
                Створити новий турнір
              </h2>
              <p className="text-xs text-slate-400">
                Формат Single Elimination: вибір дисципліни, сітки та розкладу
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition"
            aria-label="Закрити"
          >
            <CloseIcon className="w-5 h-5" />
          </button>
        </div>

        {/* Wizard Step Navigation */}
        <div className="px-6 py-3 bg-[#0B0E14] border-b border-[#1E2536] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setStep(1)}
              className={`flex items-center gap-2 text-xs font-semibold px-3 py-1.5 rounded-lg transition ${
                step === 1
                  ? 'bg-orange-500/10 text-orange-400 border border-orange-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <span className="w-5 h-5 rounded-full bg-slate-800 flex items-center justify-center text-[10px] font-mono">
                1
              </span>
              <span>Дисципліна та назва</span>
            </button>

            <span className="text-slate-600">→</span>

            <button
              type="button"
              onClick={() => {
                if (validateStep1()) setStep(2)
              }}
              className={`flex items-center gap-2 text-xs font-semibold px-3 py-1.5 rounded-lg transition ${
                step === 2
                  ? 'bg-orange-500/10 text-orange-400 border border-orange-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <span className="w-5 h-5 rounded-full bg-slate-800 flex items-center justify-center text-[10px] font-mono">
                2
              </span>
              <span>Слоти та розклад</span>
            </button>
          </div>

          <div className="text-xs font-mono text-slate-500">Крок {step} з 2</div>
        </div>

        {/* RFC 7807 Error Banner */}
        {apiError && (
          <div className="mx-6 mt-4 p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-200 animate-fadeIn">
            <div className="flex items-start gap-3">
              <AlertCircleIcon className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />
              <div className="flex-1 text-xs">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm text-rose-300">{apiError.title}</span>
                  {apiError.code && (
                    <span className="font-mono text-[10px] px-2 py-0.5 bg-rose-950/80 rounded border border-rose-800/80 text-rose-300">
                      {apiError.code}
                    </span>
                  )}
                </div>
                <p className="mt-1 text-slate-300">{apiError.detail}</p>
                {apiError.invalidParams && apiError.invalidParams.length > 0 && (
                  <ul className="mt-2 list-disc list-inside text-xs space-y-0.5 text-rose-300 font-mono">
                    {apiError.invalidParams.map((p, idx) => (
                      <li key={idx}>
                        <span className="font-semibold">{p.name}</span>: {p.reason}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {step === 1 && (
            <div className="space-y-6">
              {/* Discipline selection */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-mono uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
                    <span>1. Оберіть ігрову дисципліну</span>
                    <span className="text-orange-400">*</span>
                  </label>
                  <span className="text-[11px] text-slate-500">Розмір команди встановлюється автоматично</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {(Object.keys(DISCIPLINE_CONFIG) as Discipline[]).map((key) => {
                    const item = DISCIPLINE_CONFIG[key]
                    const isSelected = discipline === key
                    return (
                      <button
                        key={key}
                        type="button"
                        onClick={() => handleSelectDiscipline(key)}
                        className={`relative p-4 rounded-xl border text-left transition flex flex-col justify-between ${
                          isSelected
                            ? `${item.theme.badgeBg} border-orange-500 border-2 shadow-lg shadow-orange-500/10`
                            : 'bg-[#0B0E14] border-[#1E2536] hover:border-slate-700 hover:bg-[#151A26]'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-3">
                          <DisciplineLogo discipline={key} className="w-10 h-10 rounded-xl object-cover shadow-md" />
                          <span
                            className={`text-xs font-mono font-bold px-2 py-0.5 rounded-md border ${item.theme.badgeBg} ${item.theme.badgeText} ${item.theme.badgeBorder}`}
                          >
                            {item.formatLabel}
                          </span>
                        </div>
                        <div>
                          <div className="font-bold text-sm text-white">{item.shortName}</div>
                          <div className="text-[11px] text-slate-400 mt-0.5 line-clamp-1">
                            {item.category}
                          </div>
                        </div>
                        {isSelected && (
                          <div className="absolute top-2 right-2 h-2 w-2 rounded-full bg-orange-400 animate-pulse" />
                        )}
                      </button>
                    )
                  })}
                </div>

                {fieldErrors.discipline && (
                  <p className="text-xs text-rose-400 mt-1">{fieldErrors.discipline}</p>
                )}

                {/* Selected Discipline Info */}
                {selectedDisciplineMeta && (
                  <div className="mt-3 p-3.5 rounded-xl bg-[#0B0E14] border border-[#1E2536] text-xs space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">Встановлений розмір команди:</span>
                      <span className="font-mono font-semibold text-slate-200">
                        {selectedDisciplineMeta.formatLabel} ({selectedDisciplineMeta.teamSize}{' '}
                        {selectedDisciplineMeta.teamSize === 1 ? 'гравець' : 'гравців'} у складі)
                      </span>
                    </div>

                    {selectedDisciplineMeta.defaultMaps.length > 0 ? (
                      <div className="pt-2 border-t border-[#1E2536]">
                        <div className="text-slate-400 mb-1.5 flex items-center justify-between">
                          <span>Офіційний пул карт (призначається автоматично):</span>
                          <span className="font-mono text-slate-500">
                            {selectedDisciplineMeta.defaultMaps.length} карт
                          </span>
                        </div>
                        <div className="flex flex-wrap gap-1.5">
                          {selectedDisciplineMeta.defaultMaps.map((mapName) => (
                            <span
                              key={mapName}
                              className="px-2 py-0.5 bg-[#121620] border border-[#263147] rounded text-slate-300 font-mono text-[11px]"
                            >
                              {mapName}
                            </span>
                          ))}
                        </div>
                      </div>
                    ) : (
                      <div className="pt-2 border-t border-[#1E2536] text-slate-400 flex items-center gap-1.5">
                        <span>Для Chess.com пул карт не застосовується (стандартні шахові правила).</span>
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Tournament Name & Description */}
              <div className="space-y-4">
                <div>
                  <label
                    htmlFor="tournament-name"
                    className="block text-xs font-mono uppercase tracking-wider text-slate-300 mb-1.5"
                  >
                    2. Назва турніру <span className="text-orange-400">*</span>
                  </label>
                  <input
                    id="tournament-name"
                    type="text"
                    value={name}
                    onChange={(e) => {
                      setName(e.target.value)
                      if (fieldErrors.name) {
                        setFieldErrors((prev) => {
                          const next = { ...prev }
                          delete next.name
                          return next
                        })
                      }
                    }}
                    placeholder="Наприклад: KMA CS2 Autumn Cup 2026"
                    className={`w-full px-4 py-2.5 bg-[#0B0E14] border rounded-xl text-sm text-white placeholder-slate-600 focus:outline-none transition ${
                      fieldErrors.name
                        ? 'border-rose-500 focus:ring-1 focus:ring-rose-500'
                        : 'border-[#1E2536] focus:border-orange-500'
                    }`}
                  />
                  {fieldErrors.name && (
                    <p className="text-xs text-rose-400 mt-1">{fieldErrors.name}</p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="tournament-desc"
                    className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1.5"
                  >
                    3. Опис та регламент змагань (необов’язково)
                  </label>
                  <textarea
                    id="tournament-desc"
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Вкажіть правила овертайму, посилання на Discord або додаткову інформацію для капітанів..."
                    className="w-full px-4 py-2.5 bg-[#0B0E14] border border-[#1E2536] focus:border-orange-500 rounded-xl text-sm text-white placeholder-slate-600 focus:outline-none transition"
                  />
                </div>
              </div>

              <div className="pt-2 flex justify-end">
                <button
                  type="button"
                  onClick={handleGoToStep2}
                  className="px-6 py-2.5 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition flex items-center gap-2 shadow-lg shadow-orange-500/20"
                >
                  <span>Далі: Слоти та розклад</span>
                  <ChevronRightIcon className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-6">
              {/* Slot limit */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-mono uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
                    <UsersIcon className="w-4 h-4 text-orange-400" />
                    <span>Ліміт слотів для команд</span>
                    <span className="text-orange-400">*</span>
                  </label>
                  <span className="text-[11px] text-slate-500 font-mono">Single Elimination (8, 16, 32)</span>
                </div>

                <div className="grid grid-cols-3 gap-3">
                  {SLOT_OPTIONS.map((slots) => {
                    const isSelected = slotLimit === slots
                    return (
                      <button
                        key={slots}
                        type="button"
                        onClick={() => handleSelectSlotLimit(slots)}
                        className={`py-3 px-4 rounded-xl border text-center transition flex flex-col items-center justify-center ${
                          isSelected
                            ? 'bg-orange-500/10 border-orange-500 border-2 text-white shadow-lg shadow-orange-500/10'
                            : 'bg-[#0B0E14] border-[#1E2536] text-slate-300 hover:border-slate-700'
                        }`}
                      >
                        <span className="text-lg font-mono font-extrabold">{slots}</span>
                        <span className="text-xs font-medium text-slate-400 mt-0.5">{slots} команд</span>
                      </button>
                    )
                  })}
                </div>
                {fieldErrors.slotLimit && (
                  <p className="text-xs text-rose-400 mt-1">{fieldErrors.slotLimit}</p>
                )}
              </div>

              {/* Schedule Inputs */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-mono uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
                    <ClockIcon className="w-4 h-4 text-orange-400" />
                    <span>Графік проведення турніру</span>
                    <span className="text-orange-400">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={handleFillDemoSchedule}
                    className="text-xs font-semibold text-orange-400 hover:text-orange-300 transition flex items-center gap-1"
                  >
                    <SparklesIcon className="w-3.5 h-3.5" />
                    <span>Заповнити рекомендованим розкладом</span>
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1 font-mono">
                      1. Дедлайн реєстрації:
                    </label>
                    <input
                      type="datetime-local"
                      value={deadlineInput}
                      onChange={(e) => setDeadlineInput(e.target.value)}
                      className="w-full px-3 py-2 bg-[#0B0E14] border border-[#1E2536] focus:border-orange-500 rounded-xl text-xs font-mono text-white focus:outline-none"
                    />
                    {fieldErrors.registrationDeadlineAt && (
                      <p className="text-[11px] text-rose-400 mt-1">
                        {fieldErrors.registrationDeadlineAt}
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1 font-mono">
                      2. Відкриття Check-in:
                    </label>
                    <input
                      type="datetime-local"
                      value={checkInInput}
                      onChange={(e) => setCheckInInput(e.target.value)}
                      className="w-full px-3 py-2 bg-[#0B0E14] border border-[#1E2536] focus:border-orange-500 rounded-xl text-xs font-mono text-white focus:outline-none"
                    />
                    {fieldErrors.checkInOpensAt && (
                      <p className="text-[11px] text-rose-400 mt-1">
                        {fieldErrors.checkInOpensAt}
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="block text-[11px] text-slate-400 mb-1 font-mono">
                      3. Початок турніру:
                    </label>
                    <input
                      type="datetime-local"
                      value={startInput}
                      onChange={(e) => setStartInput(e.target.value)}
                      className="w-full px-3 py-2 bg-[#0B0E14] border border-[#1E2536] focus:border-orange-500 rounded-xl text-xs font-mono text-white focus:outline-none"
                    />
                    {fieldErrors.startTimeAt && (
                      <p className="text-[11px] text-rose-400 mt-1">
                        {fieldErrors.startTimeAt}
                      </p>
                    )}
                  </div>
                </div>

                {fieldErrors.schedule && (
                  <p className="text-xs text-rose-400 font-mono mt-1">{fieldErrors.schedule}</p>
                )}

                {hasScheduleInputs && scheduleValidation.isValid && (
                  <div className="p-2.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs flex items-center gap-2 font-mono">
                    <CheckIcon className="w-4 h-4 text-emerald-400" />
                    <span>Послідовність часу валідна: Дедлайн &lt; Check-in &lt; Старт</span>
                  </div>
                )}
              </div>

              {/* CS2 Server Connect (if CS2) */}
              {discipline === 'CS2' && (
                <div className="space-y-1.5 p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536]">
                  <label
                    htmlFor="cs2-connect"
                    className="block text-xs font-mono uppercase tracking-wider text-amber-400 flex items-center gap-1.5"
                  >
                    <ServerIcon className="w-4 h-4" />
                    <span>Рядок підключення до CS2 сервера (необов’язково)</span>
                  </label>
                  <input
                    id="cs2-connect"
                    type="text"
                    value={cs2ServerConnect}
                    onChange={(e) => setCs2ServerConnect(e.target.value)}
                    placeholder="connect 194.44.12.5:27015; password kma_cup"
                    className="w-full px-3.5 py-2 bg-[#121620] border border-[#263147] focus:border-amber-500 rounded-lg text-xs font-mono text-amber-300 placeholder-slate-600 focus:outline-none"
                  />
                  <p className="text-[11px] text-slate-500">
                    Рядок буде доступний капітанам команд у деталях турніру.
                  </p>
                </div>
              )}

              {/* Action Buttons */}
              <div className="pt-4 border-t border-[#1E2536] flex items-center justify-between">
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="px-4 py-2.5 rounded-xl border border-slate-700 hover:bg-slate-800 text-slate-300 text-xs font-semibold transition flex items-center gap-2"
                >
                  <ArrowLeftIcon className="w-4 h-4" />
                  <span>Назад до назви</span>
                </button>

                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={onClose}
                    className="px-4 py-2.5 rounded-xl text-slate-400 hover:text-white text-xs font-semibold transition"
                  >
                    Скасувати
                  </button>

                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="px-6 py-2.5 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition flex items-center gap-2 shadow-lg shadow-orange-500/20"
                  >
                    <PlusIcon className="w-4 h-4" />
                    <span>{isSubmitting ? 'Створення турніру...' : 'Створити турнір'}</span>
                  </button>
                </div>
              </div>
            </div>
          )}
        </form>
      </div>
    </div>
  )
}

// Re-export as TournamentForm for backward compatibility
export const TournamentForm = TournamentFormModal

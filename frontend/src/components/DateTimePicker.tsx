import React, { useEffect, useMemo, useState } from 'react'
import {
  CalendarIcon,
  CheckIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  ClockIcon,
  CloseIcon
} from './icons/Icons'

export interface DateTimePickerProps {
  label: string
  value: string // Format: YYYY-MM-DDTHH:mm
  onChange: (value: string) => void
  hasError?: boolean
}

const MONTH_NAMES = [
  'Січень',
  'Лютий',
  'Березень',
  'Квітень',
  'Травень',
  'Червень',
  'Липень',
  'Серпень',
  'Вересень',
  'Жовтень',
  'Листопад',
  'Грудень'
]

const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Нд']

const HOURS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'))
const MINUTES = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'))

function parseLocalValue(value: string): Date {
  if (value) {
    const parsed = new Date(value)
    if (!Number.isNaN(parsed.getTime())) {
      return parsed
    }
  }
  const fallback = new Date()
  fallback.setMinutes(0, 0, 0)
  fallback.setHours(fallback.getHours() + 1)
  return fallback
}

function formatDisplay(value: string): string | null {
  if (!value) return null
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(value)
  if (!match) return value
  const [, year, month, day, hour, minute] = match
  return `${day}.${month}.${year}, ${hour}:${minute}`
}

function toLocalInputString(year: number, month: number, day: number, hour: string, minute: string): string {
  const mm = String(month + 1).padStart(2, '0')
  const dd = String(day).padStart(2, '0')
  return `${year}-${mm}-${dd}T${hour}:${minute}`
}

export const DateTimePicker: React.FC<DateTimePickerProps> = ({
  label,
  value,
  onChange,
  hasError = false
}) => {
  const [isOpen, setIsOpen] = useState(false)

  const initialDate = useMemo(() => parseLocalValue(value), [value])
  const [viewYear, setViewYear] = useState(initialDate.getFullYear())
  const [viewMonth, setViewMonth] = useState(initialDate.getMonth())
  const [selectedYear, setSelectedYear] = useState(initialDate.getFullYear())
  const [selectedMonth, setSelectedMonth] = useState(initialDate.getMonth())
  const [selectedDay, setSelectedDay] = useState(initialDate.getDate())
  const [selectedHour, setSelectedHour] = useState(String(initialDate.getHours()).padStart(2, '0'))
  const [selectedMinute, setSelectedMinute] = useState(String(initialDate.getMinutes()).padStart(2, '0'))

  const syncDraftFromValue = () => {
    const base = parseLocalValue(value)
    setViewYear(base.getFullYear())
    setViewMonth(base.getMonth())
    setSelectedYear(base.getFullYear())
    setSelectedMonth(base.getMonth())
    setSelectedDay(base.getDate())
    setSelectedHour(String(base.getHours()).padStart(2, '0'))
    setSelectedMinute(String(base.getMinutes()).padStart(2, '0'))
  }

  const handleOpen = () => {
    syncDraftFromValue()
    setIsOpen(true)
  }

  // Capture Escape so closing the calendar does not close the parent tournament modal
  useEffect(() => {
    if (!isOpen) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation()
        e.stopImmediatePropagation()
        setIsOpen(false)
      }
    }
    window.addEventListener('keydown', handleKeyDown, true)
    return () => window.removeEventListener('keydown', handleKeyDown, true)
  }, [isOpen])

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
  // Convert Sunday=0..Saturday=6 into Monday=0..Sunday=6
  const firstWeekday = (new Date(viewYear, viewMonth, 1).getDay() + 6) % 7

  const today = new Date()
  const isToday = (day: number) =>
    today.getFullYear() === viewYear &&
    today.getMonth() === viewMonth &&
    today.getDate() === day

  const isSelectedDay = (day: number) =>
    selectedYear === viewYear &&
    selectedMonth === viewMonth &&
    selectedDay === day

  const handlePrevMonth = () => {
    if (viewMonth === 0) {
      setViewYear((y) => y - 1)
      setViewMonth(11)
    } else {
      setViewMonth((m) => m - 1)
    }
  }

  const handleNextMonth = () => {
    if (viewMonth === 11) {
      setViewYear((y) => y + 1)
      setViewMonth(0)
    } else {
      setViewMonth((m) => m + 1)
    }
  }

  const handleSelectDay = (day: number) => {
    setSelectedYear(viewYear)
    setSelectedMonth(viewMonth)
    setSelectedDay(day)
  }

  const handleApply = () => {
    onChange(toLocalInputString(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute))
    setIsOpen(false)
  }

  const displayValue = formatDisplay(value)

  return (
    <div>
      <button
        type="button"
        onClick={handleOpen}
        aria-label={`Обрати дату та час: ${label}`}
        className={`group w-full px-3 py-2 bg-[#0B0E14] border rounded-xl text-left transition flex items-center justify-between gap-2 cursor-pointer ${
          hasError
            ? 'border-rose-500 hover:border-rose-400'
            : 'border-[#1E2536] hover:border-orange-500/60 focus:border-orange-500 focus:outline-none'
        }`}
      >
        <span
          className={`text-xs font-mono truncate ${
            displayValue ? 'text-white' : 'text-slate-500'
          }`}
        >
          {displayValue ?? 'Оберіть дату і час'}
        </span>

        <span
          aria-hidden="true"
          className="p-1.5 rounded-lg bg-[#181F2E] border border-[#2A354D] text-slate-200 group-hover:bg-orange-500 group-hover:border-orange-500 group-hover:text-white transition shrink-0"
        >
          <CalendarIcon className="w-3.5 h-3.5" />
        </span>
      </button>

      {isOpen && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/60 backdrop-blur-[2px] animate-fadeIn"
          onClick={() => setIsOpen(false)}
          role="dialog"
          aria-modal="true"
          aria-label={`Календар: ${label}`}
        >
          <div
            className="w-full max-w-[340px] bg-[#121620] border border-[#263147] rounded-2xl shadow-2xl overflow-hidden animate-scaleIn"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="px-4 py-3 bg-[#0E121A] border-b border-[#1E2536] flex items-center justify-between">
              <div className="flex items-center gap-2 min-w-0">
                <CalendarIcon className="w-4 h-4 text-orange-400 shrink-0" />
                <span className="text-xs font-mono font-semibold text-slate-200 truncate">
                  {label}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                aria-label="Закрити календар"
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                <CloseIcon className="w-4 h-4" />
              </button>
            </div>

            {/* Month Switcher */}
            <div className="p-4 space-y-3">
              <div className="flex items-center justify-between">
                <button
                  type="button"
                  onClick={handlePrevMonth}
                  aria-label="Попередній місяць"
                  className="p-1.5 rounded-lg bg-[#0B0E14] border border-[#1E2536] text-slate-300 hover:text-white hover:border-slate-600 transition"
                >
                  <ChevronLeftIcon className="w-4 h-4" />
                </button>
                <span className="text-sm font-bold text-white">
                  {MONTH_NAMES[viewMonth]} {viewYear}
                </span>
                <button
                  type="button"
                  onClick={handleNextMonth}
                  aria-label="Наступний місяць"
                  className="p-1.5 rounded-lg bg-[#0B0E14] border border-[#1E2536] text-slate-300 hover:text-white hover:border-slate-600 transition"
                >
                  <ChevronRightIcon className="w-4 h-4" />
                </button>
              </div>

              {/* Weekday Headers */}
              <div className="grid grid-cols-7 gap-1 text-center">
                {WEEKDAYS.map((dayName) => (
                  <div
                    key={dayName}
                    className="text-[10px] font-mono uppercase text-slate-500 py-1"
                  >
                    {dayName}
                  </div>
                ))}
              </div>

              {/* Calendar Grid */}
              <div className="grid grid-cols-7 gap-1">
                {Array.from({ length: firstWeekday }).map((_, idx) => (
                  <div key={`empty-${idx}`} />
                ))}
                {Array.from({ length: daysInMonth }, (_, idx) => {
                  const day = idx + 1
                  const selected = isSelectedDay(day)
                  const currentToday = isToday(day)
                  return (
                    <button
                      key={day}
                      type="button"
                      onClick={() => handleSelectDay(day)}
                      className={`h-8 rounded-lg text-xs font-mono transition flex items-center justify-center cursor-pointer ${
                        selected
                          ? 'bg-orange-500 text-white font-bold shadow-md shadow-orange-500/25'
                          : currentToday
                          ? 'bg-[#181F2E] text-orange-400 border border-orange-500/40 hover:bg-orange-500/20'
                          : 'text-slate-300 hover:bg-[#1A2130] hover:text-white'
                      }`}
                    >
                      {day}
                    </button>
                  )
                })}
              </div>

              {/* Time Selector */}
              <div className="pt-3 border-t border-[#1E2536] flex items-center justify-between gap-3">
                <div className="flex items-center gap-1.5 text-xs text-slate-400 font-mono">
                  <ClockIcon className="w-3.5 h-3.5 text-orange-400" />
                  <span>Час:</span>
                </div>

                <div className="flex items-center gap-1.5">
                  <select
                    aria-label="Години"
                    value={selectedHour}
                    onChange={(e) => setSelectedHour(e.target.value)}
                    className="bg-[#0B0E14] border border-[#263147] focus:border-orange-500 rounded-lg px-2.5 py-1.5 text-xs font-mono text-white focus:outline-none cursor-pointer"
                  >
                    {HOURS.map((h) => (
                      <option key={h} value={h}>
                        {h}
                      </option>
                    ))}
                  </select>
                  <span className="text-slate-400 font-mono font-bold">:</span>
                  <select
                    aria-label="Хвилини"
                    value={selectedMinute}
                    onChange={(e) => setSelectedMinute(e.target.value)}
                    className="bg-[#0B0E14] border border-[#263147] focus:border-orange-500 rounded-lg px-2.5 py-1.5 text-xs font-mono text-white focus:outline-none cursor-pointer"
                  >
                    {MINUTES.map((m) => (
                      <option key={m} value={m}>
                        {m}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* Footer Actions */}
            <div className="px-4 py-3 bg-[#0E121A] border-t border-[#1E2536] flex items-center justify-between gap-2">
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="px-3.5 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                Скасувати
              </button>
              <button
                type="button"
                onClick={handleApply}
                className="px-4 py-2 rounded-xl bg-orange-500 hover:bg-orange-400 text-white text-xs font-bold transition flex items-center gap-1.5 shadow-lg shadow-orange-500/20 cursor-pointer"
              >
                <CheckIcon className="w-3.5 h-3.5" />
                <span>Застосувати</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

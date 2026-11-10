/**
 * Formats an ISO-8601 date string into a user-friendly Ukrainian format.
 * E.g., "01 лист. 2026, 17:00"
 */
export function formatDateTime(isoString: string | undefined | null): string {
  if (!isoString) return '—'
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat('uk-UA', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Europe/Kyiv'
  }).format(date)
}

/**
 * Formats date into a short time-focused string. E.g. "17:00 (1 лист.)"
 */
export function formatShortDateTime(isoString: string | undefined | null): string {
  if (!isoString) return '—'
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat('uk-UA', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Europe/Kyiv'
  }).format(date)
}

/**
 * Converts a Date or ISO string into local "YYYY-MM-DDTHH:mm" for <input type="datetime-local">.
 */
export function toLocalDatetimeInputString(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

/**
 * Converts a datetime-local input string into ISO-8601 UTC string.
 */
export function localInputToIso(inputValue: string): string {
  if (!inputValue) return ''
  const date = new Date(inputValue)
  if (isNaN(date.getTime())) return ''
  return date.toISOString()
}

/**
 * Generate default demo dates in the future:
 * - Deadline: +2 hours
 * - Check-in: +3 hours
 * - Start time: +3.5 hours
 */
export function generateDemoSchedule(): {
  deadlineInput: string
  checkInInput: string
  startInput: string
} {
  const now = new Date()
  
  const deadline = new Date(now.getTime() + 2 * 60 * 60 * 1000)
  const checkIn = new Date(now.getTime() + 3 * 60 * 60 * 1000)
  const start = new Date(now.getTime() + 3.5 * 60 * 60 * 1000)

  return {
    deadlineInput: toLocalDatetimeInputString(deadline),
    checkInInput: toLocalDatetimeInputString(checkIn),
    startInput: toLocalDatetimeInputString(start)
  }
}

/**
 * Validates the chronological order of tournament schedule:
 * 1. deadline < check-in < start
 * 2. all dates must be strictly in the future
 */
export interface ScheduleValidationResult {
  isValid: boolean
  deadlineInPast: boolean
  checkInBeforeDeadline: boolean
  startBeforeCheckIn: boolean
  errors: string[]
}

export function validateSchedule(
  deadlineInput: string,
  checkInInput: string,
  startInput: string
): ScheduleValidationResult {
  const errors: string[] = []
  if (!deadlineInput || !checkInInput || !startInput) {
    return {
      isValid: false,
      deadlineInPast: false,
      checkInBeforeDeadline: false,
      startBeforeCheckIn: false,
      errors: ['Усі дати графіка є обов’язковими.']
    }
  }

  const now = Date.now()
  const deadlineMs = new Date(deadlineInput).getTime()
  const checkInMs = new Date(checkInInput).getTime()
  const startMs = new Date(startInput).getTime()

  const deadlineInPast = deadlineMs <= now
  const checkInBeforeDeadline = checkInMs <= deadlineMs
  const startBeforeCheckIn = startMs <= checkInMs

  if (deadlineInPast) {
    errors.push('Дедлайн реєстрації має бути в майбутньому.')
  }
  if (checkInBeforeDeadline) {
    errors.push('Відкриття Check-in має бути суворо пізніше за дедлайн реєстрації.')
  }
  if (startBeforeCheckIn) {
    errors.push('Початок змагань має бути суворо пізніше за відкриття Check-in.')
  }

  return {
    isValid: errors.length === 0,
    deadlineInPast,
    checkInBeforeDeadline,
    startBeforeCheckIn,
    errors
  }
}

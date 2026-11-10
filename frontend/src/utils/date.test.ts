import { describe, it, expect } from 'vitest'
import { formatDateTime, validateSchedule, generateDemoSchedule, localInputToIso, toLocalDatetimeInputString } from './date'

describe('date utils', () => {
  it('formats ISO datetime in Ukrainian locale', () => {
    const formatted = formatDateTime('2026-11-01T15:00:00Z')
    expect(formatted).toBeTruthy()
    expect(formatted).not.toBe('—')
  })

  it('returns fallback for empty date', () => {
    expect(formatDateTime(undefined)).toBe('—')
    expect(formatDateTime('')).toBe('—')
  })

  it('validates schedule order correctly', () => {
    const now = new Date()
    const deadline = toLocalDatetimeInputString(new Date(now.getTime() + 2 * 3600 * 1000))
    const checkIn = toLocalDatetimeInputString(new Date(now.getTime() + 3 * 3600 * 1000))
    const start = toLocalDatetimeInputString(new Date(now.getTime() + 4 * 3600 * 1000))

    const result = validateSchedule(deadline, checkIn, start)
    expect(result.isValid).toBe(true)
    expect(result.errors).toHaveLength(0)
  })

  it('rejects schedule with invalid order (check-in before deadline)', () => {
    const now = new Date()
    const deadline = toLocalDatetimeInputString(new Date(now.getTime() + 3 * 3600 * 1000))
    const checkIn = toLocalDatetimeInputString(new Date(now.getTime() + 2 * 3600 * 1000))
    const start = toLocalDatetimeInputString(new Date(now.getTime() + 4 * 3600 * 1000))

    const result = validateSchedule(deadline, checkIn, start)
    expect(result.isValid).toBe(false)
    expect(result.checkInBeforeDeadline).toBe(true)
  })

  it('rejects dates in the past', () => {
    const now = new Date()
    const past = toLocalDatetimeInputString(new Date(now.getTime() - 3600 * 1000))
    const checkIn = toLocalDatetimeInputString(new Date(now.getTime() + 2 * 3600 * 1000))
    const start = toLocalDatetimeInputString(new Date(now.getTime() + 3 * 3600 * 1000))

    const result = validateSchedule(past, checkIn, start)
    expect(result.isValid).toBe(false)
    expect(result.deadlineInPast).toBe(true)
  })

  it('generateDemoSchedule generates valid future schedule', () => {
    const demo = generateDemoSchedule()
    expect(demo.deadlineInput).toBeTruthy()
    expect(demo.checkInInput).toBeTruthy()
    expect(demo.startInput).toBeTruthy()

    const result = validateSchedule(demo.deadlineInput, demo.checkInInput, demo.startInput)
    expect(result.isValid).toBe(true)
  })

  it('converts local input string to valid ISO-8601 UTC string', () => {
    const iso = localInputToIso('2026-11-01T15:30')
    expect(iso).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
  })
})

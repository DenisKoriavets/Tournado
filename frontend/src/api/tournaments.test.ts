import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createTournament, getTournaments, getTournamentById, ApiError } from './tournaments'
import type { CreateTournamentPayload, Tournament } from '../types/tournament'

describe('tournaments api client', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('creates tournament successfully', async () => {
    const payload: CreateTournamentPayload = {
      name: 'Test Cup',
      discipline: 'CS2',
      slotLimit: 16,
      registrationDeadlineAt: '2026-11-01T15:00:00Z',
      checkInOpensAt: '2026-11-01T16:00:00Z',
      startTimeAt: '2026-11-01T17:00:00Z'
    }

    const mockResponse: Tournament = {
      id: 'mock-uuid',
      publicSlug: 'mock-uuid',
      name: 'Test Cup',
      discipline: 'CS2',
      teamSize: 5,
      format: 'SINGLE_ELIMINATION',
      slotLimit: 16,
      status: 'REGISTRATION_OPEN',
      registrationDeadlineAt: payload.registrationDeadlineAt,
      checkInOpensAt: payload.checkInOpensAt,
      startTimeAt: payload.startTimeAt,
      mapPool: ['de_mirage'],
      registeredTeamsCount: 0,
      createdAt: '2026-11-01T12:00:00Z'
    }

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse
    } as Response)

    const result = await createTournament(payload)
    expect(result.id).toBe('mock-uuid')
    expect(result.discipline).toBe('CS2')
    expect(result.slotLimit).toBe(16)
  })

  it('extracts RFC 7807 problem details upon error', async () => {
    const payload: CreateTournamentPayload = {
      name: 'Test Cup',
      discipline: 'CS2',
      slotLimit: 16,
      registrationDeadlineAt: '2026-11-01T15:00:00Z',
      checkInOpensAt: '2026-11-01T14:00:00Z', // invalid
      startTimeAt: '2026-11-01T17:00:00Z'
    }

    const rfcError = {
      type: 'https://tournado.ua/errors/invalid-schedule-order',
      title: 'Invalid schedule order',
      status: 422,
      detail: 'Registration deadline must be strictly before check-in opens.',
      code: 'INVALID_SCHEDULE_ORDER',
      invalidParams: [
        { name: 'checkInOpensAt', reason: 'Must be after registration deadline' }
      ]
    }

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 422,
      statusText: 'Unprocessable Entity',
      json: async () => rfcError
    } as Response)

    try {
      await createTournament(payload)
      expect.fail('Should have thrown an ApiError')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      const apiErr = err as ApiError
      expect(apiErr.problem.code).toBe('INVALID_SCHEDULE_ORDER')
      expect(apiErr.problem.status).toBe(422)
      expect(apiErr.problem.invalidParams).toHaveLength(1)
      expect(apiErr.problem.invalidParams?.[0].name).toBe('checkInOpensAt')
    }
  })

  it('fetches tournaments list', async () => {
    const mockList = [
      {
        id: '1',
        publicSlug: 'cup-1',
        name: 'Cup 1',
        discipline: 'CS2',
        teamSize: 5,
        format: 'SINGLE_ELIMINATION',
        slotLimit: 16,
        status: 'REGISTRATION_OPEN',
        registrationDeadlineAt: '2026-11-01T15:00:00Z',
        checkInOpensAt: '2026-11-01T16:00:00Z',
        startTimeAt: '2026-11-01T17:00:00Z',
        registeredTeamsCount: 0,
        createdAt: '2026-11-01T12:00:00Z'
      }
    ]

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockList
    } as Response)

    const list = await getTournaments()
    expect(list).toHaveLength(1)
    expect(list[0].name).toBe('Cup 1')
  })

  it('fetches tournament by id', async () => {
    const mockTournament = {
      id: 'tourn-123',
      publicSlug: 'tourn-123',
      name: 'Specific Cup',
      discipline: 'CHESS',
      teamSize: 1,
      format: 'SINGLE_ELIMINATION',
      slotLimit: 8,
      status: 'REGISTRATION_OPEN',
      registrationDeadlineAt: '2026-11-01T15:00:00Z',
      checkInOpensAt: '2026-11-01T16:00:00Z',
      startTimeAt: '2026-11-01T17:00:00Z',
      mapPool: [],
      registeredTeamsCount: 0,
      createdAt: '2026-11-01T12:00:00Z'
    }

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockTournament
    } as Response)

    const result = await getTournamentById('tourn-123')
    expect(result.id).toBe('tourn-123')
    expect(result.discipline).toBe('CHESS')
  })
})

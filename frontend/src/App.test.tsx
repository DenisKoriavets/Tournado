import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import * as authApi from './api/auth'
import { ApiError } from './api/http'
import * as registrationsApi from './api/registrations'
import * as teamsApi from './api/teams'
import * as tournamentsApi from './api/tournaments'
import type { Tournament } from './types/tournament'
import { savePendingInvite } from './utils/pendingInvite'

// US-11 (public tournament link) and US-05 checklist 2 (invite survives login) at the application level.
describe('App routing', () => {
  const tournament: Tournament = {
    id: '3a013827-3de6-4157-8e4d-b9f83a4d6b71',
    publicSlug: 'kma-cyber-clash-2026',
    name: 'KMA Cyber Clash 2026',
    description: 'Public cup',
    discipline: 'CS2',
    teamSize: 5,
    format: 'SINGLE_ELIMINATION',
    slotLimit: 16,
    status: 'REGISTRATION_OPEN',
    registrationDeadlineAt: '2027-11-20T16:00:00Z',
    checkInOpensAt: '2027-11-20T17:00:00Z',
    startTimeAt: '2027-11-20T18:00:00Z',
    registeredTeamsCount: 0,
    createdAt: '2026-11-10T12:00:00Z'
  }

  const anonymous = () =>
    vi.spyOn(authApi, 'getCurrentUser').mockRejectedValue(
      new ApiError({ title: 'Unauthorized', status: 401, detail: 'Login required', code: 'AUTH_UNAUTHORIZED' })
    )

  beforeEach(() => {
    window.localStorage.clear()
    vi.spyOn(tournamentsApi, 'getTournaments').mockResolvedValue([])
    vi.spyOn(registrationsApi, 'getTournamentRegistrations').mockResolvedValue({
      tournamentId: tournament.id,
      organizerId: 'organizer-1',
      slotLimit: 16,
      registeredCount: 0,
      rostersLocked: false,
      teams: []
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    window.history.replaceState({}, '', '/')
    window.localStorage.clear()
  })

  it('US-11 sc.2: an anonymous visitor opening /tournaments/{id} sees that tournament', async () => {
    anonymous()
    vi.spyOn(tournamentsApi, 'getTournamentById').mockResolvedValue(tournament)
    window.history.replaceState({}, '', `/tournaments/${tournament.id}`)

    render(<App />)

    expect(await screen.findByRole('heading', { name: 'KMA Cyber Clash 2026' })).toBeInTheDocument()
    expect(tournamentsApi.getTournamentById).toHaveBeenCalledWith(tournament.id)
  })

  it('US-11 sc.3: an unknown tournament id shows "Турнір не знайдено"', async () => {
    anonymous()
    vi.spyOn(tournamentsApi, 'getTournamentById').mockRejectedValue(
      new ApiError({ title: 'Not Found', status: 404, detail: 'Not found', code: 'TOURNAMENT_NOT_FOUND' })
    )
    window.history.replaceState({}, '', `/tournaments/${tournament.id}`)

    render(<App />)

    expect(await screen.findByText(/Турнір не знайдено/)).toBeInTheDocument()
  })

  it('US-05 checklist 2: after login lands on "/", the remembered invite is reopened', async () => {
    vi.spyOn(authApi, 'getCurrentUser').mockResolvedValue({
      id: 'user-3',
      email: 'member@ukma.edu.ua',
      steamId: null,
      displayName: 'NewPlayer',
      supercellPlayerTag: null,
      chessUsername: null,
      avatarUrl: null
    })
    vi.spyOn(teamsApi, 'previewTeamInvite').mockResolvedValue({
      token: 'inv_saved',
      teamId: 'team-1',
      teamName: 'Kyiv Mohyla Five',
      discipline: 'CS2',
      memberCount: 3,
      maxMembers: 5,
      isFull: false
    })
    savePendingInvite('inv_saved')

    render(<App />)

    await waitFor(() => expect(teamsApi.previewTeamInvite).toHaveBeenCalledWith('inv_saved'))
    expect(await screen.findByRole('button', { name: /Підтвердити вступ до команди/i })).toBeInTheDocument()
  })

  it('opens AuthModal above JoinInviteModal (z-[70] over z-50) when a guest clicks login from /join/:token', async () => {
    vi.spyOn(authApi, 'getCurrentUser').mockRejectedValue(new Error('Unauthorized'))
    vi.spyOn(teamsApi, 'previewTeamInvite').mockResolvedValue({
      token: 'inv_demo',
      teamId: 'team-1',
      teamName: 'Kyiv Mohyla Five',
      discipline: 'CS2',
      memberCount: 3,
      maxMembers: 5,
      isFull: false
    })
    window.history.replaceState({}, '', '/join/inv_demo')

    render(<App />)

    const loginButton = await screen.findByRole('button', { name: /Увійти для підтвердження вступу/i })
    loginButton.click()

    const authHeading = await screen.findByRole('heading', { name: /Вхід до Tournado/i })
    const inviteHeading = screen.getByRole('heading', { name: /Запрошення до команди/i })
    const authOverlay = authHeading.closest('.fixed')
    const inviteOverlay = inviteHeading.closest('.fixed')

    expect(authOverlay).toHaveClass('z-[70]')
    expect(inviteOverlay).toHaveClass('z-50')
    expect(
      inviteOverlay!.compareDocumentPosition(authOverlay!) & Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })
})

import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as disputesApi from '../api/disputes'
import * as registrationsApi from '../api/registrations'
import * as teamsApi from '../api/teams'
import { AuthContext, type AuthContextValue } from '../context/AuthContext'
import type { UserProfile } from '../types/auth'
import type { TournamentRegistrations } from '../types/registrations'
import type { TeamSummary } from '../types/teams'
import type { Tournament } from '../types/tournament'
import { TournamentDrawer } from './TournamentDrawer'

describe('TournamentDrawer component', () => {
  const sampleTournament: Tournament = {
    id: '3a013827-3de6-4157-8e4d-b9f83a4d6b71',
    publicSlug: 'kma-cyber-clash-2026',
    name: 'KMA Cyber Clash 2026',
    description: 'Тестовий турнір НаУКМА',
    discipline: 'CS2',
    teamSize: 5,
    format: 'SINGLE_ELIMINATION',
    slotLimit: 16,
    status: 'REGISTRATION_OPEN',
    registrationDeadlineAt: '2027-11-20T16:00:00Z',
    checkInOpensAt: '2027-11-20T17:00:00Z',
    startTimeAt: '2027-11-20T18:00:00Z',
    registeredTeamsCount: 1,
    createdAt: '2026-11-10T12:00:00Z'
  }

  const captainUser: UserProfile = {
    id: 'captain-1',
    email: 'captain@ukma.edu.ua',
    steamId: '76561198000000001',
    displayName: 'CaptainOne',
    supercellPlayerTag: null,
    chessUsername: null,
    avatarUrl: null
  }

  const organizerUser: UserProfile = {
    id: 'organizer-1',
    email: 'org@ukma.edu.ua',
    steamId: null,
    displayName: 'Organizer',
    supercellPlayerTag: null,
    chessUsername: null,
    avatarUrl: null
  }

  const baseRegistrations: TournamentRegistrations = {
    tournamentId: sampleTournament.id,
    organizerId: 'organizer-1',
    slotLimit: 16,
    registeredCount: 1,
    rostersLocked: false,
    teams: [
      {
        registrationId: 'reg-1',
        teamId: 'team-registered',
        teamName: 'Existing Roster',
        discipline: 'CS2',
        status: 'REGISTERED',
        memberCount: 5,
        registeredAt: '2026-11-15T12:00:00Z',
        rosterLocked: false
      }
    ]
  }

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('does not render raw tournament UUID ID in the drawer header', () => {
    render(<TournamentDrawer tournament={sampleTournament} isOpen={true} onClose={vi.fn()} />)

    expect(screen.getByText('KMA Cyber Clash 2026')).toBeInTheDocument()
    expect(screen.queryByText(/ID:\s*3a013827-3de6-4157-8e4d-b9f83a4d6b71/i)).toBeNull()
  })

  it('shows login prompt in participants tab when unauthenticated', async () => {
    const openAuthModalMock = vi.fn()
    vi.spyOn(registrationsApi, 'getTournamentRegistrations').mockResolvedValueOnce(baseRegistrations)

    const authValue: AuthContextValue = {
      user: null,
      isLoading: false,
      isAuthModalOpen: false,
      authModalPrompt: null,
      authErrorNotice: null,
      openAuthModal: openAuthModalMock,
      closeAuthModal: vi.fn(),
      refreshUser: vi.fn().mockResolvedValue(null),
      logout: vi.fn(),
      clearAuthErrorNotice: vi.fn()
    }

    render(
      <AuthContext.Provider value={authValue}>
        <TournamentDrawer tournament={sampleTournament} isOpen={true} onClose={vi.fn()} />
      </AuthContext.Provider>
    )

    fireEvent.click(screen.getByRole('button', { name: 'Учасники та заявка' }))

    await waitFor(() => {
      expect(screen.getByText('Existing Roster')).toBeInTheDocument()
      expect(
        screen.getByRole('button', { name: /Увійдіть, щоб зареєструвати команду/i })
      ).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Увійдіть, щоб зареєструвати команду/i }))
    expect(openAuthModalMock).toHaveBeenCalledTimes(1)
  })

  it('US-12 sc.1: a captain registers an eligible team and the registrations list updates', async () => {
    const captainTeam: TeamSummary = {
      id: 'team-captain-cs2',
      name: 'KMA Legends',
      discipline: 'CS2',
      captainId: 'captain-1',
      memberCount: 5,
      maxMembers: 5,
      isFull: true
    }

    const updatedRegistrations: TournamentRegistrations = {
      ...baseRegistrations,
      registeredCount: 2,
      teams: [
        ...baseRegistrations.teams,
        {
          registrationId: 'reg-2',
          teamId: 'team-captain-cs2',
          teamName: 'KMA Legends',
          discipline: 'CS2',
          status: 'REGISTERED',
          memberCount: 5,
          registeredAt: '2026-11-16T12:00:00Z',
          rosterLocked: false
        }
      ]
    }

    vi.spyOn(registrationsApi, 'getTournamentRegistrations')
      .mockResolvedValueOnce(baseRegistrations) // organizer probe when the drawer opens
      .mockResolvedValueOnce(baseRegistrations)
      .mockResolvedValueOnce(updatedRegistrations)
    vi.spyOn(teamsApi, 'getMyTeams').mockResolvedValueOnce([captainTeam])
    vi.spyOn(registrationsApi, 'registerTeamForTournament').mockResolvedValueOnce({
      registrationId: 'reg-2',
      tournamentId: sampleTournament.id,
      teamId: 'team-captain-cs2',
      status: 'REGISTERED',
      registeredAt: '2026-11-16T12:00:00Z'
    })

    const onChanged = vi.fn()
    const authValue: AuthContextValue = {
      user: captainUser,
      isLoading: false,
      isAuthModalOpen: false,
      authModalPrompt: null,
      authErrorNotice: null,
      openAuthModal: vi.fn(),
      closeAuthModal: vi.fn(),
      refreshUser: vi.fn().mockResolvedValue(captainUser),
      logout: vi.fn(),
      clearAuthErrorNotice: vi.fn()
    }

    render(
      <AuthContext.Provider value={authValue}>
        <TournamentDrawer
          tournament={sampleTournament}
          isOpen={true}
          onClose={vi.fn()}
          onRegistrationsChanged={onChanged}
        />
      </AuthContext.Provider>
    )

    fireEvent.click(screen.getByRole('button', { name: 'Учасники та заявка' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Зареєструвати команду' })).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: 'Зареєструвати команду' }))

    await waitFor(() => {
      expect(registrationsApi.registerTeamForTournament).toHaveBeenCalledWith(
        sampleTournament.id,
        'team-captain-cs2'
      )
      expect(onChanged).toHaveBeenCalledTimes(1)
      expect(screen.getByText('KMA Legends')).toBeInTheDocument()
      expect(screen.getByText('Команду успішно зареєстровано на турнір.')).toBeInTheDocument()
    })
  })

  it('US-13 sc.1 / US-14: the organizer removes a team after confirmation and locks rosters', async () => {
    const afterRemoveRegistrations: TournamentRegistrations = {
      ...baseRegistrations,
      registeredCount: 0,
      teams: []
    }

    const lockedRegistrations: TournamentRegistrations = {
      ...baseRegistrations,
      rostersLocked: true,
      teams: [{ ...baseRegistrations.teams[0], rosterLocked: true }]
    }

    vi.spyOn(registrationsApi, 'getTournamentRegistrations')
      .mockResolvedValueOnce(baseRegistrations) // organizer probe when the drawer opens
      .mockResolvedValueOnce(baseRegistrations)
      .mockResolvedValueOnce(lockedRegistrations)
      .mockResolvedValueOnce(afterRemoveRegistrations)
    vi.spyOn(teamsApi, 'getMyTeams').mockResolvedValueOnce([])
    vi.spyOn(disputesApi, 'getTournamentDisputes').mockResolvedValue({
      tournamentId: baseRegistrations.tournamentId,
      openDisputesCount: 0,
      disputes: []
    })
    vi.spyOn(registrationsApi, 'lockTournamentRosters').mockResolvedValueOnce({
      status: 'LOCKED',
      totalSnapshottedPlayers: 5
    })
    vi.spyOn(registrationsApi, 'removeTeamRegistration').mockResolvedValueOnce(undefined)

    const onChanged = vi.fn()
    const authValue: AuthContextValue = {
      user: organizerUser,
      isLoading: false,
      isAuthModalOpen: false,
      authModalPrompt: null,
      authErrorNotice: null,
      openAuthModal: vi.fn(),
      closeAuthModal: vi.fn(),
      refreshUser: vi.fn().mockResolvedValue(organizerUser),
      logout: vi.fn(),
      clearAuthErrorNotice: vi.fn()
    }

    render(
      <AuthContext.Provider value={authValue}>
        <TournamentDrawer
          tournament={sampleTournament}
          isOpen={true}
          onClose={vi.fn()}
          onRegistrationsChanged={onChanged}
        />
      </AuthContext.Provider>
    )

    fireEvent.click(screen.getByRole('button', { name: 'Учасники та заявка' }))

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /Зафіксувати склади \(Roster Lock\)/i })
      ).toBeInTheDocument()
    })

    // Lock rosters
    fireEvent.click(screen.getByRole('button', { name: /Зафіксувати склади \(Roster Lock\)/i }))

    await waitFor(() => {
      expect(registrationsApi.lockTournamentRosters).toHaveBeenCalledWith(sampleTournament.id)
      expect(screen.getByText(/Заморожено гравців: 5/i)).toBeInTheDocument()
    })

    // Two-step team removal
    fireEvent.click(screen.getByRole('button', { name: 'Видалити' }))
    expect(screen.getByRole('button', { name: 'Підтвердити видалення' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити видалення' }))

    await waitFor(() => {
      expect(registrationsApi.removeTeamRegistration).toHaveBeenCalledWith(
        sampleTournament.id,
        'team-registered'
      )
      expect(onChanged).toHaveBeenCalledTimes(2)
    })
  })

  const renderAs = (user: UserProfile | null, tournament: Tournament = sampleTournament) => {
    const authValue: AuthContextValue = {
      user,
      isLoading: false,
      isAuthModalOpen: false,
      authModalPrompt: null,
      authErrorNotice: null,
      openAuthModal: vi.fn(),
      closeAuthModal: vi.fn(),
      refreshUser: vi.fn().mockResolvedValue(user),
      logout: vi.fn(),
      clearAuthErrorNotice: vi.fn()
    }
    return render(
      <AuthContext.Provider value={authValue}>
        <TournamentDrawer tournament={tournament} isOpen={true} onClose={vi.fn()} />
      </AuthContext.Provider>
    )
  }

  it('US-31 sc.1: the organizer sees the «Спори» tab with the open-disputes counter before opening it', async () => {
    vi.spyOn(registrationsApi, 'getTournamentRegistrations').mockResolvedValue(baseRegistrations)
    vi.spyOn(disputesApi, 'getTournamentDisputes').mockResolvedValue({
      tournamentId: sampleTournament.id,
      openDisputesCount: 2,
      disputes: []
    })

    renderAs(organizerUser)

    expect(await screen.findByLabelText('Відкритих спорів: 2')).toBeInTheDocument()
    expect(disputesApi.getTournamentDisputes).toHaveBeenCalledWith(sampleTournament.id)
  })

  it('US-31 checklist 5: a captain who is not the organizer never sees disputes and they are not requested', async () => {
    vi.spyOn(registrationsApi, 'getTournamentRegistrations').mockResolvedValue(baseRegistrations)
    const listDisputes = vi.spyOn(disputesApi, 'getTournamentDisputes')

    renderAs(captainUser)

    await waitFor(() => expect(registrationsApi.getTournamentRegistrations).toHaveBeenCalled())
    expect(screen.queryByRole('button', { name: /Спори/ })).not.toBeInTheDocument()
    expect(listDisputes).not.toHaveBeenCalled()
  })

  // Defence in depth: the backend no longer sends a chess pool (core TournamentMapPoolIntegrationTest US-08 sc.2),
  // and the UI must not show one even for legacy data created before that fix.
  it('US-08 checklist 4: a Chess.com tournament shows no map pool, even for legacy data with a pool', () => {
    const chessTournament: Tournament = {
      ...sampleTournament,
      discipline: 'CHESS',
      teamSize: 1,
      mapPool: ['Blitz 3+2', 'Rapid 10+0']
    }

    renderAs(null, chessTournament)
    fireEvent.click(screen.getByRole('button', { name: 'Пул карт та правила' }))

    expect(screen.queryByText('Blitz 3+2')).not.toBeInTheDocument()
    expect(screen.getByText(/класичний мап-пул відсутній/)).toBeInTheDocument()
  })
})

import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as matchesApi from '../api/matches'
import * as teamsApi from '../api/teams'
import { AuthContext, type AuthContextValue } from '../context/AuthContext'
import type { MatchRoom } from '../types/matches'
import type { TeamDetails } from '../types/teams'
import type { UserProfile } from '../types/auth'
import { MatchRoomModal } from './MatchRoomModal'

describe('MatchRoomModal component', () => {
  const captain1User: UserProfile = {
    id: 'captain-1',
    email: 'captain1@ukma.edu.ua',
    steamId: '76561198012345678',
    displayName: 'CyberHawk',
    supercellPlayerTag: null,
    chessUsername: null,
    avatarUrl: null
  }

  const team1Details: TeamDetails = {
    id: 'team-1',
    name: 'Navi Junior KMA',
    discipline: 'CS2',
    captainId: 'captain-1',
    memberCount: 1,
    maxMembers: 5,
    isFull: false,
    members: [{ userId: 'captain-1', displayName: 'CyberHawk', isCaptain: true, gameIdentifier: '76561198012345678' }]
  }

  const team2Details: TeamDetails = {
    id: 'team-2',
    name: 'KPI Titans',
    discipline: 'CS2',
    captainId: 'captain-2',
    memberCount: 1,
    maxMembers: 5,
    isFull: false,
    members: [{ userId: 'captain-2', displayName: 'TitanCaptain', isCaptain: true, gameIdentifier: '76561198099887766' }]
  }

  const pickBanRoom: MatchRoom = {
    matchId: 'match-1',
    status: 'PICK_BAN',
    discipline: 'CS2',
    readyDeadlineAt: null,
    banDeadlineAt: '2026-11-22T12:05:00Z',
    team1: { id: 'team-1', name: 'Navi Junior KMA', isReady: true },
    team2: { id: 'team-2', name: 'KPI Titans', isReady: true },
    currentBanTurnTeamId: 'team-1',
    bannedMaps: [],
    remainingMaps: ['Mirage', 'Inferno', 'Nuke'],
    decidedMap: null,
    credentials: null
  }

  const pendingRoom: MatchRoom = {
    ...pickBanRoom,
    status: 'PENDING',
    team1: { id: 'team-1', name: 'Navi Junior KMA', isReady: false },
    team2: { id: 'team-2', name: 'KPI Titans', isReady: false },
    currentBanTurnTeamId: null
  }

  const inProgressRoom: MatchRoom = {
    ...pickBanRoom,
    status: 'IN_PROGRESS',
    currentBanTurnTeamId: null,
    decidedMap: 'Dust II',
    credentials: {
      discipline: 'CS2',
      cs2ConnectString: 'connect 194.44.12.5:27015; password kma_cup',
      brawlStarsCode: null,
      chessGameUrl: null,
      decidedMap: 'Dust II'
    }
  }

  const renderModal = (matchId: string | null, user: UserProfile | null = captain1User) => {
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
        <MatchRoomModal matchId={matchId} onClose={vi.fn()} />
      </AuthContext.Provider>
    )
  }

  beforeEach(() => {
    vi.restoreAllMocks()
    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } })
  })

  it('shows rosters and a ready button for the captain, then marks the team ready', async () => {
    vi.spyOn(matchesApi, 'getMatchRoom').mockResolvedValue(pendingRoom)
    vi.spyOn(teamsApi, 'getTeamById').mockImplementation((teamId: string) =>
      Promise.resolve(teamId === 'team-1' ? team1Details : team2Details)
    )
    vi.spyOn(matchesApi, 'markMatchReady').mockResolvedValue({ isReady: true })

    renderModal('match-1')

    await waitFor(() => {
      expect(screen.getAllByText('Navi Junior KMA').length).toBeGreaterThanOrEqual(1)
      expect(screen.getByText('CyberHawk')).toBeInTheDocument()
      expect(screen.getByText('TitanCaptain')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /я готовий/i }))

    await waitFor(() => {
      expect(matchesApi.markMatchReady).toHaveBeenCalledWith('match-1')
    })
  })

  it('lets the captain on turn ban a map during Map Veto', async () => {
    vi.spyOn(matchesApi, 'getMatchRoom').mockResolvedValue(pickBanRoom)
    vi.spyOn(teamsApi, 'getTeamById').mockImplementation((teamId: string) =>
      Promise.resolve(teamId === 'team-1' ? team1Details : team2Details)
    )
    vi.spyOn(matchesApi, 'banMatchMap').mockResolvedValue({ ...pickBanRoom, currentBanTurnTeamId: 'team-2' })

    renderModal('match-1')

    await waitFor(() => {
      expect(screen.getByText('Черга банити: Navi Junior KMA')).toBeInTheDocument()
    })

    fireEvent.click(screen.getAllByRole('button', { name: /викреслити/i })[0])

    await waitFor(() => {
      expect(matchesApi.banMatchMap).toHaveBeenCalledWith('match-1', 'Mirage')
    })
  })

  it('shows the CS2 connect string with a copy action once the match is in progress', async () => {
    vi.spyOn(matchesApi, 'getMatchRoom').mockResolvedValue(inProgressRoom)
    vi.spyOn(teamsApi, 'getTeamById').mockImplementation((teamId: string) =>
      Promise.resolve(teamId === 'team-1' ? team1Details : team2Details)
    )

    renderModal('match-1')

    await waitFor(() => {
      expect(screen.getByText('connect 194.44.12.5:27015; password kma_cup')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /копіювати connect ip/i }))

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith('connect 194.44.12.5:27015; password kma_cup')
    })
  })

  it('hides action buttons for an outsider who is not a captain of either team', async () => {
    const outsider: UserProfile = { ...captain1User, id: 'outsider-user' }
    vi.spyOn(matchesApi, 'getMatchRoom').mockResolvedValue(pendingRoom)
    vi.spyOn(teamsApi, 'getTeamById').mockImplementation((teamId: string) =>
      Promise.resolve(teamId === 'team-1' ? team1Details : team2Details)
    )

    renderModal('match-1', outsider)

    await waitFor(() => {
      expect(screen.getByText('Лише капітан може підтвердити готовність')).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: /я готовий/i })).not.toBeInTheDocument()
  })
})

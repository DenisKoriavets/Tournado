import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as teamsApi from '../api/teams'
import { AuthContext, type AuthContextValue } from '../context/AuthContext'
import type { UserProfile } from '../types/auth'
import type { TeamDetails, TeamSummary } from '../types/teams'
import { TeamsDrawer } from './TeamsDrawer'

describe('TeamsDrawer component', () => {
  const captainUser: UserProfile = {
    id: 'user-captain',
    email: 'captain@ukma.edu.ua',
    steamId: '76561198000000001',
    displayName: 'CaptainOne',
    supercellPlayerTag: null,
    chessUsername: null,
    avatarUrl: null
  }

  const incompleteTeamSummary: TeamSummary = {
    id: 'team-1',
    name: 'NaUKMA Five',
    discipline: 'CS2',
    captainId: 'user-captain',
    memberCount: 2,
    maxMembers: 5,
    isFull: false
  }

  const incompleteTeamDetails: TeamDetails = {
    ...incompleteTeamSummary,
    members: [
      {
        userId: 'user-captain',
        displayName: 'CaptainOne',
        isCaptain: true,
        gameIdentifier: '76561198000000001'
      },
      {
        userId: 'user-2',
        displayName: 'RiflerTwo',
        isCaptain: false,
        gameIdentifier: null
      }
    ]
  }

  const renderDrawer = (user: UserProfile | null = captainUser) => {
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
        <TeamsDrawer isOpen={true} onClose={vi.fn()} />
      </AuthContext.Provider>
    )
  }

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('displays team roster fullness N із M and generates invite link for captain', async () => {
    vi.spyOn(teamsApi, 'getMyTeams').mockResolvedValueOnce([incompleteTeamSummary])
    vi.spyOn(teamsApi, 'getTeamById').mockResolvedValueOnce(incompleteTeamDetails)
    vi.spyOn(teamsApi, 'createTeamInvite').mockResolvedValueOnce({
      token: 'invite-abc-123',
      inviteUrl: '/join/invite-abc-123',
      expiresAt: '2026-11-23T12:00:00Z'
    })

    renderDrawer()

    await waitFor(() => {
      expect(screen.getAllByText('NaUKMA Five').length).toBeGreaterThanOrEqual(1)
      expect(screen.getByText('2 із 5')).toBeInTheDocument()
      expect(screen.getByText('Не вистачає 3 гравців')).toBeInTheDocument()
      expect(screen.getByText('Ідентифікатор відсутній')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Згенерувати інвайт-посилання/i }))

    await waitFor(() => {
      expect(teamsApi.createTeamInvite).toHaveBeenCalledWith('team-1')
      const inviteInput = screen.getByLabelText('Інвайт-посилання команди') as HTMLInputElement
      expect(inviteInput.value).toContain('/join/invite-abc-123')
    })
  })

  it('creates a new team and displays full roster indicator for solo Chess team', async () => {
    const newChessSummary: TeamSummary = {
      id: 'team-chess',
      name: 'Grandmaster Solo',
      discipline: 'CHESS',
      captainId: 'user-captain',
      memberCount: 1,
      maxMembers: 1,
      isFull: true
    }

    const newChessDetails: TeamDetails = {
      ...newChessSummary,
      members: [
        {
          userId: 'user-captain',
          displayName: 'CaptainOne',
          isCaptain: true,
          gameIdentifier: 'gm_kma'
        }
      ]
    }

    vi.spyOn(teamsApi, 'getMyTeams').mockResolvedValueOnce([])
    vi.spyOn(teamsApi, 'createTeam').mockResolvedValueOnce(newChessSummary)
    vi.spyOn(teamsApi, 'getTeamById').mockResolvedValueOnce(newChessDetails)

    renderDrawer()

    await waitFor(() => {
      expect(screen.getByText('У вас ще немає жодної команди')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Створити команду/i }))

    fireEvent.change(screen.getByLabelText(/Назва команди/i), {
      target: { value: 'Grandmaster Solo' }
    })
    fireEvent.click(screen.getByRole('button', { name: /Chess\.com/i }))

    const submitButtons = screen.getAllByRole('button', { name: 'Створити команду' })
    fireEvent.click(submitButtons[submitButtons.length - 1])

    await waitFor(() => {
      expect(teamsApi.createTeam).toHaveBeenCalledWith({
        name: 'Grandmaster Solo',
        discipline: 'CHESS'
      })
      expect(screen.getByText('Склад повний')).toBeInTheDocument()
      expect(screen.getByText('1 із 1')).toBeInTheDocument()
    })
  })
})

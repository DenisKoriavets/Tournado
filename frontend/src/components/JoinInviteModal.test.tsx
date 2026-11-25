import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as teamsApi from '../api/teams'
import { AuthContext, type AuthContextValue } from '../context/AuthContext'
import type { UserProfile } from '../types/auth'
import type { TeamDetails, TeamInvitePreview } from '../types/teams'
import { JoinInviteModal } from './JoinInviteModal'

describe('JoinInviteModal component', () => {
  const preview: TeamInvitePreview = {
    token: 'tok-999',
    teamId: 'team-1',
    teamName: 'Kyiv Mohyla Five',
    discipline: 'CS2',
    memberCount: 3,
    maxMembers: 5,
    isFull: false
  }

  const memberUser: UserProfile = {
    id: 'user-3',
    email: 'member@ukma.edu.ua',
    steamId: '76561198000000009',
    displayName: 'NewPlayer',
    supercellPlayerTag: null,
    chessUsername: null,
    avatarUrl: null
  }

  const openAuthModalMock = vi.fn()
  const onJoinedMock = vi.fn()
  const onCloseMock = vi.fn()

  const renderModal = (user: UserProfile | null) => {
    const authValue: AuthContextValue = {
      user,
      isLoading: false,
      isAuthModalOpen: false,
      authModalPrompt: null,
      authErrorNotice: null,
      openAuthModal: openAuthModalMock,
      closeAuthModal: vi.fn(),
      refreshUser: vi.fn().mockResolvedValue(user),
      logout: vi.fn(),
      clearAuthErrorNotice: vi.fn()
    }

    return render(
      <AuthContext.Provider value={authValue}>
        <JoinInviteModal token="tok-999" onClose={onCloseMock} onJoined={onJoinedMock} />
      </AuthContext.Provider>
    )
  }

  beforeEach(() => {
    vi.restoreAllMocks()
    openAuthModalMock.mockClear()
    onJoinedMock.mockClear()
    onCloseMock.mockClear()
  })

  it('prompts guest user to log in before confirming invite join', async () => {
    vi.spyOn(teamsApi, 'previewTeamInvite').mockResolvedValueOnce(preview)

    renderModal(null)

    await waitFor(() => {
      expect(screen.getAllByText('Kyiv Mohyla Five').length).toBeGreaterThanOrEqual(1)
      expect(screen.getByText('3 / 5')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Увійти для підтвердження вступу/i }))
    expect(openAuthModalMock).toHaveBeenCalledTimes(1)
  })

  it('joins team when authenticated user confirms', async () => {
    const joinedDetails: TeamDetails = {
      id: 'team-1',
      name: 'Kyiv Mohyla Five',
      discipline: 'CS2',
      captainId: 'user-1',
      memberCount: 4,
      maxMembers: 5,
      isFull: false,
      members: []
    }

    vi.spyOn(teamsApi, 'previewTeamInvite').mockResolvedValueOnce(preview)
    vi.spyOn(teamsApi, 'joinTeamByInvite').mockResolvedValueOnce(joinedDetails)

    renderModal(memberUser)

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /Підтвердити вступ до команди/i })
      ).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Підтвердити вступ до команди/i }))

    await waitFor(() => {
      expect(teamsApi.joinTeamByInvite).toHaveBeenCalledWith('tok-999')
      expect(onJoinedMock).toHaveBeenCalledWith(joinedDetails)
      expect(onCloseMock).toHaveBeenCalledTimes(1)
    })
  })
})

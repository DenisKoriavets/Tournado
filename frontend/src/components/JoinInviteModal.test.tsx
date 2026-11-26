import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/http'
import * as teamsApi from '../api/teams'
import { readPendingInvite } from '../utils/pendingInvite'
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

  it('US-05 checklist 1-2: the invite shows the team and asks a guest to log in before joining', async () => {
    vi.spyOn(teamsApi, 'previewTeamInvite').mockResolvedValueOnce(preview)

    renderModal(null)

    await waitFor(() => {
      expect(screen.getAllByText('Kyiv Mohyla Five').length).toBeGreaterThanOrEqual(1)
      expect(screen.getByText('3 / 5')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Увійти для підтвердження вступу/i }))
    expect(openAuthModalMock).toHaveBeenCalledTimes(1)
  })

  it('US-05 sc.1: an authenticated player confirms and joins the team', async () => {
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

  it("US-05 checklist 2: the guest's invite is remembered before login so it reopens after the redirect", async () => {
    window.localStorage.clear()
    vi.spyOn(teamsApi, 'previewTeamInvite').mockResolvedValueOnce(preview)

    renderModal(null)
    fireEvent.click(await screen.findByRole('button', { name: /Увійти для підтвердження вступу/i }))

    expect(readPendingInvite()).toBe('tok-999')
    window.localStorage.clear()
  })

  it.each([
    ['US-05 sc.2', 'ROSTER_FULL', 409, 'У команді немає вільних місць'],
    ['US-05 sc.3', 'PLAYER_ALREADY_IN_TEAM', 409, 'Ви вже є в ростері цієї команди'],
    ['US-05 sc.5', 'ROSTER_LOCKED', 409, 'Склад команди заблоковано']
  ])('%s: joining fails with %s and the player sees a clear explanation', async (_id, code, status, message) => {
    vi.spyOn(teamsApi, 'previewTeamInvite').mockResolvedValueOnce(preview)
    vi.spyOn(teamsApi, 'joinTeamByInvite').mockRejectedValueOnce(
      new ApiError({ title: 'Conflict', status, detail: 'English backend detail', code })
    )

    renderModal(memberUser)
    fireEvent.click(await screen.findByRole('button', { name: /Підтвердити вступ до команди/i }))

    expect(await screen.findByText(new RegExp(message))).toBeInTheDocument()
    expect(onJoinedMock).not.toHaveBeenCalled()
  })

  it('US-05 sc.2: opening the link of a full roster already says there are no free places and blocks joining', async () => {
    vi.spyOn(teamsApi, 'previewTeamInvite').mockResolvedValueOnce({ ...preview, memberCount: 5, isFull: true })
    const join = vi.spyOn(teamsApi, 'joinTeamByInvite')

    renderModal(memberUser)

    expect(await screen.findByText(/заповнено всі слоти/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Підтвердити вступ до команди/i })).toBeDisabled()
    expect(join).not.toHaveBeenCalled()
  })

  it('US-05 sc.4: an invalid or expired invite shows "invalid invitation" and no join button', async () => {
    vi.spyOn(teamsApi, 'previewTeamInvite').mockRejectedValueOnce(
      new ApiError({ title: 'Bad Request', status: 400, detail: 'Invite expired', code: 'INVITE_LINK_EXPIRED' })
    )

    renderModal(memberUser)

    expect(await screen.findByText(/Запрошення недійсне/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Підтвердити вступ до команди/i })).not.toBeInTheDocument()
  })
})

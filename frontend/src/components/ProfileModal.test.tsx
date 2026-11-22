import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'
import { ApiError } from '../api/http'
import { AuthContext, type AuthContextValue } from '../context/AuthContext'
import type { UserProfile } from '../types/auth'
import { ProfileModal } from './ProfileModal'

describe('ProfileModal component', () => {
  const sampleUser: UserProfile = {
    id: 'user-1',
    email: 'player@ukma.edu.ua',
    steamId: null,
    displayName: 'KMA_Captain',
    supercellPlayerTag: null,
    chessUsername: 'kma_chess',
    avatarUrl: null
  }

  const refreshUserMock = vi.fn().mockResolvedValue(sampleUser)

  const renderWithAuth = (user: UserProfile | null = sampleUser) => {
    const value: AuthContextValue = {
      user,
      isLoading: false,
      isAuthModalOpen: false,
      authModalPrompt: null,
      authErrorNotice: null,
      openAuthModal: vi.fn(),
      closeAuthModal: vi.fn(),
      refreshUser: refreshUserMock,
      logout: vi.fn(),
      clearAuthErrorNotice: vi.fn()
    }

    return render(
      <AuthContext.Provider value={value}>
        <ProfileModal isOpen={true} onClose={vi.fn()} />
      </AuthContext.Provider>
    )
  }

  beforeEach(() => {
    vi.restoreAllMocks()
    refreshUserMock.mockClear()
  })

  it('submits updated game identifiers and refreshes profile', async () => {
    const updatedProfile: UserProfile = {
      ...sampleUser,
      steamId: '76561198012345678',
      supercellPlayerTag: '#2PP98L9',
      chessUsername: 'gm_kma'
    }

    vi.spyOn(authApi, 'updateGameIdentifiers').mockResolvedValueOnce(updatedProfile)

    renderWithAuth()

    fireEvent.change(screen.getByLabelText(/Steam ID/i), {
      target: { value: '76561198012345678' }
    })
    fireEvent.change(screen.getByLabelText(/Supercell Player Tag/i), {
      target: { value: '#2PP98L9' }
    })
    fireEvent.change(screen.getByLabelText(/Chess\.com Username/i), {
      target: { value: 'gm_kma' }
    })

    fireEvent.click(screen.getByRole('button', { name: /Зберегти ідентифікатори/i }))

    await waitFor(() => {
      expect(authApi.updateGameIdentifiers).toHaveBeenCalledWith({
        steamId: '76561198012345678',
        supercellPlayerTag: '#2PP98L9',
        chessUsername: 'gm_kma'
      })
      expect(refreshUserMock).toHaveBeenCalledTimes(1)
      expect(screen.getByText('Ігрові ідентифікатори успішно збережено.')).toBeInTheDocument()
    })
  })

  it('renders 422 INVALID_GAME_IDENTIFIER invalidParams field errors inline', async () => {
    vi.spyOn(authApi, 'updateGameIdentifiers').mockRejectedValueOnce(
      new ApiError({
        title: 'Невірний формат ігрового ідентифікатора',
        status: 422,
        detail: 'Перевірте формат введених ідентифікаторів.',
        code: 'INVALID_GAME_IDENTIFIER',
        invalidParams: [
          {
            name: 'steamId',
            reason: 'Steam ID повинен містити 17 цифр і починатися з 7656119.'
          },
          {
            name: 'supercellPlayerTag',
            reason: 'Supercell Tag має починатися з #.'
          }
        ]
      })
    )

    renderWithAuth()

    fireEvent.change(screen.getByLabelText(/Steam ID/i), {
      target: { value: '12345' }
    })
    fireEvent.change(screen.getByLabelText(/Supercell Player Tag/i), {
      target: { value: 'INVALID' }
    })

    fireEvent.click(screen.getByRole('button', { name: /Зберегти ідентифікатори/i }))

    await waitFor(() => {
      expect(screen.getByText('INVALID_GAME_IDENTIFIER')).toBeInTheDocument()
      expect(
        screen.getByText('Steam ID повинен містити 17 цифр і починатися з 7656119.')
      ).toBeInTheDocument()
      expect(screen.getByText('Supercell Tag має починатися з #.')).toBeInTheDocument()
    })
  })
})

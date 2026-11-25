import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'
import { AuthModal } from './AuthModal'

describe('AuthModal component', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders Steam login and Email Magic Link options', () => {
    render(<AuthModal isOpen={true} onClose={vi.fn()} />)

    expect(screen.getByText('Вхід до Tournado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Увійти через Steam/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/Увійти через пошту \(Magic Link\)/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Отримати Magic Link/i })).toBeInTheDocument()
  })

  it('triggers Steam login handler when Steam button is clicked', () => {
    const onSteamLogin = vi.fn()
    render(<AuthModal isOpen={true} onClose={vi.fn()} onSteamLogin={onSteamLogin} />)

    fireEvent.click(screen.getByRole('button', { name: /Увійти через Steam/i }))
    expect(onSteamLogin).toHaveBeenCalledTimes(1)
  })

  it('requests magic link and shows Mailpit inbox instructions on success', async () => {
    vi.spyOn(authApi, 'requestEmailLoginLink').mockResolvedValueOnce({
      message: 'Sent'
    })

    render(<AuthModal isOpen={true} onClose={vi.fn()} />)

    fireEvent.change(screen.getByLabelText(/Увійти через пошту/i), {
      target: { value: 'captain@ukma.edu.ua' }
    })
    fireEvent.click(screen.getByRole('button', { name: /Отримати Magic Link/i }))

    await waitFor(() => {
      expect(authApi.requestEmailLoginLink).toHaveBeenCalledWith('captain@ukma.edu.ua')
      expect(screen.getByText('Посилання для входу надіслано')).toBeInTheDocument()
      expect(screen.getByText('captain@ukma.edu.ua')).toBeInTheDocument()
      expect(screen.getByText('http://localhost:8025')).toBeInTheDocument()
    })
  })

  it('displays Steam cancelled alert banner when errorNotice is provided', () => {
    render(
      <AuthModal
        isOpen={true}
        onClose={vi.fn()}
        errorNotice="Авторизацію через Steam було скасовано. Спробуйте ще раз."
      />
    )

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Авторизацію через Steam було скасовано. Спробуйте ще раз.'
    )
  })
})

import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { TournamentFormModal } from './TournamentFormModal'
import * as api from '../api/tournaments'
import { ApiError } from '../api/tournaments'
import type { Tournament } from '../types/tournament'

describe('TournamentFormModal component', () => {
  const onClose = vi.fn()
  const onSuccess = vi.fn()

  beforeEach(() => {
    vi.restoreAllMocks()
    onClose.mockClear()
    onSuccess.mockClear()
  })

  it('does not render when isOpen is false', () => {
    render(<TournamentFormModal isOpen={false} onClose={onClose} onSuccess={onSuccess} />)
    expect(screen.queryByText(/Створити новий турнір/i)).toBeNull()
  })

  it('renders discipline choices and automatic team sizes in Step 1', () => {
    render(<TournamentFormModal isOpen={true} onClose={onClose} onSuccess={onSuccess} />)

    expect(screen.getByText('CS2')).toBeInTheDocument()
    expect(screen.getAllByText('Brawl Stars').length).toBeGreaterThan(0)
    expect(screen.getByText('Chess.com')).toBeInTheDocument()

    // Select CS2
    fireEvent.click(screen.getByText('CS2'))
    expect(screen.getByText(/5v5 \(5 гравців у складі\)/i)).toBeInTheDocument()
    expect(screen.getByText('de_mirage')).toBeInTheDocument()

    // Select Chess.com
    fireEvent.click(screen.getByText('Chess.com'))
    expect(screen.getByText(/1v1 \(1 гравець у складі\)/i)).toBeInTheDocument()
    expect(screen.getByText(/Для Chess.com пул карт не застосовується/i)).toBeInTheDocument()
  })

  it('validates Step 1 required fields before allowing progression to Step 2', () => {
    render(<TournamentFormModal isOpen={true} onClose={onClose} onSuccess={onSuccess} />)

    const nextBtn = screen.getByText(/Далі: Слоти та розклад/i)
    fireEvent.click(nextBtn)

    expect(screen.getByText('Оберіть ігрову дисципліну.')).toBeInTheDocument()
    expect(screen.getByText(/Вкажіть назву турніру/i)).toBeInTheDocument()
  })

  it('navigates to Step 2 and allows slot limit and schedule configuration', () => {
    render(<TournamentFormModal isOpen={true} onClose={onClose} onSuccess={onSuccess} />)

    // Fill Step 1
    fireEvent.click(screen.getByText('CS2'))
    fireEvent.change(screen.getByPlaceholderText(/Наприклад: KMA CS2 Autumn Cup 2026/i), {
      target: { value: 'KMA CS2 Cup' }
    })

    // Advance to Step 2
    fireEvent.click(screen.getByText(/Далі: Слоти та розклад/i))

    // Step 2 items visible
    expect(screen.getByText('8 команд')).toBeInTheDocument()
    expect(screen.getByText('16 команд')).toBeInTheDocument()
    expect(screen.getByText('32 команд')).toBeInTheDocument()
    expect(screen.getByLabelText(/Рядок підключення до CS2 сервера/i)).toBeInTheDocument()

    fireEvent.click(screen.getByText('16 команд'))
  })

  it('submits form successfully when valid', async () => {
    const mockCreated: Tournament = {
      id: 'new-id-123',
      publicSlug: 'new-id-123',
      name: 'Autumn Cup 2026',
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

    vi.spyOn(api, 'createTournament').mockResolvedValueOnce(mockCreated)

    render(<TournamentFormModal isOpen={true} onClose={onClose} onSuccess={onSuccess} />)

    // Step 1
    fireEvent.click(screen.getByText('CS2'))
    fireEvent.change(screen.getByPlaceholderText(/Наприклад: KMA CS2 Autumn Cup 2026/i), {
      target: { value: 'Autumn Cup 2026' }
    })
    fireEvent.click(screen.getByText(/Далі: Слоти та розклад/i))

    // Step 2
    fireEvent.click(screen.getByText('16 команд'))
    fireEvent.click(screen.getByText(/Заповнити рекомендованим розкладом/i))

    // Submit
    fireEvent.click(screen.getByText(/Створити турнір/i))

    await waitFor(() => {
      expect(api.createTournament).toHaveBeenCalledTimes(1)
      expect(onSuccess).toHaveBeenCalledWith(mockCreated)
      expect(onClose).toHaveBeenCalled()
    })
  })

  it('renders RFC 7807 problem details error banner and field errors', async () => {
    const problem = {
      title: 'Невірний ліміт слотів',
      status: 422,
      detail: 'Ліміт слотів повинен бути 8, 16 або 32.',
      code: 'INVALID_SLOT_LIMIT',
      invalidParams: [
        { name: 'slotLimit', reason: 'Слоти можуть бути лише 8, 16 або 32.' }
      ]
    }

    vi.spyOn(api, 'createTournament').mockRejectedValueOnce(new ApiError(problem))

    render(<TournamentFormModal isOpen={true} onClose={onClose} onSuccess={onSuccess} />)

    fireEvent.click(screen.getByText('CS2'))
    fireEvent.change(screen.getByPlaceholderText(/Наприклад: KMA CS2 Autumn Cup 2026/i), {
      target: { value: 'Autumn Cup 2026' }
    })
    fireEvent.click(screen.getByText(/Далі: Слоти та розклад/i))

    fireEvent.click(screen.getByText('16 команд'))
    fireEvent.click(screen.getByText(/Заповнити рекомендованим розкладом/i))

    fireEvent.click(screen.getByText(/Створити турнір/i))

    await waitFor(() => {
      expect(screen.getByText('Невірний ліміт слотів')).toBeInTheDocument()
      expect(screen.getByText('INVALID_SLOT_LIMIT')).toBeInTheDocument()
      expect(screen.getByText(/Ліміт слотів повинен бути 8, 16 або 32/i)).toBeInTheDocument()
      const slotErrors = screen.getAllByText(/Слоти можуть бути лише 8, 16 або 32/i)
      expect(slotErrors.length).toBeGreaterThanOrEqual(1)
    })
  })
})

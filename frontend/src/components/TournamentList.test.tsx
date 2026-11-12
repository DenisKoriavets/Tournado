import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { TournamentList } from './TournamentList'
import type { Tournament } from '../types/tournament'

describe('TournamentList component', () => {
  const mockTournaments: Tournament[] = [
    {
      id: '1',
      publicSlug: 'cs2-tourn',
      name: 'CS2 Autumn Clash',
      description: 'Major tournament',
      discipline: 'CS2',
      teamSize: 5,
      format: 'SINGLE_ELIMINATION',
      slotLimit: 16,
      status: 'REGISTRATION_OPEN',
      registrationDeadlineAt: '2026-11-01T15:00:00Z',
      checkInOpensAt: '2026-11-01T16:00:00Z',
      startTimeAt: '2026-11-01T17:00:00Z',
      mapPool: ['de_mirage', 'de_inferno'],
      registeredTeamsCount: 4,
      createdAt: '2026-11-01T12:00:00Z'
    },
    {
      id: '2',
      publicSlug: 'brawl-tourn',
      name: 'Brawl Stars Championship',
      description: 'Mobile esports',
      discipline: 'BRAWL_STARS',
      teamSize: 3,
      format: 'SINGLE_ELIMINATION',
      slotLimit: 8,
      status: 'REGISTRATION_OPEN',
      registrationDeadlineAt: '2026-11-02T15:00:00Z',
      checkInOpensAt: '2026-11-02T16:00:00Z',
      startTimeAt: '2026-11-02T17:00:00Z',
      mapPool: ['Hard Rock Mine'],
      registeredTeamsCount: 2,
      createdAt: '2026-11-01T12:00:00Z'
    }
  ]

  const onSelect = vi.fn()
  const onCreateClick = vi.fn()
  const onRetry = vi.fn()

  it('renders list of tournament cards', () => {
    render(
      <TournamentList
        tournaments={mockTournaments}
        isLoading={false}
        error={null}
        onSelectTournament={onSelect}
        onCreateClick={onCreateClick}
        onRetry={onRetry}
      />
    )

    expect(screen.getByText('CS2 Autumn Clash')).toBeInTheDocument()
    expect(screen.getByText('Brawl Stars Championship')).toBeInTheDocument()
  })

  it('filters tournaments by discipline tab', () => {
    render(
      <TournamentList
        tournaments={mockTournaments}
        isLoading={false}
        error={null}
        onSelectTournament={onSelect}
        onCreateClick={onCreateClick}
        onRetry={onRetry}
      />
    )

    // Click CS2 filter tab
    const cs2Tab = screen.getAllByText('CS2')[0]
    fireEvent.click(cs2Tab)

    expect(screen.getByText('CS2 Autumn Clash')).toBeInTheDocument()
    expect(screen.queryByText('Brawl Stars Championship')).toBeNull()
  })

  it('filters tournaments by search query', () => {
    render(
      <TournamentList
        tournaments={mockTournaments}
        isLoading={false}
        error={null}
        onSelectTournament={onSelect}
        onCreateClick={onCreateClick}
        onRetry={onRetry}
      />
    )

    const searchInput = screen.getByPlaceholderText(/Пошук за назвою/i)
    fireEvent.change(searchInput, { target: { value: 'Brawl' } })

    expect(screen.queryByText('CS2 Autumn Clash')).toBeNull()
    expect(screen.getByText('Brawl Stars Championship')).toBeInTheDocument()
  })

  it('triggers onSelect when tournament card is clicked', () => {
    render(
      <TournamentList
        tournaments={mockTournaments}
        isLoading={false}
        error={null}
        onSelectTournament={onSelect}
        onCreateClick={onCreateClick}
        onRetry={onRetry}
      />
    )

    fireEvent.click(screen.getByText('CS2 Autumn Clash'))
    expect(onSelect).toHaveBeenCalledWith(mockTournaments[0])
  })
})

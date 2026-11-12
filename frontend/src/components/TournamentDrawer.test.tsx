import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { TournamentDrawer } from './TournamentDrawer'
import type { Tournament } from '../types/tournament'

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
    registrationDeadlineAt: '2026-11-20T16:00:00Z',
    checkInOpensAt: '2026-11-20T17:00:00Z',
    startTimeAt: '2026-11-20T18:00:00Z',
    registeredTeamsCount: 4,
    createdAt: '2026-11-10T12:00:00Z'
  }

  it('does not render raw tournament UUID ID in the drawer header', () => {
    render(
      <TournamentDrawer
        tournament={sampleTournament}
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    expect(screen.getByText('KMA Cyber Clash 2026')).toBeInTheDocument()
    expect(screen.queryByText(/ID:\s*3a013827-3de6-4157-8e4d-b9f83a4d6b71/i)).toBeNull()
  })
})

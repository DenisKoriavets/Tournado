import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { OrganizerDisputesState } from '../hooks/useOrganizerDisputes'
import type { DisputeSummary, TournamentDisputes } from '../types/disputes'
import { TournamentDisputesTab } from './TournamentDisputesTab'

// US-31 "Візуальна індикація суперечки на дашборді".
describe('TournamentDisputesTab (US-31)', () => {
  const openDispute: DisputeSummary = {
    disputeId: 'dispute-1',
    matchId: 'match-1',
    roundNumber: 1,
    matchOrder: 1,
    status: 'OPEN',
    team1: { id: 'team-a', name: 'QA Terror Squad' },
    team2: { id: 'team-b', name: 'QA Vegayta Five' },
    openedByTeamId: 'team-a',
    aiAnalysisStatus: 'SUCCESS',
    aiRecognizedScoreTeam1: 13,
    aiRecognizedScoreTeam2: 9,
    aiConfidence: 0.96,
    allRosterNicknamesFound: true,
    createdAt: '2026-11-25T12:00:00Z',
    resolvedAt: null
  }

  const stateWith = (data: TournamentDisputes): OrganizerDisputesState => ({
    data,
    error: null,
    forbidden: false,
    reload: vi.fn()
  })

  it('US-31 sc.1: an open dispute is shown with the distinct «Спір» status and the AI summary', () => {
    render(
      <TournamentDisputesTab
        state={stateWith({ tournamentId: 't', openDisputesCount: 1, disputes: [openDispute] })}
        onOpenDispute={vi.fn()}
      />
    )

    expect(screen.getByText('Спір')).toBeInTheDocument()
    expect(screen.getByText(/Відкритих спорів: 1/)).toBeInTheDocument()
    expect(screen.getByText('ШІ: 13:9 (96%)')).toBeInTheDocument()
  })

  it('US-31 sc.2: after the organizer closes it, the dispute is marked «Опрацьовано» and the counter disappears', () => {
    render(
      <TournamentDisputesTab
        state={stateWith({
          tournamentId: 't',
          openDisputesCount: 0,
          disputes: [{ ...openDispute, status: 'RESOLVED', resolvedAt: '2026-11-25T12:05:00Z' }]
        })}
        onOpenDispute={vi.fn()}
      />
    )

    expect(screen.getByText('Опрацьовано')).toBeInTheDocument()
    expect(screen.queryByText('Спір')).not.toBeInTheDocument()
    expect(screen.getByText('Відкритих спорів немає.')).toBeInTheDocument()
  })

  it('US-31 sc.3: two open disputes are both marked and the counter shows 2', () => {
    render(
      <TournamentDisputesTab
        state={stateWith({
          tournamentId: 't',
          openDisputesCount: 2,
          disputes: [openDispute, { ...openDispute, disputeId: 'dispute-2', matchId: 'match-2', matchOrder: 2 }]
        })}
        onOpenDispute={vi.fn()}
      />
    )

    expect(screen.getAllByText('Спір')).toHaveLength(2)
    expect(screen.getByText(/Відкритих спорів: 2/)).toBeInTheDocument()
  })

  it('US-31 checklist 4: clicking a disputed match opens its dispute card', () => {
    const onOpenDispute = vi.fn()
    render(
      <TournamentDisputesTab
        state={stateWith({ tournamentId: 't', openDisputesCount: 1, disputes: [openDispute] })}
        onOpenDispute={onOpenDispute}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: /Раунд 1 · Матч 1/ }))

    expect(onOpenDispute).toHaveBeenCalledWith('dispute-1')
  })

  it('US-33: the list hints to check nicknames only when the AI really found a mismatch', () => {
    render(
      <TournamentDisputesTab
        state={stateWith({
          tournamentId: 't',
          openDisputesCount: 2,
          disputes: [
            { ...openDispute, allRosterNicknamesFound: false },
            {
              ...openDispute,
              disputeId: 'dispute-2',
              aiAnalysisStatus: 'UNRECOGNIZED',
              aiRecognizedScoreTeam1: null,
              aiRecognizedScoreTeam2: null,
              aiConfidence: null,
              allRosterNicknamesFound: false
            }
          ]
        })}
        onOpenDispute={vi.fn()}
      />
    )

    expect(screen.getAllByText('Перевірте нікнейми')).toHaveLength(1)
    expect(screen.getByText('ШІ: рахунок не розпізнано')).toBeInTheDocument()
  })
})

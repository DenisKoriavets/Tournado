import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import * as disputesApi from '../api/disputes'
import { ApiError } from '../api/http'
import type { DisputeCard } from '../types/disputes'
import { DisputeCardModal, PENDING_POLL_MS } from './DisputeCardModal'

// US-32 (AI hint), US-33 (nickname check), US-34 (organizer closes the dispute).
describe('DisputeCardModal (US-32, US-33, US-34)', () => {
  const baseCard: DisputeCard = {
    disputeId: 'dispute-1',
    matchId: 'match-1',
    tournamentId: 'tournament-1',
    status: 'OPEN',
    screenshotUrl: '/api/v1/disputes/dispute-1/screenshot',
    roundNumber: 1,
    matchOrder: 1,
    team1: { id: 'team-a', name: 'QA Terror Squad' },
    team2: { id: 'team-b', name: 'QA Vegayta Five' },
    openedByTeamId: 'team-a',
    submittedScores: [
      { teamId: 'team-a', team1Score: 13, team2Score: 9, submittedAt: '2026-11-25T11:50:00Z' },
      { teamId: 'team-b', team1Score: 9, team2Score: 13, submittedAt: '2026-11-25T11:52:00Z' }
    ],
    aiAnalysisStatus: 'SUCCESS',
    aiAnalysis: {
      status: 'SUCCESS',
      recognizedScoreTeam1: 13,
      recognizedScoreTeam2: 9,
      confidence: 0.92,
      allRosterNicknamesFound: true,
      detectedNicknames: ['BOT Mayer'],
      nicknameChecks: [
        { nickname: 'BOT Mayer', sideTeamId: 'team-a', inRoster: true, matchedTeamId: 'team-a', matchedRosterName: 'BOT Mayer' }
      ],
      analyzedAt: '2026-11-25T12:00:02Z'
    },
    canApproveAiSuggestion: true,
    resolution: null,
    createdAt: '2026-11-25T12:00:00Z'
  }

  const unrecognizedCard: DisputeCard = {
    ...baseCard,
    aiAnalysisStatus: 'UNRECOGNIZED',
    aiAnalysis: {
      ...baseCard.aiAnalysis!,
      status: 'UNRECOGNIZED',
      recognizedScoreTeam1: null,
      recognizedScoreTeam2: null,
      confidence: null,
      allRosterNicknamesFound: false,
      detectedNicknames: [],
      nicknameChecks: []
    },
    canApproveAiSuggestion: false
  }

  const resolvedResult = (team1Score: number, team2Score: number, winnerTeamId: string) => ({
    disputeId: 'dispute-1',
    status: 'RESOLVED' as const,
    matchId: 'match-1',
    matchStatus: 'COMPLETED' as const,
    winnerTeamId,
    team1Score,
    team2Score,
    decision: 'AI_SUGGESTION' as const,
    resolvedAt: '2026-11-25T12:05:00Z'
  })

  const renderCard = () => {
    const onResolved = vi.fn()
    render(<DisputeCardModal disputeId="dispute-1" onClose={vi.fn()} onResolved={onResolved} />)
    return { onResolved }
  }

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('US-32 sc.1: shows the «Підказка ШІ» with the recognised score, winner and confidence in percent', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(baseCard)
    renderCard()

    const hint = (await screen.findByText('Підказка ШІ')).closest('div') as HTMLElement

    expect(within(hint).getByText('13:9')).toBeInTheDocument()
    expect(within(hint).getByText(/на користь «QA Terror Squad»/)).toBeInTheDocument()
    expect(within(hint).getByText(/впевненість 92%/)).toBeInTheDocument()
  })

  it('US-32 checklist 2: the original screenshot is shown next to the AI hint', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(baseCard)
    renderCard()

    const screenshot = await screen.findByRole('img', { name: /Скріншот табло/ })

    expect(screenshot).toHaveAttribute('src', '/api/v1/disputes/dispute-1/screenshot')
  })

  it('US-32 sc.3: showing the hint never closes the match by itself', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(baseCard)
    const resolve = vi.spyOn(disputesApi, 'resolveDispute')
    renderCard()

    await screen.findByText('Підказка ШІ')

    expect(resolve).not.toHaveBeenCalled()
  })

  // The winner rule (higher score wins, advancement) is backend logic, verified by core
  // DisputeResolutionIntegrationTest; here we check what the UI sends and that it shows the server's ruling.
  it('US-34 sc.1: one click on "Затвердити підказку ШІ" (+ confirmation) sends AI_SUGGESTION and shows the ruling', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(baseCard)
    vi.spyOn(disputesApi, 'resolveDispute').mockResolvedValue(resolvedResult(13, 9, 'team-a'))
    const { onResolved } = renderCard()

    fireEvent.click(await screen.findByRole('button', { name: 'Затвердити підказку ШІ' }))
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити рішення' }))

    expect(await screen.findByText(/Спір закрито з рахунком 13:9\. Переможець: QA Terror Squad/)).toBeInTheDocument()
    expect(disputesApi.resolveDispute).toHaveBeenCalledWith('dispute-1', { decision: 'AI_SUGGESTION' })
    expect(onResolved).toHaveBeenCalledTimes(1)
  })

  it('US-34 sc.2: the organizer enters 9:13 manually -> MANUAL_SCORE 9/13 is sent and the server ruling is shown', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(baseCard)
    vi.spyOn(disputesApi, 'resolveDispute').mockResolvedValue({
      ...resolvedResult(9, 13, 'team-b'),
      decision: 'MANUAL_SCORE'
    })
    renderCard()

    fireEvent.change(await screen.findByLabelText('QA Terror Squad'), { target: { value: '9' } })
    fireEvent.change(screen.getByLabelText('QA Vegayta Five'), { target: { value: '13' } })
    fireEvent.click(screen.getByRole('button', { name: 'Закрити спір із цим рахунком' }))
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити рішення' }))

    expect(await screen.findByText(/Переможець: QA Vegayta Five/)).toBeInTheDocument()
    expect(disputesApi.resolveDispute).toHaveBeenCalledWith('dispute-1', {
      decision: 'MANUAL_SCORE',
      team1Score: 9,
      team2Score: 13
    })
  })

  it('US-34 sc.3 / US-32 sc.2: score not recognised -> approve is disabled, manual entry is available', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(unrecognizedCard)
    renderCard()

    expect(await screen.findByText(/Рахунок не розпізнано/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Затвердити підказку ШІ' })).toBeDisabled()
    expect(screen.getByRole('form', { name: 'Ручне введення рахунку' })).toBeInTheDocument()
  })

  it('US-34 sc.4: a tied manual score is rejected and nothing is sent', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(baseCard)
    const resolve = vi.spyOn(disputesApi, 'resolveDispute')
    renderCard()

    fireEvent.change(await screen.findByLabelText('QA Terror Squad'), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText('QA Vegayta Five'), { target: { value: '5' } })
    fireEvent.click(screen.getByRole('button', { name: 'Закрити спір із цим рахунком' }))

    expect(screen.getByText('Нічия неможлива — має бути переможець')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Підтвердити рішення' })).not.toBeInTheDocument()
    expect(resolve).not.toHaveBeenCalled()
  })

  it('US-34 checklist 5: only the organizer may close the dispute (AUTH_FORBIDDEN is explained)', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(baseCard)
    vi.spyOn(disputesApi, 'resolveDispute').mockRejectedValue(
      new ApiError({ title: 'Forbidden', status: 403, detail: 'Forbidden', code: 'AUTH_FORBIDDEN' })
    )
    renderCard()

    fireEvent.click(await screen.findByRole('button', { name: 'Затвердити підказку ШІ' }))
    fireEvent.click(screen.getByRole('button', { name: 'Підтвердити рішення' }))

    expect(await screen.findByText('Лише організатор цього турніру може закрити спір.')).toBeInTheDocument()
  })

  it('US-33 sc.1: nicknames found in the locked roster are marked "Збіг із ростером" without a warning', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(baseCard)
    renderCard()

    expect(await screen.findByText('Збіг із ростером')).toBeInTheDocument()
    expect(screen.queryByText(/Можливий сторонній гравець/)).not.toBeInTheDocument()
  })

  it('US-33 sc.2: a nickname missing from the roster is flagged and the card warns about a possible ringer', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue({
      ...baseCard,
      aiAnalysis: {
        ...baseCard.aiAnalysis!,
        allRosterNicknamesFound: false,
        detectedNicknames: ['BOT Mayer', 'Smurf_Pro'],
        nicknameChecks: [
          ...baseCard.aiAnalysis!.nicknameChecks,
          { nickname: 'Smurf_Pro', sideTeamId: 'team-b', inRoster: false, matchedTeamId: null, matchedRosterName: null }
        ]
      }
    })
    renderCard()

    expect(await screen.findByText('Не знайдено в ростері')).toBeInTheDocument()
    expect(screen.getByText(/Можливий сторонній гравець/)).toBeInTheDocument()
  })

  it('US-33 sc.3: no nicknames recognised -> "нікнейми не розпізнано" and no ringer warning', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue(unrecognizedCard)
    renderCard()

    expect(await screen.findByText(/нікнейми не розпізнано/)).toBeInTheDocument()
    expect(screen.queryByText(/Можливий сторонній гравець/)).not.toBeInTheDocument()
  })

  it('US-32: while the AI analysis is PENDING the card refreshes and stops once the result arrives', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const getDispute = vi
      .spyOn(disputesApi, 'getDispute')
      .mockResolvedValueOnce({ ...baseCard, aiAnalysisStatus: 'PENDING', aiAnalysis: null, canApproveAiSuggestion: false })
      .mockResolvedValue(baseCard)
    renderCard()

    expect(await screen.findByText('ШІ аналізує скріншот…')).toBeInTheDocument()
    await act(() => vi.advanceTimersByTimeAsync(PENDING_POLL_MS))
    await waitFor(() => expect(screen.getByText(/впевненість 92%/)).toBeInTheDocument())
    await act(() => vi.advanceTimersByTimeAsync(PENDING_POLL_MS * 3))

    expect(getDispute).toHaveBeenCalledTimes(2)
  })

  it('US-34 checklist 6: a resolved dispute is read-only and shows the final decision', async () => {
    vi.spyOn(disputesApi, 'getDispute').mockResolvedValue({
      ...baseCard,
      status: 'RESOLVED',
      canApproveAiSuggestion: false,
      resolution: {
        decision: 'AI_SUGGESTION',
        winnerTeamId: 'team-a',
        team1Score: 13,
        team2Score: 9,
        resolutionNotes: null,
        resolvedByUserId: 'organizer',
        resolvedAt: '2026-11-25T12:05:00Z'
      }
    })
    renderCard()

    expect(await screen.findByText(/Опрацьовано — рішення організатора остаточне/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Затвердити підказку ШІ' })).not.toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Ручне введення рахунку' })).not.toBeInTheDocument()
  })
})

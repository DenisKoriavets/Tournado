import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/http'
import * as matchesApi from '../api/matches'
import { ScoreEntryForm } from './ScoreEntryForm'

// US-27 "Внесення рахунку матчу капітаном" + US-28 sc.1/sc.2 (outcome shown to the captain).
describe('ScoreEntryForm (US-27)', () => {
  const callbacks = () => ({
    onMatchCompleted: vi.fn(),
    onRoomOutdated: vi.fn(),
    onOpenDispute: vi.fn()
  })

  const renderForm = (handlers = callbacks()) => {
    render(
      <ScoreEntryForm
        matchId="match-1"
        team1Id="team-1"
        team2Id="team-2"
        team1Name="Navi Junior KMA"
        team2Name="KPI Titans"
        {...handlers}
      />
    )
    return handlers
  }

  const enterScore = (team1: string, team2: string) => {
    fireEvent.change(screen.getByLabelText('Navi Junior KMA'), { target: { value: team1 } })
    fireEvent.change(screen.getByLabelText('KPI Titans'), { target: { value: team2 } })
    fireEvent.click(screen.getByRole('button', { name: 'Надіслати рахунок' }))
  }

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('US-27 sc.1: saves 13:9 and shows "waiting for opponent" without revealing any opponent score', async () => {
    vi.spyOn(matchesApi, 'submitMatchScore').mockResolvedValue({
      matchStatus: 'IN_PROGRESS',
      winnerTeamId: null,
      bothSubmitted: false,
      scoresMatched: false
    })
    renderForm()

    enterScore('13', '9')

    expect(await screen.findByText('Рахунок збережено. Очікується підтвердження суперником.')).toBeInTheDocument()
    expect(matchesApi.submitMatchScore).toHaveBeenCalledWith('match-1', 13, 9)
    expect(screen.queryByLabelText('Navi Junior KMA')).not.toBeInTheDocument()
  })

  it.each([
    ['-1', '9'],
    ['abc', '9'],
    ['13.5', '9']
  ])('US-27 sc.2: invalid value %s is not sent and an error appears next to the field', (team1, team2) => {
    const submit = vi.spyOn(matchesApi, 'submitMatchScore')
    renderForm()

    enterScore(team1, team2)

    expect(submit).not.toHaveBeenCalled()
    expect(screen.getByText("Лише ціле невід'ємне число")).toBeInTheDocument()
    expect(screen.getByLabelText('Navi Junior KMA')).toHaveAttribute('aria-invalid', 'true')
  })

  it('US-27 sc.3: a tied score 5:5 is rejected with an explanation and not sent', () => {
    const submit = vi.spyOn(matchesApi, 'submitMatchScore')
    renderForm()

    enterScore('5', '5')

    expect(submit).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Нічия неможлива — має бути переможець')
  })

  it('US-28 sc.2: second captain disagrees -> "scores do not match" and the dispute button', async () => {
    vi.spyOn(matchesApi, 'submitMatchScore').mockResolvedValue({
      matchStatus: 'IN_PROGRESS',
      winnerTeamId: null,
      bothSubmitted: true,
      scoresMatched: false
    })
    const handlers = renderForm()

    enterScore('9', '13')
    fireEvent.click(await screen.findByRole('button', { name: 'Відкрити спір' }))

    expect(screen.getByText(/Рахунки не збігаються/)).toBeInTheDocument()
    expect(handlers.onOpenDispute).toHaveBeenCalledTimes(1)
    expect(handlers.onMatchCompleted).not.toHaveBeenCalled()
  })

  it('US-28 sc.1: scores match -> match completed with the winner, room is asked to reload', async () => {
    vi.spyOn(matchesApi, 'submitMatchScore').mockResolvedValue({
      matchStatus: 'COMPLETED',
      winnerTeamId: 'team-2',
      bothSubmitted: true,
      scoresMatched: true
    })
    const handlers = renderForm()

    enterScore('9', '13')

    expect(await screen.findByText(/Переможець: KPI Titans/)).toBeInTheDocument()
    expect(handlers.onMatchCompleted).toHaveBeenCalledTimes(1)
  })

  it('US-27 checklist 4: 409 SCORE_ALREADY_SUBMITTED (e.g. after reload) switches to the waiting state', async () => {
    vi.spyOn(matchesApi, 'submitMatchScore').mockRejectedValue(
      new ApiError({ title: 'Conflict', status: 409, detail: 'Already submitted', code: 'SCORE_ALREADY_SUBMITTED' })
    )
    renderForm()

    enterScore('13', '9')

    expect(await screen.findByText('Рахунок збережено. Очікується підтвердження суперником.')).toBeInTheDocument()
  })

  it('US-27 checklist 1: 409 MATCH_NOT_IN_PROGRESS shows the reason and reloads the room', async () => {
    vi.spyOn(matchesApi, 'submitMatchScore').mockRejectedValue(
      new ApiError({
        title: 'Conflict',
        status: 409,
        detail: 'Scores can only be submitted while the match is in progress.',
        code: 'MATCH_NOT_IN_PROGRESS'
      })
    )
    const handlers = renderForm()

    enterScore('13', '9')

    expect(await screen.findByText('Scores can only be submitted while the match is in progress.')).toBeInTheDocument()
    expect(handlers.onRoomOutdated).toHaveBeenCalledTimes(1)
  })
})

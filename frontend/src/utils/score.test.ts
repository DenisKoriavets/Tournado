import { describe, expect, it } from 'vitest'
import { parseScorePair, validateScorePair } from './score'

// US-27 AC2/AC3 and US-34 AC4: two non-negative integers, no tie.
describe('validateScorePair', () => {
  it('US-27 sc.1: accepts a valid 13:9 score', () => {
    expect(validateScorePair('13', '9')).toBeNull()
    expect(parseScorePair(' 13 ', '9')).toEqual({ team1Score: 13, team2Score: 9 })
  })

  it.each([
    ['-1', '9', { team1: "Лише ціле невід'ємне число" }],
    ['abc', '9', { team1: "Лише ціле невід'ємне число" }],
    ['13.5', '9', { team1: "Лише ціле невід'ємне число" }],
    ['13', '', { team2: 'Вкажіть рахунок' }],
    ['1000', '9', { team1: 'Не більше 999' }]
  ])('US-27 sc.2: rejects invalid value %s:%s', (team1, team2, expected) => {
    expect(validateScorePair(team1, team2)).toEqual(expected)
  })

  it('US-27 sc.3: rejects a tied score 5:5 because Single Elimination needs a winner', () => {
    expect(validateScorePair('5', '5')).toEqual({ form: 'Нічия неможлива — має бути переможець' })
  })

  it('US-27 checklist 2: accepts the boundary values 0 and 999', () => {
    expect(validateScorePair('0', '999')).toBeNull()
  })
})

import { describe, expect, it } from 'vitest'
import { parseTournamentIdFromPath, tournamentPath } from './route'

// US-11: public tournament link /tournaments/{uuid}.
describe('tournament route', () => {
  const id = '3a013827-3de6-4157-8e4d-b9f83a4d6b71'

  it('US-11 sc.1: parses the public tournament link built by the drawer', () => {
    expect(parseTournamentIdFromPath(tournamentPath(id))).toBe(id)
    expect(parseTournamentIdFromPath(`/tournaments/${id}/`)).toBe(id)
  })

  it.each(['/', '/join/inv_abc', '/tournaments/', '/tournaments/not-a-uuid', `/tournaments/${id}/extra`])(
    'ignores unrelated or malformed path %s',
    (path) => {
      expect(parseTournamentIdFromPath(path)).toBeNull()
    }
  )
})

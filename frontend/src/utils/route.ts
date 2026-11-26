// US-11: the public tournament link has the form /tournaments/{uuid} (TournamentDrawer builds it on copy).

const TOURNAMENT_PATH = /^\/tournaments\/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\/?$/

export function parseTournamentIdFromPath(pathname: string): string | null {
  const match = pathname.match(TOURNAMENT_PATH)
  return match ? match[1].toLowerCase() : null
}

export function tournamentPath(tournamentId: string): string {
  return `/tournaments/${encodeURIComponent(tournamentId)}`
}

export interface UserProfile {
  id: string
  email: string | null
  steamId: string | null
  displayName: string
  supercellPlayerTag: string | null
  chessUsername: string | null
  avatarUrl: string | null
}

export interface UpdateGameIdentifiersPayload {
  steamId: string | null
  supercellPlayerTag: string | null
  chessUsername: string | null
}

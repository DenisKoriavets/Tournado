import type { MatchCredentials, MatchRoom, ReadyState } from '../types/matches'
import { requestJson } from './http'

const BASE_URL = '/api/v1/matches'

export function getMatchRoom(matchId: string): Promise<MatchRoom> {
  return requestJson<MatchRoom>(`${BASE_URL}/${encodeURIComponent(matchId)}/room`, {
    method: 'GET'
  })
}

export function markMatchReady(matchId: string): Promise<ReadyState> {
  return requestJson<ReadyState>(`${BASE_URL}/${encodeURIComponent(matchId)}/ready`, {
    method: 'POST'
  })
}

export function banMatchMap(matchId: string, mapName: string): Promise<MatchRoom> {
  return requestJson<MatchRoom>(`${BASE_URL}/${encodeURIComponent(matchId)}/ban`, {
    method: 'POST',
    body: JSON.stringify({ mapName })
  })
}

export function getMatchCredentials(matchId: string): Promise<MatchCredentials> {
  return requestJson<MatchCredentials>(`${BASE_URL}/${encodeURIComponent(matchId)}/credentials`, {
    method: 'GET'
  })
}

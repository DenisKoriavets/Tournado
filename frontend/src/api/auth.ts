import type { UpdateGameIdentifiersPayload, UserProfile } from '../types/auth'
import { requestJson } from './http'

export const STEAM_LOGIN_URL = '/api/v1/auth/steam/login'

export function requestEmailLoginLink(email: string): Promise<{ message: string }> {
  return requestJson<{ message: string }>('/api/v1/auth/email/request-link', {
    method: 'POST',
    body: JSON.stringify({ email })
  })
}

export function getCurrentUser(): Promise<UserProfile> {
  return requestJson<UserProfile>('/api/v1/auth/me', {
    method: 'GET'
  })
}

export function logoutUser(): Promise<void> {
  return requestJson<void>('/api/v1/auth/logout', {
    method: 'POST'
  })
}

export function updateGameIdentifiers(payload: UpdateGameIdentifiersPayload): Promise<UserProfile> {
  return requestJson<UserProfile>('/api/v1/users/me/game-identifiers', {
    method: 'PATCH',
    body: JSON.stringify(payload)
  })
}

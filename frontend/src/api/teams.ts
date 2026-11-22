import type {
  CreateTeamPayload,
  TeamDetails,
  TeamInvite,
  TeamInvitePreview,
  TeamSummary
} from '../types/teams'
import { requestJson } from './http'

const BASE_URL = '/api/v1/teams'

export function createTeam(payload: CreateTeamPayload): Promise<TeamSummary> {
  return requestJson<TeamSummary>(BASE_URL, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function getMyTeams(): Promise<TeamSummary[]> {
  return requestJson<TeamSummary[]>(`${BASE_URL}/my`, {
    method: 'GET'
  })
}

export function getTeamById(teamId: string): Promise<TeamDetails> {
  return requestJson<TeamDetails>(`${BASE_URL}/${encodeURIComponent(teamId)}`, {
    method: 'GET'
  })
}

export function createTeamInvite(teamId: string): Promise<TeamInvite> {
  return requestJson<TeamInvite>(`${BASE_URL}/${encodeURIComponent(teamId)}/invites`, {
    method: 'POST'
  })
}

export function previewTeamInvite(token: string): Promise<TeamInvitePreview> {
  return requestJson<TeamInvitePreview>(`${BASE_URL}/invites/${encodeURIComponent(token)}`, {
    method: 'GET'
  })
}

export function joinTeamByInvite(token: string): Promise<TeamDetails> {
  return requestJson<TeamDetails>(`${BASE_URL}/join/${encodeURIComponent(token)}`, {
    method: 'POST'
  })
}

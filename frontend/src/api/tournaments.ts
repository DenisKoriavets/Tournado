import type { CreateTournamentPayload, ProblemDetail, Tournament } from '../types/tournament'

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail || problem.title)
    this.name = 'ApiError'
    this.problem = problem
  }
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (res.ok) {
    // 204 No Content handling
    if (res.status === 204) {
      return {} as T
    }
    return (await res.json()) as T
  }

  let problem: ProblemDetail
  try {
    const raw = await res.json()
    if (raw && typeof raw === 'object' && ('title' in raw || 'detail' in raw || 'code' in raw)) {
      problem = {
        type: raw.type,
        title: raw.title || 'Помилка валідації або бізнес-правил',
        status: raw.status || res.status,
        detail: raw.detail || 'Сервер повернув помилку при обробці запиту.',
        instance: raw.instance,
        code: raw.code || `HTTP_${res.status}`,
        timestamp: raw.timestamp,
        invalidParams: Array.isArray(raw.invalidParams) ? raw.invalidParams : undefined
      }
    } else {
      problem = {
        title: res.statusText || 'Помилка HTTP',
        status: res.status,
        detail: `Сервер відхилив запит зі статусом ${res.status}.`,
        code: `HTTP_${res.status}`
      }
    }
  } catch {
    problem = {
      title: res.statusText || 'Неочікувана відповідь сервера',
      status: res.status,
      detail: `Не вдалося прочитати тіло відповіді сервера (статус ${res.status}).`,
      code: `HTTP_${res.status}`
    }
  }

  throw new ApiError(problem)
}

const BASE_URL = '/api/v1/tournaments'

/**
 * Creates a new tournament according to US-07..US-10.
 * POST /api/v1/tournaments
 */
export async function createTournament(payload: CreateTournamentPayload): Promise<Tournament> {
  try {
    const res = await fetch(BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify(payload)
    })
    return await handleResponse<Tournament>(res)
  } catch (err) {
    if (err instanceof ApiError) throw err
    throw new ApiError({
      title: 'Помилка з’єднання',
      status: 0,
      detail: err instanceof Error ? err.message : 'Не вдалося підключитися до сервера Tournado API.',
      code: 'NETWORK_ERROR'
    })
  }
}

/**
 * Retrieves the list of tournaments.
 * GET /api/v1/tournaments
 */
export async function getTournaments(): Promise<Tournament[]> {
  try {
    const res = await fetch(BASE_URL, {
      method: 'GET',
      headers: {
        Accept: 'application/json'
      }
    })
    return await handleResponse<Tournament[]>(res)
  } catch (err) {
    if (err instanceof ApiError) throw err
    throw new ApiError({
      title: 'Помилка з’єднання',
      status: 0,
      detail: err instanceof Error ? err.message : 'Не вдалося завантажити список турнірів.',
      code: 'NETWORK_ERROR'
    })
  }
}

/**
 * Retrieves full details for a tournament by its ID or public slug.
 * GET /api/v1/tournaments/{id}
 */
export async function getTournamentById(id: string): Promise<Tournament> {
  try {
    const res = await fetch(`${BASE_URL}/${encodeURIComponent(id)}`, {
      method: 'GET',
      headers: {
        Accept: 'application/json'
      }
    })
    return await handleResponse<Tournament>(res)
  } catch (err) {
    if (err instanceof ApiError) throw err
    throw new ApiError({
      title: 'Помилка з’єднання',
      status: 0,
      detail: err instanceof Error ? err.message : `Не вдалося завантажити деталі турніру ${id}.`,
      code: 'NETWORK_ERROR'
    })
  }
}

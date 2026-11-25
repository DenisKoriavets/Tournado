import type { ProblemDetail } from '../types/tournament'

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail || problem.title)
    this.name = 'ApiError'
    this.problem = problem
  }
}

async function parseProblemDetail(res: Response): Promise<ProblemDetail> {
  try {
    const raw = await res.json()
    if (raw && typeof raw === 'object' && ('title' in raw || 'detail' in raw || 'code' in raw)) {
      return {
        type: raw.type,
        title: raw.title || 'Помилка валідації або бізнес-правил',
        status: raw.status || res.status,
        detail: raw.detail || 'Сервер повернув помилку при обробці запиту.',
        instance: raw.instance,
        code: raw.code || `HTTP_${res.status}`,
        timestamp: raw.timestamp,
        invalidParams: Array.isArray(raw.invalidParams) ? raw.invalidParams : undefined
      }
    }
    return {
      title: res.statusText || 'Помилка HTTP',
      status: res.status,
      detail: `Сервер відхилив запит зі статусом ${res.status}.`,
      code: `HTTP_${res.status}`
    }
  } catch {
    return {
      title: res.statusText || 'Неочікувана відповідь сервера',
      status: res.status,
      detail: `Не вдалося прочитати тіло відповіді сервера (статус ${res.status}).`,
      code: `HTTP_${res.status}`
    }
  }
}

export async function requestJson<T>(url: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }
  if (options.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  try {
    const res = await fetch(url, {
      ...options,
      credentials: 'include',
      headers
    })

    if (!res.ok) {
      const problem = await parseProblemDetail(res)
      throw new ApiError(problem)
    }

    if (res.status === 204) {
      return undefined as T
    }

    return (await res.json()) as T
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

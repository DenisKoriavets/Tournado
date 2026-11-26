import { afterEach, describe, expect, it, vi } from 'vitest'
import { requestJson } from './http'

describe('requestJson', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  const okResponse = () =>
    new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } })

  it('US-30: sends a FormData body without forcing a JSON Content-Type (browser sets the multipart boundary)', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(okResponse())
    const body = new FormData()
    body.append('screenshot', new File(['x'], 'a.png', { type: 'image/png' }))

    await requestJson('/api/v1/matches/m/disputes', { method: 'POST', body })

    const headers = fetchSpy.mock.calls[0][1]?.headers as Headers
    expect(headers.has('Content-Type')).toBe(false)
    expect(fetchSpy.mock.calls[0][1]?.body).toBe(body)
  })

  it('keeps the JSON Content-Type for string bodies', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(okResponse())

    await requestJson('/api/v1/matches/m/scores', { method: 'POST', body: JSON.stringify({ team1Score: 1 }) })

    const headers = fetchSpy.mock.calls[0][1]?.headers as Headers
    expect(headers.get('Content-Type')).toBe('application/json')
  })

  it('maps an HTML error page (e.g. nginx 413) to a ProblemDetail with code HTTP_413', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('<html>413 Request Entity Too Large</html>', { status: 413, statusText: 'Payload Too Large' })
    )

    await expect(requestJson('/api/v1/matches/m/disputes', { method: 'POST' })).rejects.toMatchObject({
      problem: { status: 413, code: 'HTTP_413' }
    })
  })
})

import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import * as disputesApi from '../api/disputes'
import { ApiError } from '../api/http'
import { MAX_SCREENSHOT_BYTES, OpenDisputeDialog } from './OpenDisputeDialog'

// US-30 "Відкриття суперечки та завантаження доказу".
describe('OpenDisputeDialog (US-30)', () => {
  const pngFile = () => new File([new Uint8Array([137, 80, 78, 71])], 'scoreboard.png', { type: 'image/png' })

  const renderDialog = () => {
    const onOpened = vi.fn()
    const onClose = vi.fn()
    render(<OpenDisputeDialog matchId="match-1" onClose={onClose} onOpened={onOpened} />)
    return { onOpened, onClose }
  }

  const chooseFile = (file: File) => {
    fireEvent.change(screen.getByLabelText(/Скріншот табло/), { target: { files: [file] } })
  }

  const submit = () => fireEvent.click(screen.getByRole('button', { name: 'Відкрити спір' }))

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('US-30 sc.1: a PNG screenshot is uploaded for this match and the dispute is opened', async () => {
    const result = {
      disputeId: 'dispute-1',
      matchId: 'match-1',
      status: 'OPEN' as const,
      screenshotUrl: '/api/v1/disputes/dispute-1/screenshot',
      aiAnalysisStatus: 'PENDING' as const
    }
    vi.spyOn(disputesApi, 'openDispute').mockResolvedValue(result)
    const { onOpened } = renderDialog()
    const file = pngFile()

    chooseFile(file)
    submit()

    await waitFor(() => expect(onOpened).toHaveBeenCalledWith(result))
    expect(disputesApi.openDispute).toHaveBeenCalledWith('match-1', file)
  })

  it('US-30 sc.2: without a screenshot the dispute is not opened and the user is asked for evidence', () => {
    const open = vi.spyOn(disputesApi, 'openDispute')
    renderDialog()

    submit()

    expect(open).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Додайте скріншот підсумкового табло')
  })

  it('US-30 sc.3: a document (PDF) is rejected with the list of allowed formats', () => {
    const open = vi.spyOn(disputesApi, 'openDispute')
    renderDialog()

    chooseFile(new File(['%PDF-1.4'], 'rules.pdf', { type: 'application/pdf' }))
    submit()

    expect(open).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Допустимі формати: PNG, JPG, WEBP')
  })

  it('US-30 checklist 4: a file over 10 MB is rejected before upload', () => {
    const open = vi.spyOn(disputesApi, 'openDispute')
    renderDialog()
    const big = pngFile()
    Object.defineProperty(big, 'size', { value: MAX_SCREENSHOT_BYTES + 1 })

    chooseFile(big)
    submit()

    expect(open).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Файл більший за 10 МБ')
  })

  it('US-30 sc.4: a second dispute in the same match is refused with "already open"', async () => {
    vi.spyOn(disputesApi, 'openDispute').mockRejectedValue(
      new ApiError({ title: 'Conflict', status: 409, detail: 'Dispute already open', code: 'DISPUTE_ALREADY_OPEN' })
    )
    const { onOpened } = renderDialog()

    chooseFile(pngFile())
    submit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Спір у цьому матчі вже відкрито')
    expect(onOpened).not.toHaveBeenCalled()
  })

  it('US-30 checklist 4: nginx 413 (HTML, no ProblemDetail) is shown as the size message', async () => {
    vi.spyOn(disputesApi, 'openDispute').mockRejectedValue(
      new ApiError({ title: 'Payload Too Large', status: 413, detail: '413', code: 'HTTP_413' })
    )
    renderDialog()

    chooseFile(pngFile())
    submit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Файл більший за 10 МБ')
  })
})

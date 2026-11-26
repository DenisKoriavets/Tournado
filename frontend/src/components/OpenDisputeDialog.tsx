import React, { useEffect, useMemo, useState } from 'react'
import { openDispute } from '../api/disputes'
import { ApiError } from '../api/http'
import type { OpenDisputeResult } from '../types/disputes'
import { AlertCircleIcon, CloseIcon, ShieldIcon } from './icons/Icons'

/** Must match ScreenshotValidator.MAX_SCREENSHOT_BYTES in core (10 MiB). */
export const MAX_SCREENSHOT_BYTES = 10 * 1024 * 1024
const ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/webp']

const FORMATS_MESSAGE = 'Допустимі формати: PNG, JPG, WEBP'
const SIZE_MESSAGE = 'Файл більший за 10 МБ'

const ERROR_MESSAGES: Record<string, string> = {
  INVALID_SCREENSHOT_FORMAT: FORMATS_MESSAGE,
  SCREENSHOT_TOO_LARGE: SIZE_MESSAGE,
  HTTP_413: SIZE_MESSAGE,
  DISPUTE_NOT_ALLOWED: 'Спір можна відкрити лише після того, як хоча б один капітан вніс рахунок.',
  DISPUTE_ALREADY_OPEN: 'Спір у цьому матчі вже відкрито — дочекайтеся рішення організатора.',
  ONLY_CAPTAIN_ALLOWED: 'Відкрити спір може лише капітан команди цього матчу.',
  MATCH_NOT_FOUND: 'Матч не знайдено.'
}

export interface OpenDisputeDialogProps {
  matchId: string
  onClose: () => void
  onOpened: (result: OpenDisputeResult) => void
}

/** US-30: captain opens a dispute and uploads the scoreboard screenshot as evidence. */
export const OpenDisputeDialog: React.FC<OpenDisputeDialogProps> = ({ matchId, onClose, onOpened }) => {
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  // Why the last selected file was rejected; kept so that pressing submit does not hide the reason (US-30 sc.3).
  const [rejectionReason, setRejectionReason] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const previewUrl = useMemo(
    () => (file && typeof URL.createObjectURL === 'function' ? URL.createObjectURL(file) : null),
    [file]
  )

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl)
    }
  }, [previewUrl])

  const validateFile = (candidate: File | null): string | null => {
    if (!candidate) return 'Додайте скріншот підсумкового табло — без доказу спір не відкривається.'
    if (!ALLOWED_TYPES.includes(candidate.type)) return FORMATS_MESSAGE
    if (candidate.size > MAX_SCREENSHOT_BYTES) return SIZE_MESSAGE
    return null
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0] ?? null
    const validation = selected ? validateFile(selected) : null
    setError(validation)
    setRejectionReason(validation)
    setFile(validation ? null : selected)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const validation = file ? validateFile(file) : (rejectionReason ?? validateFile(null))
    if (validation || !file) {
      setError(validation)
      return
    }
    setError(null)
    setIsSubmitting(true)
    try {
      const result = await openDispute(matchId, file)
      onOpened(result)
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setError(ERROR_MESSAGES[err.problem.code ?? ''] ?? err.problem.detail)
      } else {
        setError('Не вдалося відкрити спір. Спробуйте ще раз.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn"
      role="dialog"
      aria-modal="true"
      aria-labelledby="open-dispute-title"
    >
      <div className="absolute inset-0" onClick={onClose} />
      <form
        onSubmit={handleSubmit}
        noValidate
        className="relative w-full max-w-md bg-[#121620] border border-[#1E2536] rounded-2xl shadow-2xl p-6 space-y-4 animate-scaleIn"
      >
        <div className="flex items-center justify-between">
          <h3 id="open-dispute-title" className="text-base font-bold text-white flex items-center gap-2">
            <ShieldIcon className="w-4 h-4 text-rose-400" />
            Відкрити спір
          </h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800 transition"
            aria-label="Закрити вікно спору"
          >
            <CloseIcon className="w-4 h-4" />
          </button>
        </div>
        <p className="text-xs text-slate-400 leading-relaxed">
          Завантажте скріншот підсумкового табло гри. ШІ розпізнає рахунок і нікнейми, а остаточне
          рішення ухвалить організатор турніру.
        </p>

        <div className="space-y-2">
          <label htmlFor="dispute-screenshot" className="block text-xs font-semibold text-white">
            Скріншот табло (PNG, JPG, WEBP, до 10 МБ)
          </label>
          <input
            id="dispute-screenshot"
            type="file"
            accept={ALLOWED_TYPES.join(',')}
            onChange={handleFileChange}
            className="block w-full text-xs text-slate-300 file:mr-3 file:px-3 file:py-1.5 file:rounded-lg file:border-0 file:bg-slate-800 file:text-slate-200"
          />
          {previewUrl && (
            <img src={previewUrl} alt="Попередній перегляд скріншота" className="max-h-40 rounded-lg border border-[#1E2536]" />
          )}
        </div>

        {error && (
          <div role="alert" className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2">
            <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition cursor-pointer"
          >
            Скасувати
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition cursor-pointer"
          >
            {isSubmitting ? 'Відкриваємо...' : 'Відкрити спір'}
          </button>
        </div>
      </form>
    </div>
  )
}

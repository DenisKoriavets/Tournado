import React, { useEffect, useState } from 'react'
import { updateGameIdentifiers } from '../api/auth'
import { ApiError } from '../api/http'
import { useAuth } from '../context/AuthContext'
import type { UserProfile } from '../types/auth'
import type { ProblemDetail } from '../types/tournament'
import { AlertCircleIcon, CheckIcon, CloseIcon, UserIcon } from './icons/Icons'

export interface ProfileModalProps {
  isOpen: boolean
  onClose: () => void
  onProfileUpdated?: (profile: UserProfile) => void
}

export const ProfileModal: React.FC<ProfileModalProps> = ({
  isOpen,
  onClose,
  onProfileUpdated
}) => {
  const { user } = useAuth()
  if (!isOpen || !user) return null
  return (
    <ProfileModalContent
      key={user.id}
      user={user}
      onClose={onClose}
      onProfileUpdated={onProfileUpdated}
    />
  )
}

interface ProfileModalContentProps {
  user: UserProfile
  onClose: () => void
  onProfileUpdated?: (profile: UserProfile) => void
}

const ProfileModalContent: React.FC<ProfileModalContentProps> = ({
  user,
  onClose,
  onProfileUpdated
}) => {
  const { refreshUser } = useAuth()
  const [steamId, setSteamId] = useState(user.steamId ?? '')
  const [supercellPlayerTag, setSupercellPlayerTag] = useState(user.supercellPlayerTag ?? '')
  const [chessUsername, setChessUsername] = useState(user.chessUsername ?? '')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [apiError, setApiError] = useState<ProblemDetail | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  const clearFieldError = (key: string) => {
    setFieldErrors((prev) => {
      if (!prev[key]) return prev
      const next = { ...prev }
      delete next[key]
      return next
    })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setApiError(null)
    setSuccessMessage(null)
    setFieldErrors({})
    setIsSubmitting(true)

    try {
      const updated = await updateGameIdentifiers({
        steamId: steamId.trim() ? steamId.trim() : null,
        supercellPlayerTag: supercellPlayerTag.trim() ? supercellPlayerTag.trim() : null,
        chessUsername: chessUsername.trim() ? chessUsername.trim() : null
      })
      setSteamId(updated.steamId ?? '')
      setSupercellPlayerTag(updated.supercellPlayerTag ?? '')
      setChessUsername(updated.chessUsername ?? '')
      await refreshUser()
      onProfileUpdated?.(updated)
      setSuccessMessage('Ігрові ідентифікатори успішно збережено.')
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setApiError(err.problem)
        if (err.problem.invalidParams && err.problem.invalidParams.length > 0) {
          const mapped: Record<string, string> = {}
          for (const param of err.problem.invalidParams) {
            mapped[param.name] = param.reason
          }
          setFieldErrors(mapped)
        }
      } else {
        setApiError({
          title: 'Помилка збереження',
          status: 500,
          detail: 'Не вдалося оновити профіль. Спробуйте пізніше.'
        })
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto bg-black/75 backdrop-blur-sm animate-fadeIn"
      role="dialog"
      aria-modal="true"
      aria-labelledby="profile-modal-title"
    >
      <div className="relative w-full max-w-lg bg-[#121620] border border-[#1E2536] rounded-2xl shadow-2xl overflow-hidden animate-scaleIn">
        {/* Header */}
        <div className="px-6 py-5 border-b border-[#1E2536] flex items-center justify-between bg-[#0E121A]">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-orange-500/10 border border-orange-500/30 text-orange-400">
              <UserIcon className="w-5 h-5" />
            </div>
            <div>
              <h2 id="profile-modal-title" className="text-lg font-bold text-white tracking-tight">
                Профіль гравця
              </h2>
              <p className="text-xs text-slate-400">
                Прив’язка ігрових ідентифікаторів для участі у турнірах
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition"
            aria-label="Закрити профіль"
          >
            <CloseIcon className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-5">
          {/* Account Summary */}
          <div className="p-4 rounded-xl bg-[#0B0E14] border border-[#1E2536] flex items-center justify-between gap-4">
            <div className="min-w-0">
              <div className="text-sm font-bold text-white truncate">{user.displayName}</div>
              <div className="text-xs font-mono text-slate-400 mt-0.5 truncate">
                {user.email ? user.email : 'Пошту не прив’язано (Steam-акаунт)'}
              </div>
            </div>
            <span className="shrink-0 px-2.5 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-[11px] font-mono font-semibold">
              Авторизовано
            </span>
          </div>

          {successMessage && (
            <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2.5 animate-fadeIn">
              <CheckIcon className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>{successMessage}</span>
            </div>
          )}

          {apiError && (
            <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-200 text-xs flex items-start gap-2.5 animate-fadeIn">
              <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1">
                <div className="flex items-center justify-between gap-2">
                  <span className="font-bold text-rose-300">{apiError.title}</span>
                  {apiError.code && (
                    <span className="font-mono text-[10px] px-2 py-0.5 bg-rose-950/80 rounded border border-rose-800/80 text-rose-300">
                      {apiError.code}
                    </span>
                  )}
                </div>
                <p className="mt-1 text-slate-300">{apiError.detail}</p>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Steam ID */}
            <div>
              <label
                htmlFor="profile-steam-id"
                className="block text-xs font-mono uppercase tracking-wider text-slate-300 mb-1.5"
              >
                Steam ID (CS2)
              </label>
              <input
                id="profile-steam-id"
                type="text"
                value={steamId}
                onChange={(e) => {
                  setSteamId(e.target.value)
                  clearFieldError('steamId')
                }}
                placeholder="76561198000000000"
                className={`w-full px-3.5 py-2.5 bg-[#0B0E14] border rounded-xl text-xs font-mono text-white placeholder-slate-600 focus:outline-none transition ${
                  fieldErrors.steamId
                    ? 'border-rose-500 focus:ring-1 focus:ring-rose-500'
                    : 'border-[#1E2536] focus:border-orange-500'
                }`}
              />
              {fieldErrors.steamId ? (
                <p className="text-xs text-rose-400 mt-1">{fieldErrors.steamId}</p>
              ) : (
                <p className="text-[11px] text-slate-500 mt-1">
                  17-значний SteamID64 (починається з 7656119). Залиште порожнім, щоб очистити.
                </p>
              )}
            </div>

            {/* Supercell Player Tag */}
            <div>
              <label
                htmlFor="profile-supercell-tag"
                className="block text-xs font-mono uppercase tracking-wider text-slate-300 mb-1.5"
              >
                Supercell Player Tag (Brawl Stars)
              </label>
              <input
                id="profile-supercell-tag"
                type="text"
                value={supercellPlayerTag}
                onChange={(e) => {
                  setSupercellPlayerTag(e.target.value)
                  clearFieldError('supercellPlayerTag')
                }}
                placeholder="#2PP98L9"
                className={`w-full px-3.5 py-2.5 bg-[#0B0E14] border rounded-xl text-xs font-mono text-white placeholder-slate-600 focus:outline-none transition ${
                  fieldErrors.supercellPlayerTag
                    ? 'border-rose-500 focus:ring-1 focus:ring-rose-500'
                    : 'border-[#1E2536] focus:border-orange-500'
                }`}
              />
              {fieldErrors.supercellPlayerTag ? (
                <p className="text-xs text-rose-400 mt-1">{fieldErrors.supercellPlayerTag}</p>
              ) : (
                <p className="text-[11px] text-slate-500 mt-1">
                  Починається з символу #, 3–14 латинських літер або цифр.
                </p>
              )}
            </div>

            {/* Chess.com Username */}
            <div>
              <label
                htmlFor="profile-chess-username"
                className="block text-xs font-mono uppercase tracking-wider text-slate-300 mb-1.5"
              >
                Chess.com Username
              </label>
              <input
                id="profile-chess-username"
                type="text"
                value={chessUsername}
                onChange={(e) => {
                  setChessUsername(e.target.value)
                  clearFieldError('chessUsername')
                }}
                placeholder="magnuscarlsen"
                className={`w-full px-3.5 py-2.5 bg-[#0B0E14] border rounded-xl text-xs font-mono text-white placeholder-slate-600 focus:outline-none transition ${
                  fieldErrors.chessUsername
                    ? 'border-rose-500 focus:ring-1 focus:ring-rose-500'
                    : 'border-[#1E2536] focus:border-orange-500'
                }`}
              />
              {fieldErrors.chessUsername ? (
                <p className="text-xs text-rose-400 mt-1">{fieldErrors.chessUsername}</p>
              ) : (
                <p className="text-[11px] text-slate-500 mt-1">
                  Нікнейм на платформі Chess.com (3–25 символів: літери, цифри, _ або -).
                </p>
              )}
            </div>

            <div className="pt-3 border-t border-[#1E2536] flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-white transition"
              >
                Закрити
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-5 py-2.5 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition shadow-lg shadow-orange-500/20 cursor-pointer"
              >
                {isSubmitting ? 'Збереження...' : 'Зберегти ідентифікатори'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

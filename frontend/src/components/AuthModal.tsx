import React, { useEffect, useState } from 'react'
import { requestEmailLoginLink, STEAM_LOGIN_URL } from '../api/auth'
import { ApiError } from '../api/http'
import { useAuth } from '../context/AuthContext'
import type { ProblemDetail } from '../types/tournament'
import {
  AlertCircleIcon,
  CheckIcon,
  CloseIcon,
  LogInIcon,
  MailIcon,
  SteamIcon
} from './icons/Icons'

export interface AuthModalProps {
  isOpen: boolean
  onClose: () => void
  promptMessage?: string | null
  errorNotice?: string | null
  onSteamLogin?: () => void
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  promptMessage,
  errorNotice,
  onSteamLogin
}) => {
  if (!isOpen) return null
  return (
    <AuthModalContent
      onClose={onClose}
      promptMessage={promptMessage}
      errorNotice={errorNotice}
      onSteamLogin={onSteamLogin}
    />
  )
}

const AuthModalContent: React.FC<Omit<AuthModalProps, 'isOpen'>> = ({
  onClose,
  promptMessage,
  errorNotice,
  onSteamLogin
}) => {
  const { authModalPrompt, authErrorNotice, clearAuthErrorNotice } = useAuth()
  const [email, setEmail] = useState('')
  const [emailError, setEmailError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [sentToEmail, setSentToEmail] = useState<string | null>(null)
  const [apiError, setApiError] = useState<ProblemDetail | null>(null)

  const resolvedPrompt = promptMessage ?? authModalPrompt
  const resolvedNotice = errorNotice ?? authErrorNotice

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  const handleSteamLogin = () => {
    clearAuthErrorNotice()
    if (onSteamLogin) {
      onSteamLogin()
      return
    }
    window.location.assign(STEAM_LOGIN_URL)
  }

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setEmailError(null)
    setApiError(null)

    const trimmed = email.trim()
    if (!trimmed || !trimmed.includes('@')) {
      setEmailError('Введіть коректну електронну адресу.')
      return
    }

    setIsSubmitting(true)
    try {
      await requestEmailLoginLink(trimmed)
      setSentToEmail(trimmed)
      clearAuthErrorNotice()
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setApiError(err.problem)
      } else {
        setApiError({
          title: 'Помилка відправки',
          status: 500,
          detail: 'Не вдалося надіслати посилання для входу. Спробуйте пізніше.'
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
      aria-labelledby="auth-modal-title"
    >
      <div className="relative w-full max-w-md bg-[#121620] border border-[#1E2536] rounded-2xl shadow-2xl overflow-hidden animate-scaleIn">
        {/* Header */}
        <div className="px-6 py-5 border-b border-[#1E2536] flex items-center justify-between bg-[#0E121A]">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-orange-500/10 border border-orange-500/30 text-orange-400">
              <LogInIcon className="w-5 h-5" />
            </div>
            <div>
              <h2 id="auth-modal-title" className="text-lg font-bold text-white tracking-tight">
                Вхід до Tournado
              </h2>
              <p className="text-xs text-slate-400">
                Steam OpenID або Magic Link на електронну пошту
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-800 transition"
            aria-label="Закрити вікно входу"
          >
            <CloseIcon className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-5">
          {/* Prompt Banner (e.g., when trying to create tournament or join team) */}
          {resolvedPrompt && (
            <div className="p-3.5 rounded-xl bg-orange-500/10 border border-orange-500/30 text-orange-200 text-xs leading-relaxed">
              {resolvedPrompt}
            </div>
          )}

          {/* Steam Cancelled / Auth Error Notice */}
          {resolvedNotice && (
            <div
              role="alert"
              className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-200 flex items-start gap-2.5 text-xs"
            >
              <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1 leading-relaxed">{resolvedNotice}</div>
              <button
                type="button"
                onClick={clearAuthErrorNotice}
                className="text-rose-400 hover:text-rose-200"
                aria-label="Приховати попередження"
              >
                <CloseIcon className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Steam OpenID Login */}
          <div className="space-y-2">
            <button
              type="button"
              onClick={handleSteamLogin}
              className="w-full py-3 px-4 rounded-xl bg-[#171A21] hover:bg-[#1E242E] border border-[#2A3346] hover:border-slate-500 text-white text-sm font-bold transition flex items-center justify-center gap-3 shadow-lg cursor-pointer"
            >
              <SteamIcon className="w-5 h-5 text-slate-100" />
              <span>Увійти через Steam</span>
            </button>
            <p className="text-[11px] text-slate-500 text-center font-mono">
              SteamID64 прив’язується автоматично для матчів CS2
            </p>
          </div>

          <div className="relative flex items-center py-1">
            <div className="flex-grow border-t border-[#1E2536]" />
            <span className="shrink mx-3 text-[11px] font-mono uppercase tracking-wider text-slate-500">
              або через пошту
            </span>
            <div className="flex-grow border-t border-[#1E2536]" />
          </div>

          {/* Email Magic Link Form */}
          {sentToEmail ? (
            <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 space-y-2.5 animate-fadeIn">
              <div className="flex items-start gap-2.5 text-emerald-300">
                <CheckIcon className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <div className="text-xs space-y-1">
                  <div className="font-bold text-sm text-emerald-200">
                    Посилання для входу надіслано
                  </div>
                  <p className="text-slate-300 leading-relaxed">
                    Лист із одноразовим Magic Link відправлено на{' '}
                    <span className="font-mono font-semibold text-white">{sentToEmail}</span>.
                  </p>
                  <p className="text-slate-400 leading-relaxed">
                    У локальному середовищі Docker відкрийте скриньку Mailpit за адресою{' '}
                    <a
                      href="http://localhost:8025"
                      target="_blank"
                      rel="noreferrer"
                      className="font-mono text-orange-400 hover:text-orange-300 underline"
                    >
                      http://localhost:8025
                    </a>{' '}
                    та перейдіть за посиланням у листі.
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSentToEmail(null)}
                className="text-xs font-semibold text-emerald-300 hover:text-emerald-200 underline"
              >
                Вказати іншу пошту
              </button>
            </div>
          ) : (
            <form onSubmit={handleEmailSubmit} className="space-y-3" noValidate>
              <div>
                <label
                  htmlFor="auth-email-input"
                  className="block text-xs font-mono uppercase tracking-wider text-slate-300 mb-1.5"
                >
                  Увійти через пошту (Magic Link)
                </label>
                <div className="relative">
                  <MailIcon className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                  <input
                    id="auth-email-input"
                    type="email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value)
                      if (emailError) setEmailError(null)
                    }}
                    placeholder="player@ukma.edu.ua"
                    className={`w-full pl-10 pr-4 py-2.5 bg-[#0B0E14] border rounded-xl text-sm text-white placeholder-slate-600 focus:outline-none transition ${
                      emailError
                        ? 'border-rose-500 focus:ring-1 focus:ring-rose-500'
                        : 'border-[#1E2536] focus:border-orange-500'
                    }`}
                  />
                </div>
                {emailError && <p className="text-xs text-rose-400 mt-1">{emailError}</p>}
              </div>

              {apiError && (
                <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-xs text-rose-200 flex items-start gap-2">
                  <AlertCircleIcon className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                  <div>
                    <div className="font-semibold text-rose-300">{apiError.title}</div>
                    <div className="mt-0.5">{apiError.detail}</div>
                  </div>
                </div>
              )}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full py-2.5 px-4 rounded-xl bg-orange-500 hover:bg-orange-400 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold transition flex items-center justify-center gap-2 shadow-lg shadow-orange-500/20 cursor-pointer"
              >
                <MailIcon className="w-4 h-4" />
                <span>{isSubmitting ? 'Відправка посилання...' : 'Отримати Magic Link'}</span>
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}

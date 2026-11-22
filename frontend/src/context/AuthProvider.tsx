import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { getCurrentUser, logoutUser } from '../api/auth'
import { ApiError } from '../api/http'
import type { UserProfile } from '../types/auth'
import { AuthContext, type AuthContextValue } from './AuthContext'

function readInitialAuthError(): string | null {
  if (typeof window === 'undefined') return null
  const params = new URLSearchParams(window.location.search)
  const authError = params.get('authError')
  if (authError === 'steam_cancelled') {
    return 'Авторизацію через Steam було скасовано. Ви можете спробувати ще раз або увійти через електронну пошту.'
  }
  return null
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [isAuthModalOpen, setIsAuthModalOpen] = useState<boolean>(false)
  const [authModalPrompt, setAuthModalPrompt] = useState<string | null>(null)
  const [authErrorNotice, setAuthErrorNotice] = useState<string | null>(() => readInitialAuthError())

  useEffect(() => {
    if (typeof window === 'undefined') return
    const params = new URLSearchParams(window.location.search)
    if (params.has('authError')) {
      params.delete('authError')
      const nextSearch = params.toString()
      const nextUrl = `${window.location.pathname}${nextSearch ? `?${nextSearch}` : ''}${window.location.hash}`
      window.history.replaceState({}, '', nextUrl)
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    getCurrentUser()
      .then((profile) => {
        if (!cancelled) {
          setUser(profile)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          if (err instanceof ApiError && err.problem.status === 401) {
            setUser(null)
          } else {
            setUser(null)
          }
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const refreshUser = useCallback(async (): Promise<UserProfile | null> => {
    try {
      const profile = await getCurrentUser()
      setUser(profile)
      return profile
    } catch {
      setUser(null)
      return null
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutUser()
    } finally {
      setUser(null)
    }
  }, [])

  const openAuthModal = useCallback((prompt?: string) => {
    setAuthModalPrompt(prompt ?? null)
    setIsAuthModalOpen(true)
  }, [])

  const closeAuthModal = useCallback(() => {
    setIsAuthModalOpen(false)
    setAuthModalPrompt(null)
  }, [])

  const clearAuthErrorNotice = useCallback(() => {
    setAuthErrorNotice(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      isAuthModalOpen,
      authModalPrompt,
      authErrorNotice,
      openAuthModal,
      closeAuthModal,
      refreshUser,
      logout,
      clearAuthErrorNotice
    }),
    [
      user,
      isLoading,
      isAuthModalOpen,
      authModalPrompt,
      authErrorNotice,
      openAuthModal,
      closeAuthModal,
      refreshUser,
      logout,
      clearAuthErrorNotice
    ]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

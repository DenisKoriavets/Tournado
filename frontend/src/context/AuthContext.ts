import { createContext, useContext } from 'react'
import type { UserProfile } from '../types/auth'

export interface AuthContextValue {
  user: UserProfile | null
  isLoading: boolean
  isAuthModalOpen: boolean
  authModalPrompt: string | null
  authErrorNotice: string | null
  openAuthModal: (prompt?: string) => void
  closeAuthModal: () => void
  refreshUser: () => Promise<UserProfile | null>
  logout: () => Promise<void>
  clearAuthErrorNotice: () => void
}

const defaultAuthContext: AuthContextValue = {
  user: null,
  isLoading: false,
  isAuthModalOpen: false,
  authModalPrompt: null,
  authErrorNotice: null,
  openAuthModal: () => {},
  closeAuthModal: () => {},
  refreshUser: async () => null,
  logout: async () => {},
  clearAuthErrorNotice: () => {}
}

export const AuthContext = createContext<AuthContextValue>(defaultAuthContext)

export function useAuth(): AuthContextValue {
  return useContext(AuthContext)
}

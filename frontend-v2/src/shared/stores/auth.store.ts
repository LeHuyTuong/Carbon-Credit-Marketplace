import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export const ROLES = ['EV_OWNER', 'COMPANY', 'CVA', 'ADMIN'] as const

export type Role = (typeof ROLES)[number]

export interface AuthUser {
  id?: string | number
  email: string
  fullName?: string
  role: Role
}

interface AuthState {
  token: string | null
  user: AuthUser | null
  role: Role | null
  setAuth: (payload: { token: string; user?: AuthUser | null; role?: Role | null }) => void
  setUser: (user: AuthUser) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      role: null,
      setAuth: ({ token, user, role }) =>
        set({
          token,
          user: user ?? null,
          role: role ?? user?.role ?? null,
        }),
      setUser: (user) => set({ user, role: user.role }),
      logout: () => set({ token: null, user: null, role: null }),
    }),
    {
      name: 'carbonx-auth',
      partialize: (state) => ({ token: state.token, user: state.user, role: state.role }),
    },
  ),
)

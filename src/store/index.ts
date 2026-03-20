import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { CurrentUserVO, LoginResponse } from '@/lib/contracts';
import { authService } from '@/lib/services';
import { clearStoredSession, readStoredSession, writeStoredSession } from '@/lib/session';

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous';

interface AuthStore {
  status: AuthStatus;
  session: LoginResponse | null;
  user: CurrentUserVO | null;
  error: string | null;
  initialize: () => Promise<void>;
  login: (payload: { usernameOrEmail: string; password: string }) => Promise<void>;
  logout: () => Promise<void>;
  syncFromStorage: () => void;
  clearError: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  status: 'idle',
  session: readStoredSession(),
  user: readStoredSession()?.userInfo ?? null,
  error: null,

  initialize: async () => {
    const stored = readStoredSession();
    if (!stored) {
      set({ status: 'anonymous', session: null, user: null, error: null });
      return;
    }
    set({ status: 'loading', session: stored, user: stored.userInfo, error: null });
    try {
      const user = await authService.me();
      const nextSession = { ...stored, userInfo: user };
      writeStoredSession(nextSession);
      set({ status: 'authenticated', session: nextSession, user, error: null });
    } catch (error) {
      clearStoredSession();
      set({
        status: 'anonymous',
        session: null,
        user: null,
        error: error instanceof Error ? error.message : 'Failed to initialize session',
      });
    }
  },

  login: async (payload) => {
    set({ status: 'loading', error: null });
    try {
      const session = await authService.login(payload);
      writeStoredSession(session);
      set({ status: 'authenticated', session, user: session.userInfo, error: null });
    } catch (error) {
      clearStoredSession();
      set({
        status: 'anonymous',
        session: null,
        user: null,
        error: error instanceof Error ? error.message : 'Login failed',
      });
      throw error;
    }
  },

  logout: async () => {
    try {
      if (get().session) {
        await authService.logout();
      }
    } catch {
      // Best-effort remote logout; local session is authoritative for the client.
    } finally {
      clearStoredSession();
      set({ status: 'anonymous', session: null, user: null, error: null });
    }
  },

  syncFromStorage: () => {
    const session = readStoredSession();
    set({
      status: session ? 'authenticated' : 'anonymous',
      session,
      user: session?.userInfo ?? null,
    });
  },

  clearError: () => set({ error: null }),

  isAuthenticated: () => get().status === 'authenticated' && !!get().session?.accessToken,
}));

interface UIStore {
  isSidebarCollapsed: boolean;
  isDarkMode: boolean;
  isAssistantOpen: boolean;
  assistantDraft: string;
  toggleSidebar: () => void;
  toggleDarkMode: () => void;
  openAssistant: (seed?: string) => void;
  closeAssistant: () => void;
  setAssistantDraft: (value: string) => void;
}

export const useUIStore = create<UIStore>()(
  persist(
    (set) => ({
      isSidebarCollapsed: false,
      isDarkMode: false,
      isAssistantOpen: false,
      assistantDraft: '',
      toggleSidebar: () => set((state) => ({ isSidebarCollapsed: !state.isSidebarCollapsed })),
      toggleDarkMode: () => set((state) => ({ isDarkMode: !state.isDarkMode })),
      openAssistant: (seed) => set((state) => ({
        isAssistantOpen: true,
        assistantDraft: seed ?? state.assistantDraft,
      })),
      closeAssistant: () => set({ isAssistantOpen: false }),
      setAssistantDraft: (value) => set({ assistantDraft: value }),
    }),
    { name: 'ef-ui-storage-v2' }
  )
);

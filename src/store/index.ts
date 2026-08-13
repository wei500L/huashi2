import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { CurrentUserVO, LoginRequest, LoginResponse, RegisterStudentRequest } from '@/lib/contracts';
import { getApiErrorMessage, restoreSessionFromCookie } from '@/lib/api';
import type { SupportedLocale } from '@/lib/locale';
import { readStoredLocale, writeStoredLocale } from '@/lib/locale';
import { authService } from '@/lib/services';
import { clearStoredSession, readStoredSession, writeStoredSession } from '@/lib/session';
import { workspacePreferenceKey } from '@/lib/workspaces';
import type { WorkspaceId } from '@/lib/workspaces';

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous';

interface AuthStore {
  status: AuthStatus;
  session: LoginResponse | null;
  user: CurrentUserVO | null;
  error: string | null;
  initialize: () => Promise<void>;
  login: (payload: LoginRequest) => Promise<void>;
  registerStudent: (payload: RegisterStudentRequest) => Promise<void>;
  logout: () => Promise<void>;
  syncFromStorage: () => void;
  clearError: () => void;
  isAuthenticated: () => boolean;
}

let initializePromise: Promise<void> | null = null;

export const useAuthStore = create<AuthStore>((set, get) => ({
  status: 'idle',
  session: readStoredSession(),
  user: readStoredSession()?.userInfo ?? null,
  error: null,

  initialize: async () => {
    if (initializePromise) {
      return initializePromise;
    }

    initializePromise = (async () => {
      set({ status: 'loading', error: null });
      try {
        const restored = readStoredSession() ?? await restoreSessionFromCookie();
        if (!restored?.accessToken) {
          clearStoredSession();
          set({ status: 'anonymous', session: null, user: null, error: null });
          return;
        }
        writeStoredSession(restored);
        set({ status: 'loading', session: restored, user: restored.userInfo, error: null });
        const user = await authService.me();
        const nextSession = { ...restored, userInfo: user };
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
    })().finally(() => {
      initializePromise = null;
    });

    return initializePromise;
  },

  login: async (payload) => {
    set({ status: 'loading', error: null });
    try {
      const session = await authService.login(payload);
      writeStoredSession(session);
      const stored = readStoredSession();
      if (!stored) {
        throw new Error('Login returned an invalid session');
      }
      set({ status: 'authenticated', session: stored, user: stored.userInfo, error: null });
    } catch (error) {
      clearStoredSession();
      set({
        status: 'anonymous',
        session: null,
        user: null,
        error: getApiErrorMessage(error, '登录失败'),
      });
      throw error;
    }
  },

  registerStudent: async (payload) => {
    set({ status: 'loading', error: null });
    try {
      const session = await authService.registerStudent(payload);
      writeStoredSession(session);
      const stored = readStoredSession();
      if (!stored) {
        throw new Error('Registration returned an invalid session');
      }
      set({ status: 'authenticated', session: stored, user: stored.userInfo, error: null });
    } catch (error) {
      clearStoredSession();
      set({
        status: 'anonymous',
        session: null,
        user: null,
        error: getApiErrorMessage(error, '注册失败'),
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
  isMobileSidebarOpen: boolean;
  isDarkMode: boolean;
  locale: SupportedLocale;
  isAssistantOpen: boolean;
  assistantDraft: string;
  activeAssistantConversationId: string | null;
  activeWorkspace: WorkspaceId | null;
  preferredWorkspaceByUser: Record<string, WorkspaceId>;
  toggleSidebar: () => void;
  openMobileSidebar: () => void;
  closeMobileSidebar: () => void;
  toggleDarkMode: () => void;
  setLocale: (locale: SupportedLocale) => void;
  openAssistant: (seed?: string) => void;
  closeAssistant: () => void;
  setAssistantDraft: (value: string) => void;
  setActiveAssistantConversation: (conversationId: string | null) => void;
  setActiveWorkspace: (workspace: WorkspaceId | null, user?: Pick<CurrentUserVO, 'id' | 'username'> | null) => void;
}

export const useUIStore = create<UIStore>()(
  persist(
    (set) => ({
      isSidebarCollapsed: false,
      isMobileSidebarOpen: false,
      isDarkMode: false,
      locale: readStoredLocale(),
      isAssistantOpen: false,
      assistantDraft: '',
      activeAssistantConversationId: null,
      activeWorkspace: null,
      preferredWorkspaceByUser: {},
      toggleSidebar: () => set((state) => ({ isSidebarCollapsed: !state.isSidebarCollapsed })),
      openMobileSidebar: () => set({ isMobileSidebarOpen: true }),
      closeMobileSidebar: () => set({ isMobileSidebarOpen: false }),
      toggleDarkMode: () => set((state) => ({ isDarkMode: !state.isDarkMode })),
      setLocale: (locale) => {
        writeStoredLocale(locale);
        set({ locale });
      },
      openAssistant: (seed) => set((state) => ({
        isAssistantOpen: true,
        assistantDraft: seed ?? state.assistantDraft,
        activeAssistantConversationId: seed && seed.trim() ? null : state.activeAssistantConversationId,
      })),
      closeAssistant: () => set({ isAssistantOpen: false }),
      setAssistantDraft: (value) => set({ assistantDraft: value }),
      setActiveAssistantConversation: (conversationId) => set({ activeAssistantConversationId: conversationId }),
      setActiveWorkspace: (workspace, user) =>
        set((state) => {
          const nextState: Pick<UIStore, 'activeWorkspace' | 'preferredWorkspaceByUser'> = {
            activeWorkspace: workspace,
            preferredWorkspaceByUser: state.preferredWorkspaceByUser,
          };

          const preferenceKey = workspacePreferenceKey(user);
          if (workspace && preferenceKey) {
            const currentPreference = state.preferredWorkspaceByUser[preferenceKey];
            if (currentPreference !== workspace) {
              nextState.preferredWorkspaceByUser = {
                ...state.preferredWorkspaceByUser,
                [preferenceKey]: workspace,
              };
            }
          }

          const activeUnchanged = state.activeWorkspace === nextState.activeWorkspace;
          const preferencesUnchanged = state.preferredWorkspaceByUser === nextState.preferredWorkspaceByUser;
          if (activeUnchanged && preferencesUnchanged) {
            return state;
          }

          return nextState;
        }),
    }),
    { name: 'ef-ui-storage-v2' }
  )
);

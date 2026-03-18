import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// 1. 用户认证 Store
interface User {
  id: string;
  username: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  token: string;
}

interface AuthStore {
  user: User | null;
  isAuthenticated: boolean;
  login: (userData: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      login: (userData) => set({ user: userData, isAuthenticated: true }),
      logout: () => {
        localStorage.removeItem('token');
        set({ user: null, isAuthenticated: false });
      },
    }),
    { name: 'ef-auth-storage' }
  )
);

// 2. UI 交互 Store
interface UIStore {
  isSidebarCollapsed: boolean;
  isDarkMode: boolean;
  toggleSidebar: () => void;
  toggleDarkMode: () => void;
}

export const useUIStore = create<UIStore>()(
  persist(
    (set) => ({
      isSidebarCollapsed: false,
      isDarkMode: false,
      toggleSidebar: () => set((state) => ({ isSidebarCollapsed: !state.isSidebarCollapsed })),
      toggleDarkMode: () => {
        const root = window.document.documentElement;
        set((state) => {
          const nextMode = !state.isDarkMode;
          if (nextMode) root.classList.add('dark');
          else root.classList.remove('dark');
          return { isDarkMode: nextMode };
        });
      },
    }),
    { name: 'ef-ui-storage' }
  )
);

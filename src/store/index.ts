import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// 1. User Auth Store
interface User {
  id: string;
  username: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  token: string;
}

interface AuthStore {
  user: User | null;
  isAuthenticated: boolean;
  login: (role: 'STUDENT' | 'TEACHER' | 'ADMIN') => void;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      login: (role) => set({ 
        isAuthenticated: true,
        user: { 
          id: 'mock-1', 
          username: '李华', 
          role, 
          token: 'mock-jwt' 
        } 
      }),
      logout: () => {
        localStorage.removeItem('ef-auth-storage');
        set({ user: null, isAuthenticated: false });
      },
    }),
    { name: 'ef-auth-storage' }
  )
);

// 2. UI Interaction Store
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
      isDarkMode: true, // Default to Dark Mode for high-fidelity experience
      toggleSidebar: () => set((state) => ({ isSidebarCollapsed: !state.isSidebarCollapsed })),
      toggleDarkMode: () => set((state) => ({ isDarkMode: !state.isDarkMode })),
    }),
    { name: 'ef-ui-storage' }
  )
);

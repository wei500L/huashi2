import { create } from 'zustand';
import { UserProfile } from '../types';

interface AuthState {
  user: UserProfile | null;
  isAuthenticated: boolean;
  login: (role: 'STUDENT' | 'TEACHER' | 'ADMIN') => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  login: (role) => set({
    isAuthenticated: true,
    user: {
      id: 'mock-1',
      username: 'Test User',
      role,
      proficiency: { english: 'B2', french: 'B1', compositeScore: 65 },
      transferStats: {
        positiveTransferScore: 0.75,
        negativeTransferRisk: 0.35,
        contextSensitivity: 0.60,
        semanticDiscrimination: 0.55
      }
    }
  }),
  logout: () => set({ user: null, isAuthenticated: false }),
}));

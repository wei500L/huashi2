import { create } from 'zustand';
import { WordPair, WordPairType, TrainingRecord, CognitiveMetrics } from '../types';

interface TransferState {
  currentSession: TrainingRecord[];
  activeWordPair: WordPair | null;
  wordPairs: WordPair[];
  
  // Actions
  setWordPairs: (pairs: WordPair[]) => void;
  startDiagnostic: () => void;
  recordResponse: (pairId: string, metrics: CognitiveMetrics) => void;
  resetSession: () => void;
}

export const useTransferStore = create<TransferState>((set) => ({
  currentSession: [],
  activeWordPair: null,
  wordPairs: [
    {
      id: 'w1',
      en: 'table',
      fr: 'table',
      zh: '桌子',
      type: WordPairType.Cognate,
      contextLevel: 'LOW',
      sentenceEn: 'The book is on the table.',
      sentenceFr: 'Le livre est sur la table.',
      sentenceZh: '书在桌子上。'
    },
    {
      id: 'w2',
      en: 'coin',
      fr: 'coin',
      zh: '硬币 / 角落',
      type: WordPairType.FalseFriend,
      contextLevel: 'HIGH',
      sentenceEn: 'I found a gold coin.',
      sentenceFr: 'Il attend au coin de la rue.',
      sentenceZh: '我找到一枚金币。/ 他在街角等待。'
    }
  ],

  setWordPairs: (pairs) => set({ wordPairs: pairs }),
  
  startDiagnostic: () => set((state) => ({ 
    activeWordPair: state.wordPairs[0],
    currentSession: [] 
  })),

  recordResponse: (pairId, metrics) => set((state) => ({
    currentSession: [
      ...state.currentSession,
      {
        id: Math.random().toString(36).substr(2, 9),
        timestamp: new Date().toISOString(),
        type: 'DIAGNOSTIC',
        score: metrics.accuracy * 100,
        metrics,
        wordPairId: pairId
      }
    ],
    // 简单的切题逻辑
    activeWordPair: state.wordPairs.find((_, i) => state.wordPairs[i].id === pairId) 
      ? state.wordPairs[state.wordPairs.indexOf(state.wordPairs.find(w => w.id === pairId)!) + 1] || null
      : null
  })),

  resetSession: () => set({ currentSession: [], activeWordPair: null }),
}));

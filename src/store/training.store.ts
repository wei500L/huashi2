import { create } from 'zustand';
import { TrainingSession, Exercise, TrainingPlan, TrainingMode } from '@/types/training';
import { DiagnosisResult } from '@/types/diagnosis';

interface TrainingState {
  currentSession: TrainingSession | null;
  plan: TrainingPlan | null;
  history: TrainingSession[];
  
  // Actions
  generatePlan: (result: DiagnosisResult) => void;
  startSession: (mode: TrainingMode) => void;
  submitAnswer: (answer: { exerciseId: string; isCorrect: boolean; responseTime: number }) => void;
  endSession: () => void;
  reset: () => void;
}

export const useTrainingStore = create<TrainingState>((set, get) => ({
  currentSession: null,
  plan: null,
  history: [],

  // 前端自适应算法 Mock
  generatePlan: (result) => {
    let priority: TrainingMode = 'COGNATE_BOOST';
    let reason = "基于你的词汇库分析，我们建议从基础的正迁移强化开始。";
    let targetMetrics = ["提高正迁移率", "稳定反应时"];

    if (result.negativeTransferRisk > 0.4) {
      priority = 'FALSE_FRIEND_DISCRIM';
      reason = "诊断显示你在同形异义词（False Friends）上存在较高的负迁移风险，建议进行专项辨析。";
      targetMetrics = ["降低错误率", "识别语义陷阱"];
    } else if (result.contextSensitivity < 0.4) {
      priority = 'CONTEXT_FIX';
      reason = "你的语境依赖度较低，建议通过语境纠偏训练提高语义判断的准确性。";
      targetMetrics = ["提升语境敏感度", "长难句语义锁定"];
    } else if (result.avgRT > 1200) {
      priority = 'SPEED_CHALLENGE';
      reason = "你的平均反应时较长，建议通过快速决策任务提升认知流畅度。";
      targetMetrics = ["缩短 RT (ms)", "自动化加工强化"];
    }

    set({
      plan: {
        recommendationReason: reason,
        priorityMode: priority,
        targetMetrics,
        suggestedSessions: [
          { mode: 'FALSE_FRIEND_DISCRIM', label: '纠偏：同形异义词', count: 15 },
          { mode: 'CONTEXT_FIX', label: '进阶：语境释义', count: 10 },
          { mode: 'COGNATE_BOOST', label: '强化：正迁移促进', count: 20 },
        ]
      }
    });
  },

  startSession: (mode) => {
    const mockExercises: Exercise[] = [
      {
        id: 'ex1',
        type: 'CHOICE',
        mode,
        wordPair: { en: 'coin', fr: 'coin', zh: '硬币 / 角落', type: 'FALSE_FRIEND' },
        content: {
          question: '在句子 "Il attend au coin de la rue" 中，"coin" 的含义是？',
          options: ['硬币 (Coin)', '角落 (Corner)', '街道 (Street)', '等待 (Waiting)'],
          correctAnswer: 1,
          explanation: '法文中的 "coin" 指的是“角落”，而英文中的 "coin" 是“硬币”。这是一个典型的同形异义词陷阱。',
          contextLevel: 'HIGH',
          sentence: 'Il attend au coin de la rue.'
        },
        cognitiveTag: 'TRAP'
      },
      {
        id: 'ex2',
        type: 'JUDGMENT',
        mode,
        wordPair: { en: 'table', fr: 'table', zh: '桌子', type: 'COGNATE' },
        content: {
          question: '英法词汇 "table" 在大多数日常语境下语义是否完全一致？',
          options: ['是 (Yes)', '否 (No)'],
          correctAnswer: 0,
          explanation: '这是一个典型的同源词（Cognate），在英法两语中都保留了拉丁语中的核心含义。',
          contextLevel: 'LOW',
        },
        cognitiveTag: 'OPPORTUNITY'
      }
    ];

    set({
      currentSession: {
        id: Math.random().toString(36),
        mode,
        startTime: Date.now(),
        exercises: mockExercises,
        currentIdx: 0,
        answers: []
      }
    });
  },

  submitAnswer: (ans) => {
    const { currentSession } = get();
    if (!currentSession) return;

    const newAnswers = [...currentSession.answers, ans];
    const nextIdx = currentSession.currentIdx + 1;
    
    set({
      currentSession: {
        ...currentSession,
        answers: newAnswers,
        currentIdx: nextIdx
      }
    });
  },

  endSession: () => {
    const { currentSession, history } = get();
    if (currentSession) {
      set({ history: [...history, currentSession], currentSession: null });
    }
  },

  reset: () => set({ currentSession: null, plan: null })
}));

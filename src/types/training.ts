import { WordPairType, ContextLevel } from './diagnosis';

export type TrainingMode = 'COGNATE_BOOST' | 'FALSE_FRIEND_DISCRIM' | 'CONTEXT_FIX' | 'SPEED_CHALLENGE';

export interface Exercise {
  id: string;
  type: 'CHOICE' | 'MATCH' | 'JUDGMENT' | 'FILL_IN';
  mode: TrainingMode;
  wordPair: {
    en: string;
    fr: string;
    zh: string;
    type: WordPairType;
  };
  content: {
    question: string;
    options?: string[];
    correctAnswer: string | number;
    explanation: string;
    contextLevel: ContextLevel;
    sentence?: string;
  };
  cognitiveTag: 'OPPORTUNITY' | 'TRAP'; // 认知标签：正迁移机会 vs 负迁移陷阱
}

export interface TrainingSession {
  id: string;
  mode: TrainingMode;
  startTime: number;
  exercises: Exercise[];
  currentIdx: number;
  answers: {
    exerciseId: string;
    isCorrect: boolean;
    responseTime: number;
  }[];
}

export interface TrainingPlan {
  recommendationReason: string;
  priorityMode: TrainingMode;
  targetMetrics: string[];
  suggestedSessions: {
    mode: TrainingMode;
    label: string;
    count: number;
  }[];
}

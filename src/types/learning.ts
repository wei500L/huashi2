export type WordPairType = 'COGNATE' | 'FALSE_FRIEND' | 'PARTIAL' | 'PARONYM';

export interface CognitiveMetrics {
  positiveTransferScore: number; // 0-1
  negativeTransferRisk: number;  // 0-1
  contextSensitivity: number;    // 0-1
  accuracy: number;
  avgResponseTime: number;      // ms
}

export interface TrainingTask {
  id: string;
  title: string;
  type: 'DIAGNOSIS' | 'PRACTICE' | 'REVIEW';
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  estimatedTime: number; // minutes
  description: string;
}

export interface ErrorWordPair {
  id: string;
  en: string;
  fr: string;
  zh: string;
  type: WordPairType;
  errorCount: number;
  lastErrorType: string;
}

export interface DashboardData {
  userProfile: {
    name: string;
    level: string;
    streak: number;
  };
  metrics: CognitiveMetrics;
  weeklyCompletion: number; // percentage
  trends: {
    dates: string[];
    scores: number[];
    rt: number[];
  };
  errorDistribution: { name: string; value: number }[];
  recommendedTasks: TrainingTask[];
  recentErrors: ErrorWordPair[];
}

import { WordPairType, ContextLevel, DiagnosisResult } from './diagnosis';

export interface ClassStats {
  classId: string;
  className: string;
  studentCount: number;
  avgPositiveScore: number;
  avgNegativeRisk: number;
  completionRate: number;
  errorTypeDistribution: Record<string, number>;
}

export interface StudentSummary {
  id: string;
  name: string;
  avatar: string;
  enLevel: string;
  frLevel: string;
  positiveTransferScore: number;
  negativeTransferRisk: number;
  lastActive: string;
  status: 'ACTIVE' | 'WARNING' | 'INACTIVE';
}

export interface AdminWordPair {
  id: string;
  en: string;
  fr: string;
  zh: string;
  type: WordPairType;
  difficulty: 1 | 2 | 3 | 4 | 5;
  semanticSimilarity: number; // 0-1
  contextLevels: ContextLevel[];
  tags: string[];
}

export interface InterventionStrategy {
  id: string;
  studentId: string;
  patternDetected: string;
  suggestedAction: string;
  priority: 'URGENT' | 'NORMAL' | 'LOW';
  applied: boolean;
}

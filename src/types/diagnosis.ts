export type WordPairType = 'COGNATE' | 'FALSE_FRIEND' | 'PARTIAL' | 'ORTHOGRAPHIC';
export type ContextLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface DiagnosisQuestion {
  id: string;
  type: 'RT_JUDGMENT' | 'SEMANTIC_CONTEXT';
  wordPair: {
    en: string;
    fr: string;
    type: WordPairType;
  };
  context?: {
    level: ContextLevel;
    sentence: string; // 可能是低语境（仅单词）或高语境（完整句子）
    options: string[];
    correctIndex: number;
  };
}

export interface AnswerRecord {
  questionId: string;
  isCorrect: boolean;
  responseTime: number; // 毫秒
  timestamp: number;
  wordPairType: WordPairType;
  contextLevel: ContextLevel;
}

export interface DiagnosisResult {
  positiveTransferScore: number;
  negativeTransferRisk: number;
  semanticDiscrimination: number;
  contextSensitivity: number;
  errorDistribution: Record<WordPairType, number>;
  avgRT: number;
  criticalPairs: { en: string; fr: string; reason: string }[];
}

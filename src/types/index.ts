/**
 * 词汇与迁移核心模型
 */

// 1. 词对类型 (Word Pair Type)
export enum WordPairType {
  Cognate = 'COGNATE',               // 同源词 (e.g., table/table)
  FalseFriend = 'FALSE_FRIEND',      // 同形异义词 (e.g., coin/coin)
  PartialOverlapping = 'PARTIAL',    // 部分语义重叠 (e.g., nature/nature)
  Paronym = 'PARONYM'                // 近形词 (e.g., message/massage)
}

// 2. 语言水平 (Language Proficiency)
export type ProficiencyLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2';

export interface Proficiency {
  english: ProficiencyLevel;
  french: ProficiencyLevel;
  compositeScore: number; // 0-100
}

// 3. 认知表现 (Cognitive Performance)
export interface CognitiveMetrics {
  accuracy: number;           // 正确率
  responseTime: number;       // 反应时 (ms)
  hesitationDuration: number; // 犹豫时长 (ms)
  errorType?: 'phonetic' | 'semantic' | 'orthographic' | 'interference' | 'none';
  confusedWord?: string;      // 混淆词
}

// 4. 迁移指标 (Transfer Indicators)
export interface TransferIndicators {
  positiveTransferScore: number; // 正迁移得分 (0-1)
  negativeTransferRisk: number;  // 负迁移风险 (0-1)
  contextSensitivity: number;    // 语境敏感度 (0-1)
  semanticDiscrimination: number; // 语义辨析力 (0-1)
}

// 5. 词汇条目 (Word Entry)
export interface WordPair {
  id: string;
  en: string;
  fr: string;
  zh: string;
  type: WordPairType;
  contextLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  sentenceEn: string;
  sentenceFr: string;
  sentenceZh: string;
}

// 6. 用户画像 (User Profile)
export interface UserProfile {
  id: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  username: string;
  proficiency: Proficiency;
  transferStats: TransferIndicators;
}

// 7. 训练记录 (Training Record)
export interface TrainingRecord {
  id: string;
  timestamp: string;
  type: 'DIAGNOSTIC' | 'PRACTICE' | 'CHALLENGE';
  score: number;
  metrics: CognitiveMetrics;
  wordPairId: string;
}

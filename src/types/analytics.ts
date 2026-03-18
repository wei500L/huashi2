import { WordPairType, ContextLevel } from './diagnosis';

export interface AnalyticsMetrics {
  enLevel: number; // 0-100
  frLevel: number;
  semanticDiscrimination: number;
  contextUsage: number;
  speedAccuracyBalance: number;
  negativeTransferRisk: number;
}

export interface HeatmapData {
  wordType: WordPairType;
  errorType: 'PHONETIC' | 'SEMANTIC' | 'ORTHOGRAPHIC' | 'INTERFERENCE';
  count: number;
}

export interface TrendPoint {
  date: string;
  positiveScore: number;
  negativeRisk: number;
}

export interface ContextPerformance {
  level: ContextLevel;
  accuracy: number;
  avgRT: number;
}

export interface ScatterPoint {
  rt: number;
  accuracy: number;
  frequency: number;
  word: string;
}

export interface AnalyticsReport {
  metrics: AnalyticsMetrics;
  heatmap: HeatmapData[];
  trends: TrendPoint[];
  contextPerformances: ContextPerformance[];
  scatterData: ScatterPoint[];
  topRiskPairs: { en: string; fr: string; riskScore: number; count: number }[];
}

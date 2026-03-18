import { create } from 'zustand';
import { DiagnosisQuestion, AnswerRecord, DiagnosisResult, WordPairType } from '@/types/diagnosis';

interface DiagnosisState {
  questions: DiagnosisQuestion[];
  currentIdx: number;
  records: AnswerRecord[];
  isComplete: boolean;
  result: DiagnosisResult | null;
  
  // Actions
  startDiagnosis: (questions: DiagnosisQuestion[]) => void;
  recordAnswer: (record: AnswerRecord) => void;
  calculateResult: () => void;
  reset: () => void;
}

export const useDiagnosisStore = create<DiagnosisState>((set, get) => ({
  questions: [],
  currentIdx: 0,
  records: [],
  isComplete: false,
  result: null,

  startDiagnosis: (questions) => set({ 
    questions, 
    currentIdx: 0, 
    records: [], 
    isComplete: false, 
    result: null 
  }),

  recordAnswer: (record) => {
    const { questions, currentIdx } = get();
    const newRecords = [...get().records, record];
    const nextIdx = currentIdx + 1;
    
    if (nextIdx >= questions.length) {
      set({ records: newRecords, isComplete: true });
      get().calculateResult();
    } else {
      set({ records: newRecords, currentIdx: nextIdx });
    }
  },

  calculateResult: () => {
    const { records } = get();
    const total = records.length;
    const correct = records.filter(r => r.isCorrect).length;
    
    // 正迁移计算：Cognate 正确且 RT < 1s
    const cognates = records.filter(r => r.wordPairType === 'COGNATE');
    const positiveScore = cognates.length > 0 
      ? cognates.filter(r => r.isCorrect && r.responseTime < 800).length / cognates.length 
      : 0;

    // 负迁移风险：False Friend 错误，或正确但 RT > 1.5s (认知冲突)
    const falseFriends = records.filter(r => r.wordPairType === 'FALSE_FRIEND');
    const negativeRisk = falseFriends.length > 0
      ? falseFriends.filter(r => !r.isCorrect || r.responseTime > 1500).length / falseFriends.length
      : 0;

    // 语境敏感度：高语境相比低语境正确率的提升
    const lowCtx = records.filter(r => r.contextLevel === 'LOW');
    const highCtx = records.filter(r => r.contextLevel === 'HIGH');
    const ctxSens = highCtx.length > 0 && lowCtx.length > 0
      ? (highCtx.filter(r => r.isCorrect).length / highCtx.length) - (lowCtx.filter(r => r.isCorrect).length / lowCtx.length)
      : 0;

    // 错误分布
    const distribution: Record<WordPairType, number> = {
      COGNATE: 0, FALSE_FRIEND: 0, PARTIAL: 0, ORTHOGRAPHIC: 0
    };
    records.forEach(r => { if (!r.isCorrect) distribution[r.wordPairType]++; });

    set({
      result: {
        positiveTransferScore: Number(positiveScore.toFixed(2)),
        negativeTransferRisk: Number(negativeRisk.toFixed(2)),
        semanticDiscrimination: Number((correct / total).toFixed(2)),
        contextSensitivity: Number(Math.max(0, ctxSens).toFixed(2)),
        errorDistribution: distribution,
        avgRT: Math.floor(records.reduce((acc, r) => acc + r.responseTime, 0) / total),
        criticalPairs: falseFriends.filter(r => !r.isCorrect).map(r => ({
          en: 'example', fr: 'exemple', reason: 'False Friend Confusion'
        }))
      }
    });
  },

  reset: () => set({ questions: [], currentIdx: 0, records: [], isComplete: false, result: null })
}));

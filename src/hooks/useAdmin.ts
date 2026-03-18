import { useState, useEffect } from 'react';
import { ClassStats, StudentSummary, AdminWordPair, InterventionStrategy } from '@/types/admin';

export const useAdmin = () => {
  const [classStats, setClassStats] = useState<ClassStats | null>(null);
  const [students, setStudents] = useState<StudentSummary[]>([]);
  const [vocab, setVocab] = useState<AdminWordPair[]>([]);
  const [interventions, setInterventions] = useState<InterventionStrategy[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Mock 班级统计
        setClassStats({
          classId: 'C001',
          className: '2024级法语专业1班',
          studentCount: 32,
          avgPositiveScore: 0.68,
          avgNegativeRisk: 0.42,
          completionRate: 0.85,
          errorTypeDistribution: { 'False Friend Confusion': 45, 'Phonetic Transfer': 25, 'Semantic Overlap': 30 }
        });

        // Mock 学生列表
        setStudents([
          { id: 's1', name: '李华', avatar: '', enLevel: 'B2', frLevel: 'B1', positiveTransferScore: 0.72, negativeTransferRisk: 0.35, lastActive: '2024-03-20', status: 'ACTIVE' },
          { id: 's2', name: '王芳', avatar: '', enLevel: 'C1', frLevel: 'B2', positiveTransferScore: 0.85, negativeTransferRisk: 0.58, lastActive: '2024-03-21', status: 'WARNING' },
          { id: 's3', name: '张强', avatar: '', enLevel: 'B1', frLevel: 'A2', positiveTransferScore: 0.45, negativeTransferRisk: 0.22, lastActive: '2024-03-19', status: 'ACTIVE' },
        ]);

        // Mock 词表
        setVocab([
          { id: 'v1', en: 'coin', fr: 'coin', zh: '硬币/角落', type: 'FALSE_FRIEND', difficulty: 4, semanticSimilarity: 0.1, contextLevels: ['LOW', 'HIGH'], tags: ['High Frequency', 'Business'] },
          { id: 'v2', en: 'table', fr: 'table', zh: '桌子', type: 'COGNATE', difficulty: 1, semanticSimilarity: 0.95, contextLevels: ['LOW'], tags: ['Basic'] },
        ]);

        // Mock 干预建议
        setInterventions([
          { id: 'i1', studentId: 's2', patternDetected: '过度依赖英语语音进行拼写', suggestedAction: '推送拼写负迁移纠偏专项训练', priority: 'URGENT', applied: false },
          { id: 'i2', studentId: 's3', patternDetected: '同形异义词识别延迟显著', suggestedAction: '增加 False Friend 快速决策练习', priority: 'NORMAL', applied: true },
        ]);

      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  return { classStats, students, vocab, interventions, loading };
};

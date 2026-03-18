import { useState, useEffect } from 'react';
import { DashboardData } from '@/types/learning';

export const useDashboard = () => {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // 模拟 API 请求
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        // 模拟 1.2s 的网络延迟以展示 Skeleton 效果
        await new Promise(resolve => setTimeout(resolve, 1200));

        // Mock 真实业务场景下的迁移数据
        const mockData: DashboardData = {
          userProfile: {
            name: "李华",
            level: "ENG-B2 / FRA-B1",
            streak: 15
          },
          metrics: {
            positiveTransferScore: 0.78,
            negativeTransferRisk: 0.32,
            contextSensitivity: 0.65,
            accuracy: 0.88,
            avgResponseTime: 420
          },
          weeklyCompletion: 85,
          trends: {
            dates: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
            scores: [65, 72, 68, 85, 92, 88, 95],
            rt: [520, 480, 490, 440, 420, 430, 410]
          },
          errorDistribution: [
            { name: '同形异义词 (False Friends)', value: 45 },
            { name: '部分语义重叠', value: 25 },
            { name: '近形词混淆', value: 20 },
            { name: '拼写干扰', value: 10 }
          ],
          recommendedTasks: [
            { id: 't1', title: '智能诊断：商务法语高频错词', type: 'DIAGNOSIS', priority: 'HIGH', estimatedTime: 10, description: '评估你在正式语境下对英法同源词的辨析力。' },
            { id: 't2', title: '深度训练：法律法语负迁移纠偏', type: 'PRACTICE', priority: 'MEDIUM', estimatedTime: 15, description: '专注于排除来自英语法律词汇的语义干扰。' },
            { id: 't3', title: '每日复习：已掌握词汇固化', type: 'REVIEW', priority: 'LOW', estimatedTime: 5, description: '维持你的长期记忆曲线。' }
          ],
          recentErrors: [
            { id: 'e1', en: 'coin', fr: 'coin', zh: '硬币 / 角落', type: 'FALSE_FRIEND', errorCount: 3, lastErrorType: '语义误用' },
            { id: 'e2', en: 'actually', fr: 'actuellement', zh: '实际上 / 目前', type: 'FALSE_FRIEND', errorCount: 2, lastErrorType: '时态语义干扰' },
            { id: 'e3', en: 'nature', fr: 'nature', zh: '大自然', type: 'COGNATE', errorCount: 1, lastErrorType: '拼写负迁移' }
          ]
        };

        setData(mockData);
      } catch (err) {
        setError("无法加载仪表盘数据，请检查网络连接。");
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  return { data, loading, error };
};

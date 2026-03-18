import { useState, useEffect } from 'react';
import { AnalyticsReport } from '@/types/analytics';

export const useAnalytics = () => {
  const [report, setReport] = useState<AnalyticsReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchReport = async () => {
      try {
        setLoading(true);
        // 模拟 API 延迟
        await new Promise(resolve => setTimeout(resolve, 1500));

        // Mock 真实学情数据
        const mockReport: AnalyticsReport = {
          metrics: {
            enLevel: 75,
            frLevel: 62,
            semanticDiscrimination: 0.85,
            contextUsage: 0.72,
            speedAccuracyBalance: 0.68,
            negativeTransferRisk: 0.38
          },
          heatmap: [
            { wordType: 'COGNATE', errorType: 'PHONETIC', count: 5 },
            { wordType: 'COGNATE', errorType: 'SEMANTIC', count: 2 },
            { wordType: 'FALSE_FRIEND', errorType: 'SEMANTIC', count: 42 },
            { wordType: 'FALSE_FRIEND', errorType: 'INTERFERENCE', count: 18 },
            { wordType: 'PARTIAL', errorType: 'SEMANTIC', count: 25 },
            { wordType: 'ORTHOGRAPHIC', errorType: 'ORTHOGRAPHIC', count: 32 },
          ],
          trends: [
            { date: '12-01', positiveScore: 0.65, negativeRisk: 0.45 },
            { date: '12-05', positiveScore: 0.68, negativeRisk: 0.42 },
            { date: '12-10', positiveScore: 0.72, negativeRisk: 0.38 },
            { date: '12-15', positiveScore: 0.78, negativeRisk: 0.35 },
            { date: '12-20', positiveScore: 0.82, negativeRisk: 0.32 },
          ],
          contextPerformances: [
            { level: 'LOW', accuracy: 0.62, avgRT: 1450 },
            { level: 'MEDIUM', accuracy: 0.78, avgRT: 1120 },
            { level: 'HIGH', accuracy: 0.92, avgRT: 850 },
          ],
          scatterData: [
            { rt: 450, accuracy: 0.95, frequency: 12, word: 'Table' },
            { rt: 1250, accuracy: 0.45, frequency: 8, word: 'Actually' },
            { rt: 850, accuracy: 0.75, frequency: 15, word: 'Coin' },
            { rt: 1850, accuracy: 0.32, frequency: 5, word: 'Actuellement' },
            { rt: 620, accuracy: 0.88, frequency: 20, word: 'Nature' },
          ],
          topRiskPairs: [
            { en: 'Actually', fr: 'Actuellement', riskScore: 0.92, count: 12 },
            { en: 'Coin', fr: 'Coin', riskScore: 0.85, count: 15 },
            { en: 'Library', fr: 'Librairie', riskScore: 0.78, count: 8 },
            { en: 'Demand', fr: 'Demander', riskScore: 0.72, count: 10 },
          ]
        };

        setReport(mockReport);
      } catch (err) {
        setError("获取学情分析报告失败");
      } finally {
        setLoading(false);
      }
    };

    fetchReport();
  }, []);

  // 数据格式化函数：用于 ECharts 热力图
  const getHeatmapOption = (data: any[]) => {
    const wordTypes = ['COGNATE', 'FALSE_FRIEND', 'PARTIAL', 'ORTHOGRAPHIC'];
    const errorTypes = ['PHONETIC', 'SEMANTIC', 'ORTHOGRAPHIC', 'INTERFERENCE'];
    
    const formattedData = data.map(item => [
      wordTypes.indexOf(item.wordType),
      errorTypes.indexOf(item.errorType),
      item.count
    ]);

    return {
      tooltip: { position: 'top' },
      grid: { height: '70%', top: '10%' },
      xAxis: { type: 'category', data: wordTypes, splitArea: { show: true } },
      yAxis: { type: 'category', data: errorTypes, splitArea: { show: true } },
      visualMap: {
        min: 0, max: 50, calculable: true, orient: 'horizontal', left: 'center', bottom: '0%',
        inRange: { color: ['#f8fafc', '#3b82f6', '#1e3a8a'] }
      },
      series: [{
        name: '错误频次', type: 'heatmap', data: formattedData,
        label: { show: true },
        emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
      }]
    };
  };

  return { report, loading, error, getHeatmapOption };
};

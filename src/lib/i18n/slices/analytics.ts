import type { TranslationSlice } from '../resources';

export const analyticsSlice: TranslationSlice = {
  'zh-CN': {
    dashboard: {
      title: '学习总览',
      subtitle: '实时聚合学生画像、近期诊断信号和训练建议。',
    },
    analytics: {
      title: '学情分析',
      subtitle: '真实聚合后的趋势、热力图、散点图和高风险词对。',
    },
  },
  'en-US': {
    dashboard: {
      title: 'Learning Overview',
      subtitle: 'A real-time summary of the student profile, recent diagnosis signals, and training recommendations.',
    },
    analytics: {
      title: 'Learning Analytics',
      subtitle: 'Trends, heatmaps, scatter plots, and high-risk pairs from real aggregates.',
    },
  },
};

import type { TranslationSlice } from '../resources';

export const analyticsSlice: TranslationSlice = {
  'zh-CN': {
    dashboard: {
      title: '学习总览',
      subtitle: '先查看当前主风险、推荐练习和老师发布的最新任务。',
    },
    analytics: {
      title: '查看高风险词对',
      subtitle: '按趋势、热力图和高风险词对快速定位最近最值得优先处理的问题。',
    },
  },
  'en-US': {
    dashboard: {
      title: 'Learning Overview',
      subtitle: 'Review the primary risk, recommended practice, and the latest teacher-assigned work first.',
    },
    analytics: {
      title: 'Review High-Risk Pairs',
      subtitle: 'Use trends, heatmaps, and high-risk pairs to spot what deserves attention first.',
    },
  },
};

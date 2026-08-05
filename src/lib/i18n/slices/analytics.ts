import type { TranslationSlice } from '../resources';

export const analyticsSlice: TranslationSlice = {
  'zh-CN': {
    dashboard: {
      title: '学习总览',
      subtitle: '先查看当前主风险、推荐练习和老师发布的最新任务。',
      hero: {
        kicker: '语言迁移地图',
        greeting: '{{name}}，今天继续向法语迈一步',
        firstUseTitle: '先画出你的语言迁移路径',
        withDiagnosis: '根据最近诊断，当前优先练习：{{mode}}。完成一个短练习，就能继续推进地图。',
        withoutDiagnosis: '完成一次诊断，系统会把英语基础转成更清晰的法语训练路径。',
        firstUseHint: '首次使用建议先做诊断，约需几分钟。',
      },
      map: {
        title: 'English → Français',
        label: '英语到法语的迁移地图',
        description: '每一次诊断、训练和复习都会点亮一段迁移路径。',
      },
      action: { startPractice: '开始推荐训练', reviewDue: '先复习 {{count}} 项到期内容' },
      metrics: { weakPairs: '高风险词对' },
      stage: {
        eyebrow: '当前阶段',
        title: '从理解到迁移，按这三步走',
        diagnosis: '建立基线',
        practice: '针对性训练',
        transfer: '法语情境迁移',
        current: '现在进行',
        complete: '已完成',
        upNext: '下一步',
      },
      activity: { eyebrow: '最近动态', title: '你刚刚做过什么', empty: '还没有最近活动，先从诊断开始。' },
      recent: { reviewDue: '到期复习' },
      weaknesses: {
        eyebrow: '需要关注',
        title: '最值得先处理的薄弱点',
        description: '风险分数只用于排序，先处理最靠前的一项即可。',
        open: '打开错题与复习',
      },
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
      hero: {
        kicker: 'Language migration map',
        greeting: '{{name}}, take one more step toward French today',
        firstUseTitle: 'Start by mapping your language transfer path',
        withDiagnosis: 'Based on your latest diagnosis, focus on {{mode}}. One short practice keeps the map moving.',
        withoutDiagnosis: 'Complete a diagnosis and we will turn your English baseline into a clearer French practice path.',
        firstUseHint: 'New here? Start with diagnosis; it only takes a few minutes.',
      },
      map: {
        title: 'English → Français',
        label: 'English to French migration map',
        description: 'Each diagnosis, practice, and review lights up another part of your transfer path.',
      },
      action: { startPractice: 'Start recommended practice', reviewDue: 'Review {{count}} due items first' },
      metrics: { weakPairs: 'High-risk pairs' },
      stage: {
        eyebrow: 'Current stage',
        title: 'Move from understanding to transfer in three steps',
        diagnosis: 'Set a baseline',
        practice: 'Targeted practice',
        transfer: 'French context transfer',
        current: 'In progress',
        complete: 'Complete',
        upNext: 'Up next',
      },
      activity: { eyebrow: 'Recent activity', title: 'What you have been doing', empty: 'No recent activity yet. Start with diagnosis.' },
      recent: { reviewDue: 'Review due' },
      weaknesses: {
        eyebrow: 'Needs attention',
        title: 'The next weak points to fix',
        description: 'Risk is only used for ordering. Start with the first item and keep momentum.',
        open: 'Open errors and review',
      },
    },
    analytics: {
      title: 'Review High-Risk Pairs',
      subtitle: 'Use trends, heatmaps, and high-risk pairs to spot what deserves attention first.',
    },
  },
};

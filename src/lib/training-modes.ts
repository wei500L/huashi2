import i18n from './i18n';
import { trainingModeLabel } from './format';

type SupportedLocale = 'zh-CN' | 'en-US';

export interface TrainingModeMeta {
  label: string;
  plainTitle: string;
  purpose: string;
  bestFor: string;
  tooltip: string;
}

const TRAINING_MODE_META: Record<SupportedLocale, Record<string, Omit<TrainingModeMeta, 'label'>>> = {
  'zh-CN': {
    COGNATE_BOOST: {
      plainTitle: '同源词强化',
      purpose: '把已经会的相似词继续练稳，扩大正迁移。',
      bestFor: '适合已经能基本分清词义，但还不够熟练的时候。',
      tooltip: '系统会给你更多“本来就接近”的词对，帮助把正确感觉练成稳定反应。',
    },
    FALSE_FRIEND_DISCRIM: {
      plainTitle: '易混词纠偏',
      purpose: '专门修正常见的同形异义词误判。',
      bestFor: '适合总是被熟悉词形带偏、经常选错义项的时候。',
      tooltip: '这组训练会反复对比最容易混淆的词，帮你先停下来辨义，再作答。',
    },
    CONTEXT_FIX: {
      plainTitle: '语境修正',
      purpose: '训练先看上下文，再决定词义。',
      bestFor: '适合单看词会答，但一进句子就容易判断失误的时候。',
      tooltip: '系统会把词放进句子里，逼你用语境锁定义项，而不是只看词形。',
    },
    SPEED_CHALLENGE: {
      plainTitle: '快速识别',
      purpose: '在保持正确率的前提下压缩反应时间。',
      bestFor: '适合已经能答对大多数题，但速度偏慢的时候。',
      tooltip: '重点不是更难，而是更快更稳，让正确判断变成下意识反应。',
    },
  },
  'en-US': {
    COGNATE_BOOST: {
      plainTitle: 'Cognate Boost',
      purpose: 'Reinforce the word pairs you already partly know and expand positive transfer.',
      bestFor: 'Best when you can usually tell meanings apart but are not fluent yet.',
      tooltip: 'This mode gives you more close cognate pairs so correct choices become automatic.',
    },
    FALSE_FRIEND_DISCRIM: {
      plainTitle: 'False-Friend Fix',
      purpose: 'Correct recurring false-friend and look-alike meaning mistakes.',
      bestFor: 'Best when familiar word forms keep pulling you toward the wrong meaning.',
      tooltip: 'This mode repeatedly contrasts the most confusing pairs so you slow down and disambiguate first.',
    },
    CONTEXT_FIX: {
      plainTitle: 'Context Repair',
      purpose: 'Train you to use the sentence before deciding on meaning.',
      bestFor: 'Best when isolated words feel easy but sentence context still trips you up.',
      tooltip: 'The system places target words in context so you must rely on sentence cues, not just form.',
    },
    SPEED_CHALLENGE: {
      plainTitle: 'Speed Challenge',
      purpose: 'Reduce reaction time without giving up accuracy.',
      bestFor: 'Best when accuracy is acceptable but recognition is still too slow.',
      tooltip: 'The goal is not harder items. It is faster and steadier recognition under time pressure.',
    },
  },
};

function activeLocale(): SupportedLocale {
  return i18n.resolvedLanguage === 'en-US' || i18n.language === 'en-US' ? 'en-US' : 'zh-CN';
}

export function trainingModeMeta(mode?: string | null): TrainingModeMeta {
  const normalized = mode?.trim().toUpperCase() || '';
  const fallback = TRAINING_MODE_META[activeLocale()].FALSE_FRIEND_DISCRIM;
  const copy = TRAINING_MODE_META[activeLocale()][normalized] || fallback;
  return {
    label: trainingModeLabel(mode),
    ...copy,
  };
}

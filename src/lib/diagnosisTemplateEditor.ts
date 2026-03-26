import type {
  DiagnosisTemplateDetailVO,
  DiagnosisTemplateItemRequest,
  DiagnosisTemplateItemVO,
  DiagnosisTemplateScoringProfileRequest,
  DiagnosisTemplateUpsertRequest,
  LexicalPairSummaryVO,
} from './contracts';

export const DIAGNOSIS_TEMPLATE_STATUS_VALUES = ['DRAFT', 'PUBLISHED', 'ARCHIVED'] as const;
export const DIAGNOSIS_TASK_TYPE_VALUES = ['REACTION_TIME', 'SEMANTIC_JUDGEMENT'] as const;
export const CONTEXT_SUPPORT_LEVEL_VALUES = ['LOW', 'MEDIUM', 'HIGH'] as const;

type DiagnosisTemplateStatusValue = (typeof DIAGNOSIS_TEMPLATE_STATUS_VALUES)[number];
type DiagnosisTaskTypeValue = (typeof DIAGNOSIS_TASK_TYPE_VALUES)[number];
type ContextSupportLevelValue = (typeof CONTEXT_SUPPORT_LEVEL_VALUES)[number];

const DIAGNOSIS_TASK_TYPE_ALIASES: Record<string, DiagnosisTaskTypeValue> = {
  REACTION_TIME: 'REACTION_TIME',
  reaction_time: 'REACTION_TIME',
  reaction_time_task: 'REACTION_TIME',
  SEMANTIC_JUDGEMENT: 'SEMANTIC_JUDGEMENT',
  semantic_judgement: 'SEMANTIC_JUDGEMENT',
  semantic_judgement_task: 'SEMANTIC_JUDGEMENT',
};

const CONTEXT_SUPPORT_LEVEL_ALIASES: Record<string, ContextSupportLevelValue> = {
  LOW: 'LOW',
  low: 'LOW',
  MEDIUM: 'MEDIUM',
  medium: 'MEDIUM',
  HIGH: 'HIGH',
  high: 'HIGH',
};

const DIAGNOSIS_TEMPLATE_STATUS_ALIASES: Record<string, DiagnosisTemplateStatusValue> = {
  DRAFT: 'DRAFT',
  draft: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
  published: 'PUBLISHED',
  ARCHIVED: 'ARCHIVED',
  archived: 'ARCHIVED',
};

type JsonObject = Record<string, unknown>;

function expectObject(value: unknown, path: string): JsonObject {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${path} 必须是对象。`);
  }
  return value as JsonObject;
}

function expectString(value: unknown, path: string): string {
  if (typeof value !== 'string' || !value.trim()) {
    throw new Error(`${path} 必须是非空字符串。`);
  }
  return value.trim();
}

function expectOptionalString(value: unknown, path: string): string {
  if (value == null) {
    return '';
  }
  if (typeof value !== 'string') {
    throw new Error(`${path} 必须是字符串。`);
  }
  return value.trim();
}

function expectBoolean(value: unknown, path: string): boolean {
  if (typeof value !== 'boolean') {
    throw new Error(`${path} 必须是布尔值。`);
  }
  return value;
}

function expectNumber(value: unknown, path: string): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`${path} 必须是数字。`);
  }
  return value;
}

function expectOptionalNumber(value: unknown, path: string): number | null {
  if (value == null || value === '') {
    return null;
  }
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`${path} 必须是数字。`);
  }
  return value;
}

function expectOptionalBoolean(value: unknown, path: string): boolean | null {
  if (value == null) {
    return null;
  }
  if (typeof value !== 'boolean') {
    throw new Error(`${path} 必须是布尔值。`);
  }
  return value;
}

function normalizeEnum<T extends string>(value: string, aliases: Record<string, T>, path: string): T {
  const normalized = aliases[value.trim()];
  if (!normalized) {
    throw new Error(`${path} 不受支持：${value}`);
  }
  return normalized;
}

function normalizeScoringProfile(
  value: unknown,
  path: string
): DiagnosisTemplateScoringProfileRequest | null {
  if (value == null) {
    return null;
  }
  const profile = expectObject(value, path);
  const formulaKey = expectOptionalString(profile.formulaKey, `${path}.formulaKey`) || null;
  const pairWeight = expectOptionalNumber(profile.pairWeight, `${path}.pairWeight`);
  const riskAmplifier = expectOptionalNumber(profile.riskAmplifier, `${path}.riskAmplifier`);
  const maxReactionTimeMs = expectOptionalNumber(profile.maxReactionTimeMs, `${path}.maxReactionTimeMs`);

  if (pairWeight !== null && (pairWeight <= 0 || pairWeight > 5)) {
    throw new Error(`${path}.pairWeight 必须在 (0, 5] 范围内。`);
  }
  if (riskAmplifier !== null && (riskAmplifier < 0 || riskAmplifier > 5)) {
    throw new Error(`${path}.riskAmplifier 必须在 [0, 5] 范围内。`);
  }
  if (
    maxReactionTimeMs !== null &&
    (!Number.isInteger(maxReactionTimeMs) || maxReactionTimeMs < 200 || maxReactionTimeMs > 10000)
  ) {
    throw new Error(`${path}.maxReactionTimeMs 必须是 200 到 10000 之间的整数。`);
  }

  if (formulaKey == null && pairWeight == null && riskAmplifier == null && maxReactionTimeMs == null) {
    return null;
  }

  return {
    formulaKey,
    pairWeight,
    riskAmplifier,
    maxReactionTimeMs,
  };
}

function normalizeOption(value: unknown, path: string): DiagnosisTemplateItemRequest['options'][number] {
  const option = expectObject(value, path);
  return {
    key: expectString(option.key, `${path}.key`),
    label: expectString(option.label, `${path}.label`),
    semanticMatch: expectOptionalBoolean(option.semanticMatch, `${path}.semanticMatch`),
    ignoreContextTrap: expectOptionalBoolean(option.ignoreContextTrap, `${path}.ignoreContextTrap`) ?? false,
  };
}

function normalizeStimulus(value: unknown, path: string): DiagnosisTemplateItemRequest['stimulus'] {
  const stimulus = expectObject(value, path);
  return {
    instruction: expectString(stimulus.instruction, `${path}.instruction`),
    contextSentence: expectOptionalString(stimulus.contextSentence, `${path}.contextSentence`),
    promptText: expectOptionalString(stimulus.promptText, `${path}.promptText`),
  };
}

function normalizeTemplateItem(value: unknown, path: string): DiagnosisTemplateItemRequest {
  const item = expectObject(value, path);
  const lexicalPairId = expectNumber(item.lexicalPairId, `${path}.lexicalPairId`);
  const sortOrder = expectNumber(item.sortOrder, `${path}.sortOrder`);
  if (!Number.isInteger(lexicalPairId) || lexicalPairId <= 0) {
    throw new Error(`${path}.lexicalPairId 必须是正整数。`);
  }
  if (!Number.isInteger(sortOrder) || sortOrder <= 0) {
    throw new Error(`${path}.sortOrder 必须是正整数。`);
  }

  const rawOptions = item.options;
  if (!Array.isArray(rawOptions) || rawOptions.length === 0) {
    throw new Error(`${path}.options 必须是非空数组。`);
  }

  return {
    lexicalPairId,
    taskType: normalizeEnum(expectString(item.taskType, `${path}.taskType`), DIAGNOSIS_TASK_TYPE_ALIASES, `${path}.taskType`),
    blockCode: expectString(item.blockCode, `${path}.blockCode`),
    sortOrder,
    contextSupportLevel: normalizeEnum(
      expectString(item.contextSupportLevel, `${path}.contextSupportLevel`),
      CONTEXT_SUPPORT_LEVEL_ALIASES,
      `${path}.contextSupportLevel`
    ),
    expectedSemanticMatch: expectBoolean(item.expectedSemanticMatch, `${path}.expectedSemanticMatch`),
    stimulus: normalizeStimulus(item.stimulus, `${path}.stimulus`),
    options: rawOptions.map((option, index) => normalizeOption(option, `${path}.options[${index}]`)),
    correctAnswerKey: expectString(item.correctAnswerKey, `${path}.correctAnswerKey`),
    scoringProfile: normalizeScoringProfile(item.scoringProfile, `${path}.scoringProfile`),
  };
}

function normalizePersistedItem(item: DiagnosisTemplateItemVO | DiagnosisTemplateItemRequest): DiagnosisTemplateItemRequest {
  return normalizeTemplateItem(item, 'item');
}

export function normalizeTemplateStatus(value: string): DiagnosisTemplateStatusValue {
  return normalizeEnum(value, DIAGNOSIS_TEMPLATE_STATUS_ALIASES, 'status');
}

export function serializeTemplateItems(items: DiagnosisTemplateDetailVO['items'] | DiagnosisTemplateUpsertRequest['items']): string {
  return JSON.stringify(items.map((item) => normalizePersistedItem(item)), null, 2);
}

export function parseTemplateItemsJson(itemsJson: string): DiagnosisTemplateItemRequest[] {
  const parsed = JSON.parse(itemsJson) as unknown;
  if (!Array.isArray(parsed)) {
    throw new Error('items JSON 必须是数组。');
  }
  return parsed.map((item, index) => normalizeTemplateItem(item, `items[${index}]`));
}

function defaultPairWeight(lexicalPairType: string): number {
  switch (lexicalPairType.trim().toUpperCase()) {
    case 'COGNATE':
      return 1;
    case 'PARTIAL_COGNATE':
      return 0.9;
    case 'FALSE_FRIEND':
    case 'ORTHOGRAPHIC_SIMILAR':
      return 0.6;
    default:
      return 1;
  }
}

export function buildTemplateItemFromPair(pair: LexicalPairSummaryVO, sortOrder: number): DiagnosisTemplateItemRequest {
  const expectedSemanticMatch = pair.semanticOverlapScore >= 0.5;
  return {
    lexicalPairId: pair.id,
    taskType: 'REACTION_TIME',
    blockCode: `B${Math.max(1, Math.ceil(sortOrder / 5))}`,
    sortOrder,
    contextSupportLevel: normalizeEnum(pair.defaultContextSupport, CONTEXT_SUPPORT_LEVEL_ALIASES, 'pair.defaultContextSupport'),
    expectedSemanticMatch,
    stimulus: {
      instruction: '结合语境判断英法词义是否一致',
      promptText: `${pair.englishWord} / ${pair.frenchWord}`,
      contextSentence: '',
    },
    options: [
      { key: 'semantic_match', label: '语义一致', semanticMatch: true, ignoreContextTrap: false },
      { key: 'semantic_mismatch', label: '语义不一致', semanticMatch: false, ignoreContextTrap: false },
    ],
    correctAnswerKey: expectedSemanticMatch ? 'semantic_match' : 'semantic_mismatch',
    scoringProfile: {
      formulaKey: 'RULE_V1',
      pairWeight: defaultPairWeight(pair.lexicalPairType),
      riskAmplifier: Number((pair.falseFriendRisk + 1).toFixed(2)),
      maxReactionTimeMs: 1500,
    },
  };
}

export function validateTemplateBeforeSave(
  payload: Pick<DiagnosisTemplateUpsertRequest, 'status' | 'items'>
): string[] {
  const errors: string[] = [];
  const taskTypes = new Set<DiagnosisTaskTypeValue>();
  const contextLevels = new Set<ContextSupportLevelValue>();
  let hasExpectedTrue = false;
  let hasExpectedFalse = false;

  payload.items.forEach((item, index) => {
    const prefix = `第 ${index + 1} 题`;
    taskTypes.add(item.taskType as DiagnosisTaskTypeValue);
    contextLevels.add(item.contextSupportLevel as ContextSupportLevelValue);
    hasExpectedTrue ||= item.expectedSemanticMatch;
    hasExpectedFalse ||= !item.expectedSemanticMatch;

    const optionMap = new Map<string, DiagnosisTemplateItemRequest['options'][number]>();
    item.options.forEach((option) => {
      const key = option.key.trim();
      if (optionMap.has(key)) {
        errors.push(`${prefix} 的 option.key 重复：${key}`);
      } else {
        optionMap.set(key, option);
      }
    });

    const correctOption = optionMap.get(item.correctAnswerKey.trim());
    if (!correctOption) {
      errors.push(`${prefix} 的 correctAnswerKey 未命中任何选项。`);
      return;
    }
    if (correctOption.semanticMatch !== item.expectedSemanticMatch) {
      errors.push(`${prefix} 的正确选项 semanticMatch 必须与 expectedSemanticMatch 一致。`);
    }

    if (item.taskType === 'REACTION_TIME') {
      if (item.options.length !== 2) {
        errors.push(`${prefix} 的 REACTION_TIME 题必须恰好有 2 个选项。`);
      }
      const semanticTrueCount = item.options.filter((option) => option.semanticMatch === true).length;
      const semanticFalseCount = item.options.filter((option) => option.semanticMatch === false).length;
      if (semanticTrueCount !== 1 || semanticFalseCount !== 1) {
        errors.push(`${prefix} 的 REACTION_TIME 题必须包含且只包含 1 个 true 和 1 个 false 语义选项。`);
      }
    }

    if (item.taskType === 'SEMANTIC_JUDGEMENT') {
      if (item.options.length < 2) {
        errors.push(`${prefix} 的 SEMANTIC_JUDGEMENT 题至少需要 2 个选项。`);
      }
      if (item.options.some((option) => option.semanticMatch == null)) {
        errors.push(`${prefix} 的 SEMANTIC_JUDGEMENT 题每个选项都必须提供 semanticMatch。`);
      }
    }
  });

  if (normalizeTemplateStatus(payload.status) !== 'PUBLISHED') {
    return errors;
  }
  if (payload.items.length === 0) {
    errors.push('发布模板至少需要 1 道题。');
  }
  if (!taskTypes.has('REACTION_TIME') || !taskTypes.has('SEMANTIC_JUDGEMENT')) {
    errors.push('发布模板必须同时覆盖 REACTION_TIME 和 SEMANTIC_JUDGEMENT 两种题型。');
  }
  if (!contextLevels.has('LOW') || !contextLevels.has('MEDIUM') || !contextLevels.has('HIGH')) {
    errors.push('发布模板必须覆盖 LOW、MEDIUM、HIGH 三档 contextSupportLevel。');
  }
  if (!hasExpectedTrue || !hasExpectedFalse) {
    errors.push('发布模板必须同时覆盖 expectedSemanticMatch 为 true 和 false 的题目。');
  }
  return errors;
}

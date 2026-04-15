import React from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { AlertTriangle, ArrowLeft, CheckCircle2, Code2, Plus, Trash2 } from 'lucide-react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { PageHeader, SectionEyebrow } from '@/components/common';
import { LexicalPairSuggestionInput } from '@/components/common/LexicalPairSuggestionInput';
import { getApiErrorMessage } from '@/lib/api';
import {
  buildDraftTemplateItemFromPair,
  CONTEXT_SUPPORT_LEVEL_VALUES,
  DIAGNOSIS_TASK_TYPE_VALUES,
  parseTemplateDraftItemsJson,
  serializeTemplateDraftItems,
} from '@/lib/diagnosisTemplateEditor';
import { contextLevelLabel, diagnosisTaskTypeLabel, formatDateTime, lexicalPairTypeLabel } from '@/lib/format';
import type {
  DiagnosisTemplateDraftItemRequest,
  DiagnosisTemplateDraftSchemaRequest,
  DiagnosisTemplateDraftValidationResponseVO,
  LexicalPairDetailVO,
} from '@/lib/contracts';
import { diagnosisTemplateService, lexicalPairService, teacherAnalyticsService } from '@/lib/services';

const steps = [
  { key: 'basic', label: '1. 基本信息' },
  { key: 'items', label: '2. 题项配置' },
  { key: 'pairs', label: '3. 词对选择' },
  { key: 'preview', label: '4. 预览与发布' },
] as const;

function toRequestSchema(detail: NonNullable<Awaited<ReturnType<typeof diagnosisTemplateService.getDraft>>>['schema']): DiagnosisTemplateDraftSchemaRequest {
  return {
    basic: {
      templateName: detail.basic.templateName || '',
      description: detail.basic.description || '',
      publishTarget: detail.basic.publishTarget || 'SELF',
      estimatedDurationMinutes: detail.basic.estimatedDurationMinutes || 10,
      targetClassId: detail.basic.targetClassId ?? null,
      shareScope: detail.basic.shareScope || 'PRIVATE',
      scoringVersion: detail.basic.scoringVersion || 'RULE_V1',
    },
    items: detail.items.map((item) => ({
      draftItemId: item.draftItemId,
      lexicalPairId: item.lexicalPairId || null,
      taskType: item.taskType || 'REACTION_TIME',
      blockCode: item.blockCode || `B${Math.max(1, item.sortOrder || 1)}`,
      sortOrder: item.sortOrder || 1,
      contextSupportLevel: item.contextSupportLevel || 'LOW',
      expectedSemanticMatch: item.expectedSemanticMatch ?? false,
      stimulus: {
        instruction: item.stimulus?.instruction || '',
        contextSentence: item.stimulus?.contextSentence || '',
        promptText: item.stimulus?.promptText || '',
      },
      options: (item.options || []).map((option) => ({
        key: option.key,
        label: option.label,
        semanticMatch: option.semanticMatch ?? null,
        ignoreContextTrap: option.ignoreContextTrap ?? false,
      })),
      correctAnswerKey: item.correctAnswerKey || '',
      scoringProfile: item.scoringProfile
        ? {
            formulaKey: item.scoringProfile.formulaKey || '',
            pairWeight: item.scoringProfile.pairWeight ?? null,
            riskAmplifier: item.scoringProfile.riskAmplifier ?? null,
            maxReactionTimeMs: item.scoringProfile.maxReactionTimeMs ?? null,
          }
        : null,
    })),
  };
}

function applyPairToDraftSchema(schema: DiagnosisTemplateDraftSchemaRequest, pair: Pick<LexicalPairDetailVO, 'id' | 'semanticOverlapScore' | 'defaultContextSupport' | 'englishWord' | 'frenchWord' | 'lexicalPairType' | 'falseFriendRisk'>): DiagnosisTemplateDraftSchemaRequest {
  if (schema.items.some((item) => item.lexicalPairId === pair.id)) {
    return schema;
  }
  return {
    ...schema,
    items: [
      ...schema.items,
      buildDraftTemplateItemFromPair(
        {
          id: pair.id,
          semanticOverlapScore: pair.semanticOverlapScore,
          defaultContextSupport: pair.defaultContextSupport,
          englishWord: pair.englishWord,
          frenchWord: pair.frenchWord,
          lexicalPairType: pair.lexicalPairType,
          falseFriendRisk: pair.falseFriendRisk,
        },
        schema.items.length + 1
      ),
    ],
  };
}

function validationMessageForItem(validation: DiagnosisTemplateDraftValidationResponseVO | null, draftItemId: string): string | null {
  const itemError = validation?.itemErrors.find((item) => item.draftItemId === draftItemId);
  return itemError ? Object.values(itemError.fieldErrors)[0] || null : null;
}

function firstBlockingStep(validation: DiagnosisTemplateDraftValidationResponseVO | null): number {
  if (!validation?.blockingSteps?.length) {
    return 0;
  }
  const order = ['BASIC_INFO', 'ITEM_CONFIGURATION', 'PAIR_SELECTION', 'PREVIEW_PUBLISH'];
  const found = validation.blockingSteps
    .map((step) => order.indexOf(step))
    .filter((index) => index >= 0)
    .sort((left, right) => left - right)[0];
  return found ?? 0;
}

const TemplateDraftEditorPage: React.FC = () => {
  const navigate = useNavigate();
  const { draftId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const resolvedDraftId = Number(draftId);
  const pairId = Number(searchParams.get('pairId'));
  const requestedStep = Math.max(0, Math.min(3, Number(searchParams.get('step') || '1') - 1));
  const [currentStep, setCurrentStep] = React.useState(requestedStep);
  const [schema, setSchema] = React.useState<DiagnosisTemplateDraftSchemaRequest | null>(null);
  const [draftVersion, setDraftVersion] = React.useState<number | null>(null);
  const [selectedItemId, setSelectedItemId] = React.useState<string | null>(null);
  const [pairSearchKeyword, setPairSearchKeyword] = React.useState('');
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [validation, setValidation] = React.useState<DiagnosisTemplateDraftValidationResponseVO | null>(null);
  const [advancedMode, setAdvancedMode] = React.useState(false);
  const [itemsJson, setItemsJson] = React.useState('[]');
  const insertedPairRef = React.useRef<number | null>(null);

  const detailQuery = useQuery({
    queryKey: ['teacher-diagnosis-template-draft', resolvedDraftId],
    queryFn: ({ signal }) => diagnosisTemplateService.getDraft(resolvedDraftId, { signal }),
    enabled: Number.isFinite(resolvedDraftId) && resolvedDraftId > 0,
  });

  const pairByIdQuery = useQuery({
    queryKey: ['teacher-draft-pair-detail', pairId],
    queryFn: ({ signal }) => lexicalPairService.getDetail(pairId, { signal }),
    enabled: Number.isFinite(pairId) && pairId > 0,
  });

  const pairSearchQuery = useQuery({
    queryKey: ['teacher-draft-pair-search', pairSearchKeyword],
    queryFn: ({ signal }) =>
      lexicalPairService.pageQuery(
        { pageNo: 1, pageSize: 8, keyword: pairSearchKeyword.trim() || undefined, active: true },
        { signal }
      ),
    enabled: pairSearchKeyword.trim().length > 0,
  });

  const classOptionsQuery = useQuery({
    queryKey: ['teacher-analytics-classes', 'diagnosis-draft'],
    queryFn: ({ signal }) => teacherAnalyticsService.listClasses({ signal }),
  });

  const updateSchema = React.useCallback((updater: React.SetStateAction<DiagnosisTemplateDraftSchemaRequest | null>) => {
    setValidation(null);
    setSchema(updater);
  }, []);

  const syncStep = React.useCallback(
    (stepIndex: number) => {
      const params = new URLSearchParams(searchParams);
      params.set('step', String(stepIndex + 1));
      setSearchParams(params, { replace: true });
      setCurrentStep(stepIndex);
    },
    [searchParams, setSearchParams]
  );

  React.useEffect(() => {
    if (!detailQuery.data) {
      return;
    }
    const nextSchema = toRequestSchema(detailQuery.data.schema);
    setValidation(null);
    setSchema(nextSchema);
    setDraftVersion(detailQuery.data.version);
    setSelectedItemId(nextSchema.items[0]?.draftItemId || null);
    setItemsJson(serializeTemplateDraftItems(nextSchema.items));
  }, [detailQuery.data]);

  React.useEffect(() => {
    setCurrentStep(requestedStep);
  }, [requestedStep]);

  React.useEffect(() => {
    if (!schema) {
      return;
    }
    setItemsJson(serializeTemplateDraftItems(schema.items));
  }, [schema]);

  const saveMutation = useMutation({
    mutationFn: (nextSchema: DiagnosisTemplateDraftSchemaRequest) =>
      diagnosisTemplateService.saveDraft(resolvedDraftId, {
        version: draftVersion as number,
        schema: nextSchema,
      }),
    onSuccess: (draft) => {
      const nextSchema = toRequestSchema(draft.schema);
      setSchema(nextSchema);
      setDraftVersion(draft.version);
      setSelectedItemId((current) => current && nextSchema.items.some((item) => item.draftItemId === current) ? current : nextSchema.items[0]?.draftItemId || null);
      setItemsJson(serializeTemplateDraftItems(nextSchema.items));
      setFeedback('草稿已保存。');
      setErrorMessage(null);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '草稿保存失败'));
    },
  });

  const validateMutation = useMutation({
    mutationFn: () => diagnosisTemplateService.validateDraft(resolvedDraftId),
    onSuccess: (result) => {
      setValidation(result);
      setFeedback(result.valid ? '当前草稿已通过校验。' : '草稿存在阻塞项，请按步骤修正。');
      setErrorMessage(null);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '模板校验失败'));
    },
  });

  const publishMutation = useMutation({
    mutationFn: () => diagnosisTemplateService.publishDraft(resolvedDraftId),
    onSuccess: (template) => {
      setFeedback(`模板已发布：${template.templateName}`);
      setErrorMessage(null);
      navigate('/teacher/diagnosis-templates');
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(getApiErrorMessage(error, '模板发布失败'));
    },
  });

  const persistSchema = React.useCallback(
    async (nextSchema: DiagnosisTemplateDraftSchemaRequest) => {
      if (draftVersion == null) {
        return;
      }
      await saveMutation.mutateAsync(nextSchema);
    },
    [draftVersion, saveMutation]
  );

  React.useEffect(() => {
    if (!schema || !pairByIdQuery.data || !Number.isFinite(pairId) || pairId <= 0) {
      return;
    }
    if (insertedPairRef.current === pairId) {
      return;
    }
    insertedPairRef.current = pairId;
    if (schema.items.some((item) => item.lexicalPairId === pairId)) {
      return;
    }
    const nextSchema = applyPairToDraftSchema(schema, pairByIdQuery.data);
    updateSchema(nextSchema);
    setSelectedItemId(nextSchema.items[nextSchema.items.length - 1]?.draftItemId || null);
    setCurrentStep(2);
    setFeedback(`已把词对 #${pairId} 插入草稿，请保存并继续配置题项。`);
  }, [pairByIdQuery.data, pairId, schema, updateSchema]);

  const selectedItem = React.useMemo(
    () => schema?.items.find((item) => item.draftItemId === selectedItemId) || null,
    [schema, selectedItemId]
  );

  const updateBasicField = <K extends keyof DiagnosisTemplateDraftSchemaRequest['basic']>(
    key: K,
    value: DiagnosisTemplateDraftSchemaRequest['basic'][K]
  ) => {
    updateSchema((current) => current ? { ...current, basic: { ...current.basic, [key]: value } } : current);
  };

  const updateItem = (draftItemId: string, patch: Partial<DiagnosisTemplateDraftItemRequest>) => {
    updateSchema((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        items: current.items.map((item) => (item.draftItemId === draftItemId ? { ...item, ...patch } : item)),
      };
    });
  };

  const updateOption = (draftItemId: string, optionIndex: number, patch: Partial<DiagnosisTemplateDraftItemRequest['options'][number]>) => {
    updateSchema((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        items: current.items.map((item) =>
          item.draftItemId === draftItemId
            ? {
                ...item,
                options: item.options.map((option, index) => (index === optionIndex ? { ...option, ...patch } : option)),
              }
            : item
        ),
      };
    });
  };

  const addOption = (draftItemId: string) => {
    updateSchema((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        items: current.items.map((item) =>
          item.draftItemId === draftItemId
            ? {
                ...item,
                options: [
                  ...item.options,
                  {
                    key: `option_${item.options.length + 1}`,
                    label: `选项 ${item.options.length + 1}`,
                    semanticMatch: false,
                    ignoreContextTrap: false,
                  },
                ],
              }
            : item
        ),
      };
    });
  };

  const removeOption = (draftItemId: string, optionIndex: number) => {
    updateSchema((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        items: current.items.map((item) =>
          item.draftItemId === draftItemId
            ? {
                ...item,
                options: item.options.filter((_, index) => index !== optionIndex),
              }
            : item
        ),
      };
    });
  };

  const removeItem = (draftItemId: string) => {
    updateSchema((current) => {
      if (!current) {
        return current;
      }
      const nextItems = current.items.filter((item) => item.draftItemId !== draftItemId);
      setSelectedItemId(nextItems[0]?.draftItemId || null);
      return { ...current, items: nextItems };
    });
  };

  const handleApplyJson = () => {
    try {
      const parsed = parseTemplateDraftItemsJson(itemsJson);
      updateSchema((current) => current ? { ...current, items: parsed } : current);
      setSelectedItemId(parsed[0]?.draftItemId || null);
      setFeedback('已从 JSON 同步题项。');
      setErrorMessage(null);
    } catch (error) {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : 'JSON 解析失败');
    }
  };

  const handleAddPair = (pair: LexicalPairDetailVO) => {
    if (!schema) {
      return;
    }
    const nextSchema = applyPairToDraftSchema(schema, pair);
    if (nextSchema === schema) {
      setFeedback(`词对 #${pair.id} 已存在于当前草稿。`);
      return;
    }
    updateSchema(nextSchema);
    setSelectedItemId(nextSchema.items[nextSchema.items.length - 1]?.draftItemId || null);
    setFeedback(`已加入 ${pair.englishWord} / ${pair.frenchWord}。`);
    setErrorMessage(null);
  };

  const isBusy = saveMutation.isPending || validateMutation.isPending || publishMutation.isPending;

  const handleSave = async () => {
    if (!schema) {
      return;
    }
    try {
      await persistSchema(schema);
    } catch {
      return;
    }
  };

  const handleStepChange = async (stepIndex: number) => {
    if (!schema) {
      return;
    }
    try {
      await persistSchema(schema);
      syncStep(stepIndex);
    } catch {
      return;
    }
  };

  const handleValidate = async () => {
    if (!schema) {
      return;
    }
    try {
      await persistSchema(schema);
      const result = await validateMutation.mutateAsync();
      if (!result.valid) {
        syncStep(firstBlockingStep(result));
      }
    } catch {
      return;
    }
  };

  const handlePublish = async () => {
    if (!schema) {
      return;
    }
    try {
      await persistSchema(schema);
      const result = await validateMutation.mutateAsync();
      if (!result.valid) {
        syncStep(firstBlockingStep(result));
        return;
      }
      await publishMutation.mutateAsync();
    } catch {
      return;
    }
  };

  if (detailQuery.isLoading || !schema) {
    return (
      <div className="space-y-8">
        <PageHeader eyebrow="模板草稿" title="模板草稿" subtitle="正在加载草稿与四步编辑上下文..." />
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow="模板草稿"
        title={schema.basic.templateName || `模板草稿 #${resolvedDraftId}`}
        subtitle={`草稿 #${resolvedDraftId} · v${draftVersion || '--'} · 最近更新 ${formatDateTime(detailQuery.data?.updatedAt)} · 当前页负责把草稿推进到可发布状态`}
        actions={
          <div className="flex flex-wrap gap-3">
            <Link
              to="/teacher/diagnosis-templates"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <ArrowLeft size={14} />
              返回模板列表
            </Link>
            <button
              type="button"
              onClick={() => void handleSave()}
              disabled={isBusy}
              className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10 disabled:opacity-60"
            >
              保存草稿
            </button>
            <button
              type="button"
              onClick={() => void handleValidate()}
              disabled={isBusy}
              className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10 disabled:opacity-60"
            >
              校验
            </button>
            <button
              type="button"
              onClick={() => void handlePublish()}
              disabled={isBusy}
              className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
            >
              发布模板
            </button>
          </div>
        }
      />

      <div className="grid gap-3 md:grid-cols-4">
        {steps.map((step, index) => {
          const active = index === currentStep;
          return (
            <button
              key={step.key}
              type="button"
              onClick={() => void handleStepChange(index)}
              disabled={isBusy}
              className={`rounded-[1.6rem] border px-4 py-4 text-left transition ${
                active ? 'border-primary/30 bg-primary/5' : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/[0.03]'
              } disabled:opacity-60`}
            >
              <div className="text-sm font-black text-slate-900 dark:text-white">{step.label}</div>
            </button>
          );
        })}
      </div>

      {feedback && (
        <div className="rounded-[1.8rem] border border-emerald-500/20 bg-emerald-500/5 px-5 py-4 text-sm text-emerald-600 dark:text-emerald-400">
          {feedback}
        </div>
      )}
      {errorMessage && (
        <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
          {errorMessage}
        </div>
      )}

      {validation && !validation.valid && (
        <div className="rounded-[2rem] border border-amber-500/20 bg-amber-500/5 p-5 text-sm text-amber-700 dark:text-amber-300">
          <div className="flex items-center gap-2 font-bold">
            <AlertTriangle size={16} />
            当前草稿存在阻塞项
          </div>
          <div className="mt-3 space-y-2">
            {Object.values(validation.fieldErrors).map((message) => (
              <div key={message}>{message}</div>
            ))}
            {validation.itemErrors.map((item) =>
              Object.values(item.fieldErrors).map((message) => (
                <div key={`${item.draftItemId}-${message}`}>第 {item.itemIndex + 1} 题：{message}</div>
              ))
            )}
          </div>
        </div>
      )}

      {currentStep === 0 && (
        <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">模板名称</div>
              <input
                value={schema.basic.templateName}
                onChange={(event) => updateBasicField('templateName', event.target.value)}
                className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                placeholder="例如：高风险同形异义诊断"
              />
            </label>
            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">预计时长（分钟）</div>
              <input
                type="number"
                min={1}
                value={schema.basic.estimatedDurationMinutes}
                onChange={(event) => updateBasicField('estimatedDurationMinutes', Number(event.target.value) || 1)}
                className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
              />
            </label>
            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">计分版本</div>
              <input
                value={schema.basic.scoringVersion}
                onChange={(event) => updateBasicField('scoringVersion', event.target.value)}
                className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                placeholder="RULE_V1"
              />
            </label>
            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">教师共享</div>
              <select
                value={schema.basic.shareScope || 'PRIVATE'}
                onChange={(event) => updateBasicField('shareScope', event.target.value)}
                className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
              >
                <option value="PRIVATE">私有，仅自己管理</option>
                <option value="PUBLIC">公开到模板市场</option>
              </select>
            </label>
            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">发布目标</div>
              <select
                value={schema.basic.publishTarget || 'SELF'}
                onChange={(event) => {
                  const nextTarget = event.target.value;
                  updateSchema((current) =>
                    current
                      ? {
                          ...current,
                          basic: {
                            ...current.basic,
                            publishTarget: nextTarget,
                            targetClassId: nextTarget === 'CLASS' ? current.basic.targetClassId ?? null : null,
                          },
                        }
                      : current
                  );
                }}
                className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
              >
                <option value="SELF">所有学生可见</option>
                <option value="CLASS">定向班级发布</option>
              </select>
            </label>
          </div>

          {schema.basic.publishTarget === 'CLASS' && (
            <label className="block">
              <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">目标班级</div>
              <select
                value={schema.basic.targetClassId ?? ''}
                onChange={(event) =>
                  updateBasicField('targetClassId', event.target.value ? Number(event.target.value) : null)
                }
                className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
              >
                <option value="">请选择班级</option>
                {(classOptionsQuery.data || []).map((item) => (
                  <option key={item.classId} value={item.classId}>
                    {item.className} ({item.classCode})
                  </option>
                ))}
              </select>
              <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                {classOptionsQuery.isLoading
                  ? '正在加载班级列表...'
                  : !(classOptionsQuery.data || []).length
                    ? '当前账号还没有可用班级，无法做定向发布。'
                    : '学生端只会向所选班级的在班学生展示该模板。'}
              </div>
            </label>
          )}

          <label className="block">
            <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">描述</div>
            <textarea
              value={schema.basic.description || ''}
              onChange={(event) => updateBasicField('description', event.target.value)}
              rows={4}
              className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
              placeholder="说明适用人群、重点风险词对或教学场景。"
            />
          </label>
        </section>
      )}

      {currentStep === 1 && (
        <div className="grid gap-8 xl:grid-cols-[0.82fr_1.18fr]">
          <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-4">
            <div>
              <SectionEyebrow>题项</SectionEyebrow>
              <div className="mt-3 text-xl font-black text-slate-900 dark:text-white">题项清单</div>
            </div>
            {schema.items.map((item, index) => (
              <button
                key={item.draftItemId}
                type="button"
                onClick={() => setSelectedItemId(item.draftItemId)}
                className={`w-full rounded-[1.5rem] border p-4 text-left ${
                  selectedItemId === item.draftItemId
                    ? 'border-primary/30 bg-primary/5'
                    : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/[0.03]'
                }`}
              >
                <div className="font-black text-slate-900 dark:text-white">第 {index + 1} 题</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                  词对 #{item.lexicalPairId || '--'} · {diagnosisTaskTypeLabel(item.taskType)} · {item.blockCode || '--'}
                </div>
                {validationMessageForItem(validation, item.draftItemId) && (
                  <div className="mt-2 text-xs text-amber-600 dark:text-amber-400">
                    {validationMessageForItem(validation, item.draftItemId)}
                  </div>
                )}
              </button>
            ))}
            {!schema.items.length && (
              <div className="rounded-[1.5rem] border border-dashed border-slate-300 bg-white/55 px-4 py-6 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                还没有题项。请先去第 3 步选择词对。
              </div>
            )}
          </section>

          <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
            {selectedItem ? (
              <>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-xl font-black text-slate-900 dark:text-white">编辑题项</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">当前词对 #{selectedItem.lexicalPairId || '--'}，支持逐项配置题型、语境和选项。</div>
                  </div>
                  <button
                    type="button"
                    onClick={() => removeItem(selectedItem.draftItemId)}
                    className="inline-flex items-center gap-2 rounded-2xl border border-rose-500/20 px-4 py-3 text-sm text-rose-500"
                  >
                    <Trash2 size={14} />
                    删除题项
                  </button>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">题型</div>
                    <select
                      value={selectedItem.taskType || 'REACTION_TIME'}
                      onChange={(event) => updateItem(selectedItem.draftItemId, { taskType: event.target.value })}
                      className="native-select w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    >
                      {DIAGNOSIS_TASK_TYPE_VALUES.map((value) => (
                        <option key={value} value={value}>{value}</option>
                      ))}
                    </select>
                  </label>
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">区块编码</div>
                    <input
                      value={selectedItem.blockCode || ''}
                      onChange={(event) => updateItem(selectedItem.draftItemId, { blockCode: event.target.value })}
                      className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">排序</div>
                    <input
                      type="number"
                      min={1}
                      value={selectedItem.sortOrder || 1}
                      onChange={(event) => updateItem(selectedItem.draftItemId, { sortOrder: Number(event.target.value) || 1 })}
                      className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">语境支持</div>
                    <select
                      value={selectedItem.contextSupportLevel || 'LOW'}
                      onChange={(event) => updateItem(selectedItem.draftItemId, { contextSupportLevel: event.target.value })}
                      className="native-select w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    >
                      {CONTEXT_SUPPORT_LEVEL_VALUES.map((value) => (
                        <option key={value} value={value}>{value}</option>
                      ))}
                    </select>
                  </label>
                </div>

                <label className="block">
                  <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">预期语义匹配</div>
                  <select
                    value={selectedItem.expectedSemanticMatch ? 'true' : 'false'}
                    onChange={(event) => updateItem(selectedItem.draftItemId, { expectedSemanticMatch: event.target.value === 'true' })}
                    className="native-select w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                  >
                    <option value="true">语义一致</option>
                    <option value="false">语义不一致</option>
                  </select>
                </label>

                <div className="grid gap-4">
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">作答指令</div>
                    <input
                      value={selectedItem.stimulus.instruction}
                      onChange={(event) => updateItem(selectedItem.draftItemId, { stimulus: { ...selectedItem.stimulus, instruction: event.target.value } })}
                      className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">提示文案</div>
                    <input
                      value={selectedItem.stimulus.promptText || ''}
                      onChange={(event) => updateItem(selectedItem.draftItemId, { stimulus: { ...selectedItem.stimulus, promptText: event.target.value } })}
                      className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">语境句</div>
                    <textarea
                      value={selectedItem.stimulus.contextSentence || ''}
                      onChange={(event) => updateItem(selectedItem.draftItemId, { stimulus: { ...selectedItem.stimulus, contextSentence: event.target.value } })}
                      rows={3}
                      className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                </div>

                <div className="space-y-4 rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="text-lg font-black text-slate-900 dark:text-white">选项配置</div>
                    <button
                      type="button"
                      onClick={() => addOption(selectedItem.draftItemId)}
                      className="rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10"
                    >
                      <Plus size={14} className="inline-block" /> 添加选项
                    </button>
                  </div>

                  {selectedItem.options.map((option, optionIndex) => (
                    <div key={`${selectedItem.draftItemId}-${optionIndex}`} className="grid gap-4 rounded-[1.5rem] border border-slate-200/70 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]">
                      <div className="grid gap-4 md:grid-cols-2">
                        <input
                          value={option.key}
                          onChange={(event) => updateOption(selectedItem.draftItemId, optionIndex, { key: event.target.value })}
                          className="rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                          placeholder="选项键"
                        />
                        <input
                          value={option.label}
                          onChange={(event) => updateOption(selectedItem.draftItemId, optionIndex, { label: event.target.value })}
                          className="rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                          placeholder="选项文案"
                        />
                      </div>
                      <div className="grid gap-4 md:grid-cols-3">
                        <select
                          value={String(option.semanticMatch ?? false)}
                          onChange={(event) => updateOption(selectedItem.draftItemId, optionIndex, { semanticMatch: event.target.value === 'true' })}
                          className="native-select rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                        >
                          <option value="true">语义匹配</option>
                          <option value="false">语义不匹配</option>
                        </select>
                        <select
                          value={String(option.ignoreContextTrap ?? false)}
                          onChange={(event) => updateOption(selectedItem.draftItemId, optionIndex, { ignoreContextTrap: event.target.value === 'true' })}
                          className="native-select rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                        >
                          <option value="false">正常选项</option>
                          <option value="true">忽略语境陷阱</option>
                        </select>
                        <button
                          type="button"
                          onClick={() => removeOption(selectedItem.draftItemId, optionIndex)}
                          className="rounded-2xl border border-rose-500/20 px-4 py-3 text-sm text-rose-500"
                        >
                          删除选项
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">正确答案 key</div>
                    <input
                      value={selectedItem.correctAnswerKey || ''}
                      onChange={(event) => updateItem(selectedItem.draftItemId, { correctAnswerKey: event.target.value })}
                      className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                  <label className="block">
                    <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">计分公式 key</div>
                    <input
                      value={selectedItem.scoringProfile?.formulaKey || ''}
                      onChange={(event) =>
                        updateItem(selectedItem.draftItemId, {
                          scoringProfile: { ...(selectedItem.scoringProfile || {}), formulaKey: event.target.value },
                        })
                      }
                      className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 dark:border-white/10 dark:bg-white/5"
                    />
                  </label>
                </div>
              </>
            ) : (
              <div className="rounded-[1.6rem] border border-dashed border-slate-300 bg-white/55 px-5 py-8 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                当前没有可配置题项。请先去第 3 步选择词对。
              </div>
            )}
          </section>
        </div>
      )}

      {currentStep === 2 && (
        <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-5">
          <div className="grid gap-4 xl:grid-cols-[0.72fr_1.28fr]">
            <div className="space-y-4">
              <div>
                <div className="text-xl font-black text-slate-900 dark:text-white">搜索词对</div>
                <div className="mt-2 text-sm text-slate-500 dark:text-white/45">所有插入动作都基于搜索选择，不再手动记忆 Pair ID。</div>
              </div>
              <LexicalPairSuggestionInput
                value={pairSearchKeyword}
                onChange={setPairSearchKeyword}
                onSuggestionSelect={(suggestion) => setPairSearchKeyword(suggestion.englishWord)}
                active
                placeholder="coin / actuellement / 中文释义 / yingbi / yb"
                inputClassName="bg-white/80 dark:bg-white/5"
              />
              <div className="space-y-3">
                {(pairSearchQuery.data?.records || []).map((pair) => (
                  <button
                    key={pair.id}
                    type="button"
                    onClick={() => handleAddPair(pair as unknown as LexicalPairDetailVO)}
                    className="w-full rounded-[1.5rem] border border-slate-200/70 bg-white/60 p-4 text-left dark:border-white/10 dark:bg-white/[0.03]"
                  >
                    <div className="font-black text-slate-900 dark:text-white">{pair.englishWord} / {pair.frenchWord}</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{pair.chineseGloss}</div>
                    <div className="mt-3 text-xs text-slate-400 dark:text-white/30">{lexicalPairTypeLabel(pair.lexicalPairType)}</div>
                  </button>
                ))}
                {pairSearchKeyword.trim() && !pairSearchQuery.isLoading && !(pairSearchQuery.data?.records || []).length && (
                  <div className="rounded-[1.5rem] border border-dashed border-slate-300 bg-white/55 px-4 py-6 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                    没有找到匹配词对。
                  </div>
                )}
              </div>
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="text-xl font-black text-slate-900 dark:text-white">当前已选词对</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">选中后会自动生成基础题项草稿，可继续去第 2 步细化。</div>
                </div>
                <button
                  type="button"
                  onClick={() => setAdvancedMode((current) => !current)}
                  className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10"
                >
                  <Code2 size={14} />
                  高级 JSON
                </button>
              </div>

              <div className="grid gap-3 md:grid-cols-2">
                {schema.items.map((item, index) => (
                  <button
                    key={item.draftItemId}
                    type="button"
                    onClick={() => {
                      setSelectedItemId(item.draftItemId);
                      setCurrentStep(1);
                      const params = new URLSearchParams(searchParams);
                      params.set('step', '2');
                      setSearchParams(params, { replace: true });
                    }}
                    className="rounded-[1.5rem] border border-slate-200/70 bg-white/60 p-4 text-left dark:border-white/10 dark:bg-white/[0.03]"
                  >
                    <div className="font-black text-slate-900 dark:text-white">第 {index + 1} 题</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      词对 #{item.lexicalPairId || '--'} · {diagnosisTaskTypeLabel(item.taskType)}
                    </div>
                  </button>
                ))}
              </div>

              {advancedMode && (
                <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                  <div className="text-sm font-black text-slate-900 dark:text-white">高级 JSON</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">仅作为兜底入口保留，默认工作流仍以结构化编辑为主。</div>
                  <textarea
                    value={itemsJson}
                    onChange={(event) => setItemsJson(event.target.value)}
                    rows={18}
                    className="mt-4 w-full rounded-2xl border border-slate-200 bg-white/85 px-4 py-3 font-mono text-sm dark:border-white/10 dark:bg-slate-950/45"
                  />
                  <div className="mt-4 flex gap-3">
                    <button type="button" onClick={handleApplyJson} className="rounded-2xl border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
                      从 JSON 应用
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </section>
      )}

      {currentStep === 3 && (
        <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-6">
          <div>
            <div className="text-xl font-black text-slate-900 dark:text-white">预览与发布</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">发布前会执行项级校验；未通过时会自动把你带回第一个阻塞步骤。</div>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="text-sm font-black text-slate-900 dark:text-white">基本信息</div>
              <div className="mt-4 space-y-2 text-sm text-slate-600 dark:text-white/60">
                <div>模板名称：{schema.basic.templateName || '--'}</div>
                <div>描述：{schema.basic.description || '--'}</div>
                <div>预计时长：{schema.basic.estimatedDurationMinutes} 分钟</div>
                <div>教师共享：{schema.basic.shareScope === 'PUBLIC' ? '公开到模板市场' : '私有'}</div>
                <div>计分版本：{schema.basic.scoringVersion}</div>
              </div>
            </div>
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="text-sm font-black text-slate-900 dark:text-white">覆盖摘要</div>
              <div className="mt-4 space-y-2 text-sm text-slate-600 dark:text-white/60">
                <div>题项总数：{schema.items.length}</div>
                <div>题型：{Array.from(new Set(schema.items.map((item) => diagnosisTaskTypeLabel(item.taskType)).filter(Boolean))).join(' / ') || '--'}</div>
                <div>语境：{Array.from(new Set(schema.items.map((item) => contextLevelLabel(item.contextSupportLevel)).filter(Boolean))).join(' / ') || '--'}</div>
                <div>词对数：{Array.from(new Set(schema.items.map((item) => item.lexicalPairId).filter(Boolean))).length}</div>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            {schema.items.map((item, index) => (
              <div key={item.draftItemId} className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="text-lg font-black text-slate-900 dark:text-white">第 {index + 1} 题</div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                      词对 #{item.lexicalPairId || '--'} · {diagnosisTaskTypeLabel(item.taskType)} · {contextLevelLabel(item.contextSupportLevel)}
                    </div>
                  </div>
                  {validationMessageForItem(validation, item.draftItemId) ? (
                    <div className="rounded-full border border-amber-500/20 px-3 py-1 text-xs text-amber-600 dark:text-amber-400">
                      存在阻塞项
                    </div>
                  ) : (
                    <div className="rounded-full border border-emerald-500/20 px-3 py-1 text-xs text-emerald-600 dark:text-emerald-400">
                      <CheckCircle2 size={12} className="mr-1 inline-block" />
                      已配置
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
};

export default TemplateDraftEditorPage;

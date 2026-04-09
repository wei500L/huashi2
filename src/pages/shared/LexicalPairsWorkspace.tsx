import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowRight,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Download,
  ExternalLink,
  Info,
  LoaderCircle,
  Plus,
  Search,
  Trash2,
} from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { useBodyScrollLock, useDialogAccessibility } from '@/lib/a11y';
import { saveBlob } from '@/lib/api';
import { contextLevelLabel, formatDateTime, lexicalPairTypeLabel, userHasCapability } from '@/lib/format';
import type {
  CsvImportTemplateFieldVO,
  LexicalPairDetailVO,
  LexicalPairUpsertRequest,
} from '@/lib/contracts';
import { fieldLabel, translateImportMessage } from '@/lib/lexical-import';
import { adminService, lexicalPairService } from '@/lib/services';
import { useAuthStore } from '@/store';
import LexicalImportCenter from './LexicalImportCenter';

export type LexicalPairsWorkspaceMode = 'teacher' | 'admin';
export type LexicalPairsWorkspaceView = 'all' | 'list' | 'editor' | 'imports';

type ExampleEditorState = {
  sortOrder: number;
  englishExample: string;
  frenchExample: string;
  chineseTranslation: string;
  contextSupportLevel: string;
  source: string;
};

type SenseEditorState = {
  sortOrder: number;
  englishDefinition: string;
  frenchDefinition: string;
  chineseDefinition: string;
  examples: ExampleEditorState[];
};

type PairEditorState = {
  id?: number;
  englishWord: string;
  frenchWord: string;
  chineseGloss: string;
  lexicalPairType: string;
  semanticOverlapScore: number;
  falseFriendRisk: number;
  defaultContextSupport: string;
  difficultyLevel: number;
  notes: string;
  source: string;
  active: boolean;
  tagsCsv: string;
  senses: SenseEditorState[];
};

type FilterState = {
  keyword: string;
  lexicalPairType: string;
  active: 'ALL' | 'ACTIVE' | 'INACTIVE';
  embeddingStatus: 'ALL' | 'PENDING' | 'EMBEDDED' | 'FAILED';
};

const lexicalPairTypeOptions = [
  { value: 'COGNATE', label: '同源词' },
  { value: 'FALSE_FRIEND', label: '同形异义' },
  { value: 'PARTIAL_COGNATE', label: '部分同源' },
  { value: 'ORTHOGRAPHIC_SIMILAR', label: '近形词' },
] as const;

const contextSupportOptions = [
  { value: 'LOW', label: '低语境' },
  { value: 'MEDIUM', label: '中语境' },
  { value: 'HIGH', label: '高语境' },
] as const;

const embeddingStatusOptions = [
  { value: 'ALL', label: '全部向量状态' },
  { value: 'PENDING', label: '待嵌入' },
  { value: 'EMBEDDED', label: '已嵌入' },
  { value: 'FAILED', label: '嵌入失败' },
] as const;

const workspaceMeta: Record<
  LexicalPairsWorkspaceMode,
  {
    title: string;
    subtitle: string;
    guideTitle: string;
    guideDescription: string;
    successHint: string;
  }
> = {
  teacher: {
    title: '词对管理',
    subtitle: '按列表、编辑区和导入中心拆开维护。词对入库后可直接加入模板或词表，不再要求手记 Pair ID。',
    guideTitle: '教师维护指南',
    guideDescription: '先导入词对并抽样核对，再通过“加入模板”或“加入词表”把内容接到教学链路里。',
    successHint: '导入完成后，优先把目标词对接进模板或词表，而不是额外记录 Pair 编号。',
  },
  admin: {
    title: '语料库管理',
    subtitle: '管理员可按列表、编辑区和导入中心拆分运营，并对单个词对发起定向 reindex。',
    guideTitle: '管理员操作指南',
    guideDescription: '先下载模板核对字段，再执行批量导入；导入完成后继续把词对接入模板、词表或定向 reindex。',
    successHint: '导入成功后还需要继续把词对接到产品链路里，单次 reindex 只作为检索同步兜底。',
  },
};

const defaultFilters: FilterState = {
  keyword: '',
  lexicalPairType: 'ALL',
  active: 'ALL',
  embeddingStatus: 'ALL',
};

export function embeddingStatusLabel(value?: string | null): string {
  return embeddingStatusOptions.find((item) => item.value === value)?.label || value || '--';
}

function lexicalPairTypeWorkspaceLabel(value?: string | null): string {
  return lexicalPairTypeOptions.find((item) => item.value === value)?.label || lexicalPairTypeLabel(value);
}

export function collectActiveFilterLabels(mode: LexicalPairsWorkspaceMode, filters: FilterState): string[] {
  const labels: string[] = [];
  const keyword = filters.keyword.trim();
  if (keyword) {
    labels.push(`关键词：${keyword}`);
  }
  if (filters.lexicalPairType !== 'ALL') {
    labels.push(`词对类型：${lexicalPairTypeWorkspaceLabel(filters.lexicalPairType)}`);
  }
  if (filters.active !== 'ALL') {
    labels.push(`启用状态：${filters.active === 'ACTIVE' ? '仅启用' : '仅停用'}`);
  }
  if (mode === 'admin' && filters.embeddingStatus !== 'ALL') {
    labels.push(`向量状态：${embeddingStatusLabel(filters.embeddingStatus)}`);
  }
  return labels;
}

export function describeCsvImportTemplateField(field: CsvImportTemplateFieldVO): {
  key: string;
  label: string;
  description: string;
} {
  return {
    key: field.fieldName,
    label: fieldLabel(field.fieldName),
    description: field.description || '--',
  };
}

function hasText(value?: string | null): boolean {
  return Boolean(value && value.trim());
}

function createExample(sortOrder = 1): ExampleEditorState {
  return {
    sortOrder,
    englishExample: '',
    frenchExample: '',
    chineseTranslation: '',
    contextSupportLevel: 'MEDIUM',
    source: '',
  };
}

function createSense(sortOrder = 1): SenseEditorState {
  return {
    sortOrder,
    englishDefinition: '',
    frenchDefinition: '',
    chineseDefinition: '',
    examples: [createExample(1)],
  };
}

function createEmptyEditor(): PairEditorState {
  return {
    englishWord: '',
    frenchWord: '',
    chineseGloss: '',
    lexicalPairType: 'COGNATE',
    semanticOverlapScore: 0.5,
    falseFriendRisk: 0.1,
    defaultContextSupport: 'LOW',
    difficultyLevel: 3,
    notes: '',
    source: 'teacher_manual',
    active: true,
    tagsCsv: '',
    senses: [],
  };
}

function isBlankEditor(editor: PairEditorState, selectedId: number | null, showStructuredEditor: boolean): boolean {
  return (
    selectedId === null &&
    !showStructuredEditor &&
    !editor.id &&
    editor.englishWord === '' &&
    editor.frenchWord === '' &&
    editor.chineseGloss === '' &&
    editor.lexicalPairType === 'COGNATE' &&
    editor.semanticOverlapScore === 0.5 &&
    editor.falseFriendRisk === 0.1 &&
    editor.defaultContextSupport === 'LOW' &&
    editor.difficultyLevel === 3 &&
    editor.notes === '' &&
    editor.source === 'teacher_manual' &&
    editor.active &&
    editor.tagsCsv === '' &&
    editor.senses.length === 0
  );
}

function toEditor(detail?: LexicalPairDetailVO | null): PairEditorState {
  if (!detail) {
    return createEmptyEditor();
  }
  return {
    id: detail.id,
    englishWord: detail.englishWord,
    frenchWord: detail.frenchWord,
    chineseGloss: detail.chineseGloss,
    lexicalPairType: detail.lexicalPairType,
    semanticOverlapScore: detail.semanticOverlapScore,
    falseFriendRisk: detail.falseFriendRisk,
    defaultContextSupport: detail.defaultContextSupport,
    difficultyLevel: detail.difficultyLevel,
    notes: detail.notes || '',
    source: detail.source || '',
    active: detail.active,
    tagsCsv: detail.tags.join(', '),
    senses:
      detail.senses.length > 0
        ? detail.senses.map((sense, senseIndex) => ({
            sortOrder: sense.sortOrder ?? senseIndex + 1,
            englishDefinition: sense.englishDefinition,
            frenchDefinition: sense.frenchDefinition,
            chineseDefinition: sense.chineseDefinition,
            examples:
              sense.examples.length > 0
                ? sense.examples.map((example, exampleIndex) => ({
                    sortOrder: example.sortOrder ?? exampleIndex + 1,
                    englishExample: example.englishExample,
                    frenchExample: example.frenchExample,
                    chineseTranslation: example.chineseTranslation,
                    contextSupportLevel: example.contextSupportLevel,
                    source: example.source || '',
                  }))
                : [createExample(1)],
          }))
        : [],
  };
}

function buildPayload(editor: PairEditorState): LexicalPairUpsertRequest {
  const senses = editor.senses
    .map((sense, senseIndex) => {
      const examples = sense.examples
        .map((example, exampleIndex) => ({
          sortOrder: Number(example.sortOrder) || exampleIndex + 1,
          englishExample: example.englishExample.trim(),
          frenchExample: example.frenchExample.trim(),
          chineseTranslation: example.chineseTranslation.trim(),
          contextSupportLevel: example.contextSupportLevel,
          source: example.source.trim(),
        }))
        .filter(
          (example) =>
            hasText(example.englishExample) || hasText(example.frenchExample) || hasText(example.chineseTranslation)
        );

      return {
        sortOrder: Number(sense.sortOrder) || senseIndex + 1,
        englishDefinition: sense.englishDefinition.trim(),
        frenchDefinition: sense.frenchDefinition.trim(),
        chineseDefinition: sense.chineseDefinition.trim(),
        examples,
      };
    })
    .filter(
      (sense) =>
        hasText(sense.englishDefinition) ||
        hasText(sense.frenchDefinition) ||
        hasText(sense.chineseDefinition) ||
        sense.examples.length > 0
    );

  return {
    englishWord: editor.englishWord.trim(),
    frenchWord: editor.frenchWord.trim(),
    chineseGloss: editor.chineseGloss.trim(),
    lexicalPairType: editor.lexicalPairType,
    semanticOverlapScore: Number(editor.semanticOverlapScore.toFixed(2)),
    falseFriendRisk: Number(editor.falseFriendRisk.toFixed(2)),
    defaultContextSupport: editor.defaultContextSupport,
    difficultyLevel: editor.difficultyLevel,
    notes: editor.notes.trim(),
    source: editor.source.trim(),
    active: editor.active,
    tags: editor.tagsCsv
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean),
    senses,
  };
}

function validateEditor(editor: PairEditorState): string | null {
  if (!hasText(editor.englishWord) || !hasText(editor.frenchWord) || !hasText(editor.chineseGloss)) {
    return '英语词、法语词和中文释义为必填项。';
  }
  if (editor.semanticOverlapScore < 0 || editor.semanticOverlapScore > 1) {
    return '语义重合度必须在 0 到 1 之间。';
  }
  if (editor.falseFriendRisk < 0 || editor.falseFriendRisk > 1) {
    return '负迁移风险必须在 0 到 1 之间。';
  }
  if (editor.difficultyLevel < 1 || editor.difficultyLevel > 5) {
    return '难度等级必须在 1 到 5 之间。';
  }

  for (const [index, sense] of editor.senses.entries()) {
    const hasDefinition =
      hasText(sense.englishDefinition) || hasText(sense.frenchDefinition) || hasText(sense.chineseDefinition);
    const hasExample = sense.examples.some(
      (example) =>
        hasText(example.englishExample) || hasText(example.frenchExample) || hasText(example.chineseTranslation)
    );
    if (!hasDefinition && hasExample) {
      return `义项 ${index + 1} 已填写例句，但缺少释义。`;
    }
  }
  return null;
}

function riskMeta(score: number): { label: string; className: string } {
  if (score >= 0.75) {
    return { label: 'Critical', className: 'text-rose-500 bg-rose-500/10 border-rose-500/20' };
  }
  if (score >= 0.5) {
    return { label: 'High', className: 'text-orange-500 bg-orange-500/10 border-orange-500/20' };
  }
  if (score >= 0.25) {
    return { label: 'Medium', className: 'text-amber-500 bg-amber-500/10 border-amber-500/20' };
  }
  return { label: 'Low', className: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20' };
}

const SectionCard: React.FC<{
  title: string;
  description?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
}> = ({ title, description, actions, children }) => (
  <section className="min-w-0 rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-6">
    <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div className="space-y-2">
        <h2 className="text-xl font-black text-slate-900 dark:text-white">{title}</h2>
        {description && <p className="max-w-3xl text-sm leading-6 text-slate-500 dark:text-white/45">{description}</p>}
      </div>
      {actions}
    </div>
    {children}
  </section>
);

const FieldCard: React.FC<{ label: string; hint?: string; children: React.ReactNode }> = ({ label, hint, children }) => (
  <label className="block min-w-0 rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-4 py-4 dark:border-white/10 dark:bg-white/[0.03]">
    <div className="mb-3 space-y-2">
      <div className="text-[11px] font-bold uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{label}</div>
      {hint && <div className="text-xs leading-5 text-slate-500 dark:text-white/40">{hint}</div>}
    </div>
    {children}
  </label>
);

const TextInput: React.FC<{
  value: string | number;
  onChange: (value: string) => void;
  type?: 'text' | 'number';
  placeholder?: string;
  step?: string;
  disabled?: boolean;
  inputRef?: React.Ref<HTMLInputElement>;
}> = ({ value, onChange, type = 'text', placeholder, step, disabled, inputRef }) => (
  <input
    ref={inputRef}
    type={type}
    value={value}
    step={step}
    onChange={(event) => onChange(event.target.value)}
    disabled={disabled}
    placeholder={placeholder}
    className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none transition focus:border-primary/40 disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:bg-slate-950/45"
  />
);

const SelectInput: React.FC<{
  value: string | boolean;
  onChange: (value: string) => void;
  disabled?: boolean;
  options: Array<{ value: string; label: string }>;
}> = ({ value, onChange, disabled, options }) => (
  <select
    value={String(value)}
    onChange={(event) => onChange(event.target.value)}
    disabled={disabled}
    className="native-select w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none transition focus:border-primary/40 disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:bg-slate-950/45"
  >
    {options.map((option) => (
      <option key={option.value} value={option.value}>
        {option.label}
      </option>
    ))}
  </select>
);

const RangeField: React.FC<{
  label: string;
  hint: string;
  value: number;
  onChange: (value: number) => void;
}> = ({ label, hint, value, onChange }) => (
  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-4 py-4 dark:border-white/10 dark:bg-white/[0.03]">
    <div className="mb-3 space-y-2">
      <div className="text-[11px] font-bold uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{label}</div>
      <div className="text-xs leading-5 text-slate-500 dark:text-white/40">{hint}</div>
    </div>
    <div className="flex items-center gap-4">
      <input
        type="range"
        min="0"
        max="1"
        step="0.01"
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
        className="h-2 w-full accent-primary"
      />
      <div className="min-w-14 rounded-full border border-slate-200/70 px-3 py-2 text-center text-sm font-bold text-slate-700 dark:border-white/10 dark:text-white/80">
        {value.toFixed(2)}
      </div>
    </div>
  </div>
);

export const LexicalPairsWorkspace: React.FC<{ mode: LexicalPairsWorkspaceMode; view?: LexicalPairsWorkspaceView }> = ({ mode, view = 'all' }) => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const params = useParams<{ lexicalPairId?: string }>();
  const routePairId = params.lexicalPairId ? Number(params.lexicalPairId) : null;
  const user = useAuthStore((state) => state.user);
  const meta = workspaceMeta[mode];
  const canAccessAdminConsole = userHasCapability(user, 'ADMIN_CONSOLE');
  const canAccessTeachingWorkspace = userHasCapability(user, 'TEACHING_WORKSPACE');
  const basePath = mode === 'teacher' ? '/teacher/lexical-pairs' : '/admin/lexical-pairs';
  const showGuideSection = view !== 'editor';
  const showListSection = view === 'all' || view === 'list';
  const showEditorSection = view === 'all' || view === 'editor';
  const showImportSection = view === 'all' || view === 'imports';
  const editorSectionRef = React.useRef<HTMLDivElement | null>(null);
  const englishWordInputRef = React.useRef<HTMLInputElement | null>(null);
  const deleteDialogRef = React.useRef<HTMLDivElement | null>(null);
  const deleteCancelButtonRef = React.useRef<HTMLButtonElement | null>(null);
  const deleteDialogTitleId = React.useId();
  const deleteDialogDescriptionId = React.useId();
  const [filters, setFilters] = React.useState<FilterState>(defaultFilters);
  const deferredKeyword = React.useDeferredValue(filters.keyword);
  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [pageNo, setPageNo] = React.useState(1);
  const [pageSize, setPageSize] = React.useState(20);
  const [editor, setEditor] = React.useState<PairEditorState>(createEmptyEditor);
  const [showStructuredEditor, setShowStructuredEditor] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [resetFeedback, setResetFeedback] = React.useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = React.useState<number | null>(null);
  const focusTimerIdsRef = React.useRef<number[]>([]);
  const closeDeleteDialog = React.useCallback(() => setPendingDeleteId(null), []);
  const focusEnglishWordInput = React.useCallback(() => {
    const input = englishWordInputRef.current;
    if (!input) {
      return;
    }
    input.focus();
    input.select();
  }, []);
  const scheduleEditorFocus = React.useCallback(() => {
    focusTimerIdsRef.current.forEach((timerId) => window.clearTimeout(timerId));
    focusTimerIdsRef.current = [];
    const focusEditor = () => {
      editorSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      focusEnglishWordInput();
    };
    focusEditor();
    focusTimerIdsRef.current.push(window.setTimeout(focusEditor, 0));
    focusTimerIdsRef.current.push(window.setTimeout(focusEditor, 120));
  }, [focusEnglishWordInput]);

  React.useEffect(
    () => () => {
      focusTimerIdsRef.current.forEach((timerId) => window.clearTimeout(timerId));
    },
    []
  );

  useBodyScrollLock(pendingDeleteId !== null);
  useDialogAccessibility({
    open: pendingDeleteId !== null,
    containerRef: deleteDialogRef,
    initialFocusRef: deleteCancelButtonRef,
    onClose: closeDeleteDialog,
  });

  const templateQuery = useQuery({
    queryKey: ['lexical-pair-import-template'],
    queryFn: ({ signal }) => lexicalPairService.getImportTemplate({ signal }),
    staleTime: 5 * 60 * 1000,
  });

  const overviewQuery = useQuery({
    queryKey: ['lexical-pair-overview'],
    queryFn: ({ signal }) => lexicalPairService.getOverview({ signal }),
    enabled: mode === 'admin' || canAccessAdminConsole,
  });

  const listQuery = useQuery({
    queryKey: ['lexical-pairs', deferredKeyword, filters.lexicalPairType, filters.active, filters.embeddingStatus, pageNo, pageSize],
    queryFn: ({ signal }) =>
      lexicalPairService.pageQuery({
        pageNo,
        pageSize,
        keyword: deferredKeyword.trim() || undefined,
        lexicalPairType: filters.lexicalPairType === 'ALL' ? undefined : filters.lexicalPairType,
        active: filters.active === 'ALL' ? undefined : filters.active === 'ACTIVE',
        embeddingStatus: filters.embeddingStatus === 'ALL' ? undefined : filters.embeddingStatus,
      }, { signal }),
  });

  React.useEffect(() => {
    setPageNo(1);
  }, [filters.keyword, filters.lexicalPairType, filters.active, filters.embeddingStatus, pageSize]);

  const detailQuery = useQuery({
    queryKey: ['lexical-pair-detail', selectedId],
    queryFn: ({ signal }) => lexicalPairService.getDetail(selectedId as number, { signal }),
    enabled: selectedId !== null,
  });

  React.useEffect(() => {
    if (!detailQuery.data) {
      return;
    }
    setEditor(toEditor(detailQuery.data));
    setShowStructuredEditor(detailQuery.data.senses.length > 0);
    setError(null);
    setResetFeedback(null);
  }, [detailQuery.data]);

  React.useEffect(() => {
    if (view !== 'editor') {
      return;
    }
    if (routePairId && Number.isFinite(routePairId)) {
      setSelectedId(routePairId);
      return;
    }
    setSelectedId(null);
    setEditor(createEmptyEditor());
    setShowStructuredEditor(false);
  }, [routePairId, view]);

  React.useEffect(() => {
    if (!resetFeedback) {
      return;
    }
    const timer = window.setTimeout(() => setResetFeedback(null), 2500);
    return () => window.clearTimeout(timer);
  }, [resetFeedback]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const validationError = validateEditor(editor);
      if (validationError) {
        throw new Error(validationError);
      }
      const payload = buildPayload(editor);
      if (editor.id) {
        return lexicalPairService.update(editor.id, payload);
      }
      return lexicalPairService.create(payload);
    },
    onSuccess: async (id) => {
      setSelectedId(id);
      setError(null);
      setResetFeedback(null);
      if (view === 'editor') {
        navigate(`${basePath}/${id}/edit`, { replace: true });
      }
      await queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-pair-detail', id] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-pair-overview'] });
    },
    onError: (mutationError) => {
      setError(mutationError instanceof Error ? translateImportMessage(mutationError.message) : '词对保存失败。');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => lexicalPairService.delete(id),
    onSuccess: async () => {
      setPendingDeleteId(null);
      setSelectedId(null);
      setEditor(createEmptyEditor());
      setShowStructuredEditor(false);
      setError(null);
      setResetFeedback(null);
      if (view === 'editor') {
        navigate(basePath, { replace: true });
      }
      await queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-pair-overview'] });
    },
    onError: (mutationError) => {
      setPendingDeleteId(null);
      setError(mutationError instanceof Error ? mutationError.message : '词对删除失败。');
    },
  });

  const reindexMutation = useMutation({
    mutationFn: (lexicalPairId: number) =>
      adminService.triggerRagReindex({
        mode: 'MANUAL',
        sourceTypes: ['LEXICAL_PAIR'],
        sourceIds: [String(lexicalPairId)],
        forceReembed: true,
      }),
    onSuccess: (response, lexicalPairId) => {
      setError(null);
      setResetFeedback(`已提交 Pair #${lexicalPairId} 的 reindex 任务，Job #${response.jobId}。`);
    },
    onError: (mutationError) => {
      setError(mutationError instanceof Error ? mutationError.message : 'reindex 提交失败。');
    },
  });

  const downloadTemplate = async () => {
    const resolvedTemplate =
      templateQuery.data ||
      (await templateQuery.refetch().then((response) => {
        if (!response.data) {
          throw new Error('模板加载失败，请稍后重试。');
        }
        return response.data;
      }));
    if (!resolvedTemplate) {
      throw new Error('模板加载失败，请稍后重试。');
    }
    const template = resolvedTemplate;
    const blob = new Blob([`${template.headerLine}\n${template.exampleLine}\n`], {
      type: 'text/csv;charset=utf-8',
    });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'lexical-pairs-template.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const downloadExport = async () => {
    const blob = await lexicalPairService.exportCsv({
      keyword: deferredKeyword.trim() || undefined,
      lexicalPairType: filters.lexicalPairType === 'ALL' ? undefined : filters.lexicalPairType,
      active: filters.active === 'ALL' ? undefined : filters.active === 'ACTIVE',
      embeddingStatus: filters.embeddingStatus === 'ALL' ? undefined : filters.embeddingStatus,
    });
    saveBlob(blob, `lexical-pairs-${mode}-export.csv`);
  };

  const resetEditor = () => {
    const alreadyBlank = isBlankEditor(editor, selectedId, showStructuredEditor);
    if (view === 'list') {
      navigate(`${basePath}/new`);
      return;
    }
    setSelectedId(null);
    setEditor(createEmptyEditor());
    setShowStructuredEditor(false);
    setError(null);
    setResetFeedback(
      alreadyBlank ? '右侧已是空白新建表单，已定位到英语词输入框。' : '右侧表单已重置，可直接从英语词开始新建。'
    );
    scheduleEditorFocus();
  };

  const updateEditor = <K extends keyof PairEditorState>(key: K, value: PairEditorState[K]) => {
    setEditor((current) => ({ ...current, [key]: value }));
  };

  const updateSense = (index: number, patch: Partial<SenseEditorState>) => {
    setEditor((current) => ({
      ...current,
      senses: current.senses.map((sense, senseIndex) =>
        senseIndex === index
          ? {
              ...sense,
              ...patch,
            }
          : sense
      ),
    }));
  };

  const updateExample = (senseIndex: number, exampleIndex: number, patch: Partial<ExampleEditorState>) => {
    setEditor((current) => ({
      ...current,
      senses: current.senses.map((sense, currentSenseIndex) =>
        currentSenseIndex === senseIndex
          ? {
              ...sense,
              examples: sense.examples.map((example, currentExampleIndex) =>
                currentExampleIndex === exampleIndex
                  ? {
                      ...example,
                      ...patch,
                    }
                  : example
              ),
            }
          : sense
      ),
    }));
  };

  const addSense = () => {
    setShowStructuredEditor(true);
    setEditor((current) => ({
      ...current,
      senses: [...current.senses, createSense(current.senses.length + 1)],
    }));
  };

  const removeSense = (senseIndex: number) => {
    setEditor((current) => {
      const nextSenses = current.senses.filter((_, index) => index !== senseIndex);
      if (nextSenses.length === 0) {
        setShowStructuredEditor(false);
      }
      return {
        ...current,
        senses: nextSenses,
      };
    });
  };

  const addExample = (senseIndex: number) => {
    setEditor((current) => ({
      ...current,
      senses: current.senses.map((sense, index) =>
        index === senseIndex
          ? {
              ...sense,
              examples: [...sense.examples, createExample(sense.examples.length + 1)],
            }
          : sense
      ),
    }));
  };

  const removeExample = (senseIndex: number, exampleIndex: number) => {
    setEditor((current) => ({
      ...current,
      senses: current.senses.map((sense, index) =>
        index === senseIndex
          ? {
              ...sense,
              examples: sense.examples.filter((_, currentExampleIndex) => currentExampleIndex !== exampleIndex),
            }
          : sense
      ),
    }));
  };

  const selectedRisk = riskMeta(editor.falseFriendRisk);
  const structuredExampleCount = React.useMemo(
    () => editor.senses.reduce((total, sense) => total + sense.examples.length, 0),
    [editor.senses]
  );
  const templateFields = templateQuery.data?.fields || [];
  const totalCount = listQuery.data?.total ?? 0;
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  const pageStart = totalCount === 0 ? 0 : (pageNo - 1) * pageSize + 1;
  const pageEnd = Math.min(pageNo * pageSize, totalCount);
  const activeFilterLabels = React.useMemo(() => collectActiveFilterLabels(mode, filters), [mode, filters]);
  const nextStepLinks = React.useMemo(() => {
    const links: Array<{ to: string; label: string; description: string }> = [];
    if (canAccessTeachingWorkspace) {
      links.push({
        to: '/teacher/diagnosis-templates',
        label: '加入模板',
        description: '把词对直接带到模板草稿里，继续完成题项配置。',
      });
      links.push({
        to: '/teacher/lexical-lists',
        label: '加入词表',
        description: '把词对收进词表，后续教学配置和内容组织会更清晰。',
      });
    }
    if (canAccessAdminConsole) {
      links.push({
        to: '/admin/config-center',
        label: '去检查 RAG / Reindex',
        description: '如果本地消息链路或检索结果没有及时更新，可在这里做手动 reindex。',
      });
    }
    return links;
  }, [canAccessAdminConsole, canAccessTeachingWorkspace]);

  React.useEffect(() => {
    if (pageNo > totalPages) {
      setPageNo(totalPages);
    }
  }, [pageNo, totalPages]);

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title={meta.title}
        subtitle={meta.subtitle}
        actions={
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => void downloadTemplate()}
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <Download size={14} />
              下载模板
            </button>
            <button
              type="button"
              onClick={() => void downloadExport()}
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
            >
              <Download size={14} />
              导出 CSV
            </button>
            {canAccessAdminConsole && (
              <Link
                to="/admin/config-center"
                className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
              >
                RAG 运维
                <ExternalLink size={14} />
              </Link>
            )}
            <button type="button" onClick={resetEditor} className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white">
              <Plus size={14} />
              重置并新建词对
            </button>
          </div>
        }
      />

      {mode === 'admin' && (
        <div className="space-y-3">
          {overviewQuery.isLoading && (
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-3 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
              正在加载管理端概览...
            </div>
          )}
          {overviewQuery.error && (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">
              概览加载失败：{overviewQuery.error.message}
            </div>
          )}
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <button
              type="button"
              onClick={() => setFilters((current) => ({ ...current, embeddingStatus: 'ALL' }))}
              className={`rounded-[2rem] border px-5 py-5 text-left ${
                filters.embeddingStatus === 'ALL'
                  ? 'border-primary/25 bg-primary/5'
                  : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/[0.03]'
              }`}
            >
              <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">总词对数</div>
              <div className="mt-2 text-3xl font-black text-slate-900 dark:text-white">
                {overviewQuery.isLoading ? '...' : (overviewQuery.data?.totalCount ?? '--')}
              </div>
              <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
                启用中 {overviewQuery.isLoading ? '...' : (overviewQuery.data?.activeCount ?? '--')} 条
              </div>
            </button>

            <button
              type="button"
              onClick={() => setFilters((current) => ({ ...current, embeddingStatus: 'PENDING' }))}
              className={`rounded-[2rem] border px-5 py-5 text-left ${
                filters.embeddingStatus === 'PENDING'
                  ? 'border-amber-500/25 bg-amber-500/5'
                  : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/[0.03]'
              }`}
            >
              <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">待嵌入</div>
              <div className="mt-2 text-3xl font-black text-amber-600 dark:text-amber-400">
                {overviewQuery.isLoading ? '...' : (overviewQuery.data?.pendingEmbeddingCount ?? '--')}
              </div>
              <div className="mt-3 text-sm text-slate-500 dark:text-white/45">点击后自动筛选待重建词对</div>
            </button>

            <button
              type="button"
              onClick={() => setFilters((current) => ({ ...current, embeddingStatus: 'FAILED' }))}
              className={`rounded-[2rem] border px-5 py-5 text-left ${
                filters.embeddingStatus === 'FAILED'
                  ? 'border-rose-500/25 bg-rose-500/5'
                  : 'border-slate-200/70 bg-white/60 dark:border-white/10 dark:bg-white/[0.03]'
              }`}
            >
              <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">嵌入失败</div>
              <div className="mt-2 text-3xl font-black text-rose-500">
                {overviewQuery.isLoading ? '...' : (overviewQuery.data?.failedEmbeddingCount ?? '--')}
              </div>
              <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
                已嵌入 {overviewQuery.isLoading ? '...' : (overviewQuery.data?.embeddedCount ?? '--')} 条
              </div>
            </button>

            <div className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">最近更新时间</div>
              <div className="mt-3 text-base font-black text-slate-900 dark:text-white">
                {overviewQuery.isLoading ? '正在加载...' : formatDateTime(overviewQuery.data?.latestUpdatedAt)}
              </div>
              <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
                {overviewQuery.isLoading
                  ? '最近新增 / 最近嵌入时间加载中...'
                  : `最近新增 ${formatDateTime(overviewQuery.data?.latestCreatedAt)} · 最近嵌入 ${formatDateTime(overviewQuery.data?.latestEmbeddedAt)}`}
              </div>
            </div>
          </div>
        </div>
      )}

      {showGuideSection && (
      <SectionCard
        title={meta.guideTitle}
        description={meta.guideDescription}
        actions={
          <div className="rounded-[1.4rem] border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-600 dark:text-emerald-400">
            {meta.successHint}
          </div>
        }
      >
        <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
          <div className="grid gap-4 md:grid-cols-2">
            {[
              '1. 下载模板并核对必填列。',
              '2. 优先填写基础字段，义项和例句按需补充。',
              '3. 导入后先处理失败行，确认可导入行都已转为 READY。',
              '4. 导入完成后继续使用“加入模板”“加入词表”或定向 reindex，别停留在词库层。',
            ].map((step) => (
              <div
                key={step}
                className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-5 text-sm leading-6 text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60"
              >
                {step}
              </div>
            ))}
          </div>

          <div className="space-y-4">
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="mb-3 flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white">
                <Info size={16} className="text-primary" />
                常见导入错误
              </div>
              <div className="space-y-2 text-sm leading-6 text-slate-500 dark:text-white/45">
                <div>字段名必须与模板首行完全一致，不能随意改列名。</div>
                <div>`semantic_overlap_score` 和 `false_friend_risk` 只能填 0 到 1。</div>
                <div>`difficulty_level` 只能填 1 到 5，`active` 只能填 true 或 false。</div>
                <div>如果填写了例句列，必须同时填写对应义项列。</div>
              </div>
            </div>

            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="mb-3 flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white">
                <ArrowRight size={16} className="text-primary" />
                导入后下一步
              </div>
              <div className="space-y-3 text-sm leading-6 text-slate-500 dark:text-white/45">
                <div>导入成功后，数据会先进入词对库，不会自动出现在学生端题目里。</div>
                {nextStepLinks.map((link) => (
                  <Link
                    key={link.to}
                    to={link.to}
                    className="block rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-4 transition hover:border-primary/20 hover:bg-primary/5 dark:border-white/10 dark:bg-white/[0.03]"
                  >
                    <div className="inline-flex items-center gap-2 font-bold text-slate-900 dark:text-white">
                      {link.label}
                      <ArrowRight size={14} className="text-primary" />
                    </div>
                    <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{link.description}</div>
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </div>

        <details className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
          <summary className="cursor-pointer list-none text-sm font-bold text-slate-900 dark:text-white">
            查看模板字段说明
          </summary>
          <div className="mt-5 overflow-x-auto">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                <tr>
                  <th className="pb-3">字段</th>
                  <th className="pb-3">是否必填</th>
                  <th className="pb-3">说明</th>
                  <th className="pb-3">示例</th>
                </tr>
              </thead>
              <tbody>
                {templateFields.map((field: CsvImportTemplateFieldVO) => {
                  const display = describeCsvImportTemplateField(field);
                  return (
                    <tr key={display.key} className="border-t border-slate-200/70 dark:border-white/10">
                      <td className="py-3 font-semibold text-slate-900 dark:text-white">
                        {display.label}
                        <div className="mt-1 text-xs font-normal text-slate-400 dark:text-white/30">
                          {display.key}
                        </div>
                      </td>
                      <td className="py-3 text-slate-500 dark:text-white/50">{field.required ? '必填' : '选填'}</td>
                      <td className="py-3 text-slate-500 dark:text-white/50">{display.description}</td>
                      <td className="py-3 text-slate-500 dark:text-white/50">{field.example || '--'}</td>
                    </tr>
                  );
                })}
                {templateQuery.isLoading && (
                  <tr>
                    <td colSpan={4} className="py-4 text-slate-500 dark:text-white/45">
                      模板字段加载中...
                    </td>
                  </tr>
                )}
                {templateQuery.error && (
                  <tr>
                    <td colSpan={4} className="py-4 text-rose-500">
                      模板字段加载失败：{templateQuery.error.message}
                    </td>
                  </tr>
                )}
                {!templateQuery.isLoading && !templateQuery.error && templateFields.length === 0 && (
                  <tr>
                    <td colSpan={4} className="py-4 text-slate-500 dark:text-white/45">
                      当前没有可展示的模板字段说明。
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </details>
      </SectionCard>
      )}

      <div className={`grid items-start gap-8 ${showListSection && showEditorSection ? 'xl:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)]' : ''}`}>
        {showListSection && (
        <div className="min-w-0 space-y-8">
          <SectionCard
            title="词对列表"
            description="支持关键词、词对类型、启用状态和向量状态过滤，并直接把词对送到模板、词表或 reindex。"
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <FieldCard label="关键词检索" hint="按英语词、法语词、中文释义或 searchable text 模糊查询。">
                <div className="relative">
                  <Search size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 dark:text-white/30" />
                  <input
                    value={filters.keyword}
                    onChange={(event) => setFilters((current) => ({ ...current, keyword: event.target.value }))}
                    placeholder="coin / faux ami / 中文释义"
                    className="w-full rounded-2xl border border-slate-200 bg-white/80 py-3 pl-11 pr-4 text-sm outline-none focus:border-primary/40 dark:border-white/10 dark:bg-slate-950/45"
                  />
                </div>
              </FieldCard>
              <FieldCard label="词对类型">
                <SelectInput
                  value={filters.lexicalPairType}
                  onChange={(value) => setFilters((current) => ({ ...current, lexicalPairType: value }))}
                  options={[{ value: 'ALL', label: '全部类型' }, ...lexicalPairTypeOptions]}
                />
              </FieldCard>
              <FieldCard label="启用状态">
                <SelectInput
                  value={filters.active}
                  onChange={(value) =>
                    setFilters((current) => ({
                      ...current,
                      active: value as FilterState['active'],
                    }))
                  }
                  options={[
                    { value: 'ALL', label: '全部状态' },
                    { value: 'ACTIVE', label: '仅启用' },
                    { value: 'INACTIVE', label: '仅停用' },
                  ]}
                />
              </FieldCard>
              {mode === 'admin' && (
                <FieldCard label="向量状态" hint="用于快速定位待嵌入或嵌入失败的词对。">
                  <SelectInput
                    value={filters.embeddingStatus}
                    onChange={(value) =>
                      setFilters((current) => ({
                        ...current,
                        embeddingStatus: value as FilterState['embeddingStatus'],
                      }))
                    }
                    options={[...embeddingStatusOptions]}
                  />
                </FieldCard>
              )}
            </div>

            {activeFilterLabels.length > 0 && (
              <div className="flex flex-wrap items-center gap-2">
                {activeFilterLabels.map((label) => (
                  <span
                    key={label}
                    className="rounded-full border border-slate-200/70 bg-white/70 px-3 py-1 text-xs text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60"
                  >
                    {label}
                  </span>
                ))}
                {mode === 'admin' && filters.embeddingStatus !== 'ALL' && (
                  <button
                    type="button"
                    onClick={() => setFilters((current) => ({ ...current, embeddingStatus: 'ALL' }))}
                    className="rounded-full border border-amber-500/20 px-3 py-1 text-xs text-amber-600 dark:text-amber-400"
                  >
                    清空向量筛选
                  </button>
                )}
              </div>
            )}

            <div className="flex flex-wrap items-center gap-3">
              <div className="rounded-full border border-slate-200/70 bg-white/70 px-4 py-2 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                共 {totalCount} 条词对
              </div>
              <div className="rounded-full border border-slate-200/70 bg-white/70 px-4 py-2 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                当前显示 {pageStart}-{pageEnd}
              </div>
              <div className="rounded-full border border-slate-200/70 bg-white/70 px-2 py-1 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                <select
                  value={pageSize}
                  onChange={(event) => setPageSize(Number(event.target.value))}
                  className="native-select bg-transparent px-2 py-1 outline-none"
                >
                  {[20, 50, 100].map((value) => (
                    <option key={value} value={value}>
                      每页 {value} 条
                    </option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                onClick={() => setFilters(defaultFilters)}
                className="rounded-full border border-slate-200/70 px-4 py-2 text-sm text-slate-500 dark:border-white/10 dark:text-white/45"
              >
                重置筛选
              </button>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 rounded-[1.6rem] border border-slate-200/70 bg-white/55 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
              <div>
                第 {pageNo} / {totalPages} 页
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setPageNo((current) => Math.max(1, current - 1))}
                  disabled={pageNo <= 1 || listQuery.isFetching}
                  className="rounded-full border border-slate-200/70 px-4 py-2 disabled:opacity-40 dark:border-white/10"
                >
                  上一页
                </button>
                <button
                  type="button"
                  onClick={() => setPageNo((current) => Math.min(totalPages, current + 1))}
                  disabled={pageNo >= totalPages || listQuery.isFetching}
                  className="rounded-full border border-slate-200/70 px-4 py-2 disabled:opacity-40 dark:border-white/10"
                >
                  下一页
                </button>
              </div>
            </div>

            <div className="max-h-[720px] space-y-4 overflow-y-auto pr-1">
              {listQuery.isLoading && (
                <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-6 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                  正在加载词对列表...
                </div>
              )}

              {listQuery.error && (
                <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-6 text-sm text-rose-500">
                  {listQuery.error.message}
                </div>
              )}

              {!listQuery.isLoading && !listQuery.error && !(listQuery.data?.records || []).length && (
                <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-6 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                  <div>当前筛选条件下没有词对记录。</div>
                  {activeFilterLabels.length > 0 && (
                    <div className="mt-3 flex flex-wrap items-center gap-2 text-xs">
                      <span>已启用筛选：{activeFilterLabels.join(' / ')}</span>
                      {mode === 'admin' && filters.embeddingStatus !== 'ALL' && (
                        <button
                          type="button"
                          onClick={() => setFilters((current) => ({ ...current, embeddingStatus: 'ALL' }))}
                          className="rounded-full border border-amber-500/20 px-3 py-1 text-amber-600 dark:text-amber-400"
                        >
                          清空向量筛选
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => setFilters(defaultFilters)}
                        className="rounded-full border border-slate-200/70 px-3 py-1 text-slate-500 dark:border-white/10 dark:text-white/45"
                      >
                        重置筛选
                      </button>
                    </div>
                  )}
                </div>
              )}

              {(listQuery.data?.records || []).map((item) => {
                const itemRisk = riskMeta(item.falseFriendRisk);
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => {
                      if (view === 'list') {
                        navigate(`${basePath}/${item.id}/edit`);
                        return;
                      }
                      setSelectedId(item.id);
                    }}
                    className={`w-full rounded-[1.8rem] border p-5 text-left transition ${
                      selectedId === item.id
                        ? 'border-primary/30 bg-primary/5'
                        : 'border-slate-200/70 bg-white/60 hover:border-primary/20 dark:border-white/10 dark:bg-white/[0.03]'
                    }`}
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <div className="text-lg font-black text-slate-900 dark:text-white">
                          {item.englishWord} / {item.frenchWord}
                        </div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{item.chineseGloss}</div>
                      </div>
                      <div className={`rounded-full border px-3 py-1 text-xs font-bold ${itemRisk.className}`}>{itemRisk.label}</div>
                    </div>
                    <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-white/45">
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                        Pair #{item.id}
                      </span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                        {lexicalPairTypeWorkspaceLabel(item.lexicalPairType)}
                      </span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                        {contextLevelLabel(item.defaultContextSupport)}
                      </span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                        难度 {item.difficultyLevel}
                      </span>
                      <span className="rounded-full border border-slate-200/70 px-3 py-1 dark:border-white/10">
                        {item.active ? '启用中' : '已停用'}
                      </span>
                    </div>
                    <div className="mt-3 text-xs text-slate-400 dark:text-white/30">
                      向量状态 {item.embeddingStatus || '--'} · 最近嵌入 {formatDateTime(item.lastEmbeddedAt)}
                    </div>
                    <div className="mt-4 flex flex-wrap gap-2">
                      {mode === 'teacher' && (
                        <>
                          <Link
                            to={`/teacher/diagnosis-templates?pairId=${item.id}`}
                            className="rounded-full border border-slate-200/70 px-3 py-2 text-xs text-slate-600 dark:border-white/10 dark:text-white/60"
                            onClick={(event) => event.stopPropagation()}
                          >
                            加入模板
                          </Link>
                          <Link
                            to={`/teacher/lexical-lists?pairId=${item.id}`}
                            className="rounded-full border border-slate-200/70 px-3 py-2 text-xs text-slate-600 dark:border-white/10 dark:text-white/60"
                            onClick={(event) => event.stopPropagation()}
                          >
                            加入词表
                          </Link>
                        </>
                      )}
                      {mode === 'admin' && canAccessAdminConsole && (
                        <button
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            reindexMutation.mutate(item.id);
                          }}
                          className="rounded-full border border-slate-200/70 px-3 py-2 text-xs text-slate-600 dark:border-white/10 dark:text-white/60"
                        >
                          重新索引
                        </button>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </SectionCard>

          {showImportSection && <LexicalImportCenter mode={mode} />}
        </div>
        )}

        {!showListSection && showImportSection && (
          <div className="min-w-0">
            <LexicalImportCenter mode={mode} />
          </div>
        )}

        {showEditorSection && (
        <div ref={editorSectionRef} className="scroll-mt-24">
          <SectionCard
            title={editor.id ? `编辑词对 #${editor.id}` : '新建词对'}
            description="基础信息优先填写，义项和例句按卡片结构维护。空白义项或空白例句会在保存时自动忽略。"
            actions={
              <div className="flex flex-wrap items-center gap-3">
                {detailQuery.isFetching ? (
                  <div className="inline-flex items-center gap-2 rounded-full border border-slate-200/70 px-4 py-2 text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
                    <LoaderCircle size={14} className="animate-spin" />
                    正在加载详情
                  </div>
                ) : (
                  <div className={`rounded-full border px-4 py-2 text-sm font-bold ${selectedRisk.className}`}>
                    当前风险 {selectedRisk.label}
                  </div>
                )}
                {resetFeedback && (
                  <div
                    aria-live="polite"
                    className="rounded-full border border-sky-500/20 bg-sky-500/5 px-4 py-2 text-sm text-sky-700 dark:text-sky-300"
                  >
                    {resetFeedback}
                  </div>
                )}
              </div>
            }
          >
          <div className="grid gap-4 md:grid-cols-2">
            <FieldCard label="英语词" hint="如 coin。用于训练展示和去重。">
              <TextInput
                inputRef={englishWordInputRef}
                value={editor.englishWord}
                onChange={(value) => updateEditor('englishWord', value)}
                placeholder="coin"
              />
            </FieldCard>
            <FieldCard label="法语词" hint="如 coin。与英语词组成唯一词对。">
              <TextInput value={editor.frenchWord} onChange={(value) => updateEditor('frenchWord', value)} placeholder="coin" />
            </FieldCard>
          </div>

          <FieldCard label="中文释义" hint="面向运营和教师，建议用分号区分多义。">
            <TextInput value={editor.chineseGloss} onChange={(value) => updateEditor('chineseGloss', value)} placeholder="硬币；角落" />
          </FieldCard>

          <div className="grid gap-4 sm:grid-cols-2">
            <FieldCard label="词对类型" hint="建议使用下拉，不要手动拼写枚举值。">
              <SelectInput
                value={editor.lexicalPairType}
                onChange={(value) => updateEditor('lexicalPairType', value)}
                options={lexicalPairTypeOptions.map((item) => ({ value: item.value, label: item.label }))}
              />
            </FieldCard>
            <FieldCard label="默认语境支持" hint="控制训练题初始上下文强度。">
              <SelectInput
                value={editor.defaultContextSupport}
                onChange={(value) => updateEditor('defaultContextSupport', value)}
                options={contextSupportOptions.map((item) => ({ value: item.value, label: item.label }))}
              />
            </FieldCard>
            <FieldCard label="难度等级" hint="1 最易，5 最难。">
              <SelectInput
                value={String(editor.difficultyLevel)}
                onChange={(value) => updateEditor('difficultyLevel', Number(value))}
                options={[1, 2, 3, 4, 5].map((value) => ({ value: String(value), label: `Level ${value}` }))}
              />
            </FieldCard>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <RangeField
              label="语义重合度"
              hint="越接近 1，表示英语词与法语词在义项上越接近。"
              value={editor.semanticOverlapScore}
              onChange={(value) => updateEditor('semanticOverlapScore', value)}
            />
            <RangeField
              label="负迁移风险"
              hint="越接近 1，表示越容易误判，适合优先进入高风险训练。"
              value={editor.falseFriendRisk}
              onChange={(value) => updateEditor('falseFriendRisk', value)}
            />
          </div>

          <details className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
            <summary className="cursor-pointer list-none text-sm font-bold text-slate-900 dark:text-white">
              展开高级字段
            </summary>
            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <FieldCard label="来源" hint="用于标注教材、运营批次或人工维护来源。">
                <TextInput value={editor.source} onChange={(value) => updateEditor('source', value)} placeholder="teacher_manual" />
              </FieldCard>
              <FieldCard label="标签" hint="多个标签请用英文逗号分隔。">
                <TextInput value={editor.tagsCsv} onChange={(value) => updateEditor('tagsCsv', value)} placeholder="false-friend, high-frequency" />
              </FieldCard>
              <FieldCard label="启用状态" hint="停用后不会从数据中删除，但可避免继续参与训练。">
                <SelectInput
                  value={editor.active}
                  onChange={(value) => updateEditor('active', value === 'true')}
                  options={[
                    { value: 'true', label: '启用' },
                    { value: 'false', label: '停用' },
                  ]}
                />
              </FieldCard>
              <FieldCard label="备注" hint="写给运营或教师，便于后续复核。">
                <textarea
                  value={editor.notes}
                  onChange={(event) => updateEditor('notes', event.target.value)}
                  rows={4}
                  className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none focus:border-primary/40 dark:border-white/10 dark:bg-slate-950/45"
                  placeholder="High confusion for beginners"
                />
              </FieldCard>
            </div>
          </details>

          <div className="space-y-5">
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="text-lg font-black text-slate-900 dark:text-white">义项与例句</div>
                  <div className="mt-1 text-sm text-slate-500 dark:text-white/45">
                    默认只显示基础字段。需要补结构化释义时再展开，减少空白噪音。
                  </div>
                </div>
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => setShowStructuredEditor((current) => !current)}
                    className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
                  >
                    {showStructuredEditor ? (
                      <>
                        <ChevronUp size={14} className="inline-block" /> 收起结构化区
                      </>
                    ) : (
                      <>
                        <ChevronDown size={14} className="inline-block" /> 展开结构化区
                      </>
                    )}
                  </button>
                  <button type="button" onClick={addSense} className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10">
                    <Plus size={14} className="inline-block" /> 添加义项
                  </button>
                </div>
              </div>

              {!showStructuredEditor && (
                <div className="mt-5 rounded-[1.5rem] border border-dashed border-slate-300 bg-white/55 px-5 py-6 text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                  {editor.senses.length > 0 ? (
                    <div className="flex flex-wrap items-center gap-3">
                      <span>当前已录入 {editor.senses.length} 个义项</span>
                      <span>共 {structuredExampleCount} 条例句</span>
                      <span>点击“展开结构化区”可继续编辑。</span>
                    </div>
                  ) : (
                    <div>当前只录入基础词对。若需补充释义或例句，再展开结构化区即可。</div>
                  )}
                </div>
              )}
            </div>

            {showStructuredEditor && editor.senses.length === 0 && (
              <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/50 px-5 py-8 text-center text-sm text-slate-500 dark:border-white/15 dark:bg-white/[0.02] dark:text-white/45">
                当前没有义项。可以直接保存基础词对，或添加至少一个义项和例句。
              </div>
            )}

            {showStructuredEditor && editor.senses.map((sense, senseIndex) => (
              <div key={`sense-${senseIndex}`} className="space-y-4 rounded-[2rem] border border-slate-200/70 bg-white/55 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-black text-slate-900 dark:text-white">义项 {senseIndex + 1}</div>
                    <div className="text-xs text-slate-500 dark:text-white/40">
                      至少填写一种释义；如有例句但没有释义，保存会被拦截。
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => removeSense(senseIndex)}
                    className="inline-flex items-center gap-2 rounded-full border border-rose-500/20 px-3 py-2 text-sm text-rose-500"
                  >
                    <Trash2 size={14} />
                    删除义项
                  </button>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <FieldCard label="Sort Order">
                    <TextInput
                      type="number"
                      value={sense.sortOrder}
                      onChange={(value) => updateSense(senseIndex, { sortOrder: Number(value) || sense.sortOrder })}
                    />
                  </FieldCard>
                  <FieldCard label="英语释义">
                    <TextInput
                      value={sense.englishDefinition}
                      onChange={(value) => updateSense(senseIndex, { englishDefinition: value })}
                      placeholder="a piece of money"
                    />
                  </FieldCard>
                  <FieldCard label="法语释义">
                    <TextInput
                      value={sense.frenchDefinition}
                      onChange={(value) => updateSense(senseIndex, { frenchDefinition: value })}
                      placeholder="pièce de monnaie"
                    />
                  </FieldCard>
                </div>

                <FieldCard label="中文释义">
                  <TextInput
                    value={sense.chineseDefinition}
                    onChange={(value) => updateSense(senseIndex, { chineseDefinition: value })}
                    placeholder="硬币"
                  />
                </FieldCard>

                <div className="space-y-4 rounded-[1.8rem] border border-slate-200/70 bg-white/70 p-4 dark:border-white/10 dark:bg-slate-950/25">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="text-sm font-black text-slate-900 dark:text-white">例句列表</div>
                    <button
                      type="button"
                      onClick={() => addExample(senseIndex)}
                      className="rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10"
                    >
                      <Plus size={14} className="inline-block" /> 添加例句
                    </button>
                  </div>

                  {sense.examples.map((example, exampleIndex) => (
                    <div key={`example-${senseIndex}-${exampleIndex}`} className="space-y-4 rounded-[1.5rem] border border-slate-200/70 bg-white/80 p-4 dark:border-white/10 dark:bg-slate-950/30">
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <div className="text-sm font-bold text-slate-900 dark:text-white">例句 {exampleIndex + 1}</div>
                        <button
                          type="button"
                          onClick={() => removeExample(senseIndex, exampleIndex)}
                          className="text-sm text-rose-500"
                        >
                          删除例句
                        </button>
                      </div>

                      <div className="grid gap-4 md:grid-cols-2">
                        <FieldCard label="Sort Order">
                          <TextInput
                            type="number"
                            value={example.sortOrder}
                            onChange={(value) =>
                              updateExample(senseIndex, exampleIndex, {
                                sortOrder: Number(value) || example.sortOrder,
                              })
                            }
                          />
                        </FieldCard>
                        <FieldCard label="语境支持">
                          <SelectInput
                            value={example.contextSupportLevel}
                            onChange={(value) => updateExample(senseIndex, exampleIndex, { contextSupportLevel: value })}
                            options={contextSupportOptions.map((item) => ({ value: item.value, label: item.label }))}
                          />
                        </FieldCard>
                      </div>

                      <div className="grid gap-4 md:grid-cols-2">
                        <FieldCard label="英语例句">
                          <textarea
                            value={example.englishExample}
                            onChange={(event) =>
                              updateExample(senseIndex, exampleIndex, { englishExample: event.target.value })
                            }
                            rows={3}
                            className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none focus:border-primary/40 dark:border-white/10 dark:bg-slate-950/45"
                            placeholder="I found a coin on the floor."
                          />
                        </FieldCard>
                        <FieldCard label="法语例句">
                          <textarea
                            value={example.frenchExample}
                            onChange={(event) =>
                              updateExample(senseIndex, exampleIndex, { frenchExample: event.target.value })
                            }
                            rows={3}
                            className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none focus:border-primary/40 dark:border-white/10 dark:bg-slate-950/45"
                            placeholder="J'ai trouvé une pièce dans la rue."
                          />
                        </FieldCard>
                      </div>

                      <div className="grid gap-4 md:grid-cols-2">
                        <FieldCard label="中文译文">
                          <textarea
                            value={example.chineseTranslation}
                            onChange={(event) =>
                              updateExample(senseIndex, exampleIndex, { chineseTranslation: event.target.value })
                            }
                            rows={3}
                            className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none focus:border-primary/40 dark:border-white/10 dark:bg-slate-950/45"
                            placeholder="我在地上捡到一枚硬币。"
                          />
                        </FieldCard>
                        <FieldCard label="例句来源">
                          <TextInput
                            value={example.source}
                            onChange={(value) => updateExample(senseIndex, exampleIndex, { source: value })}
                            placeholder="教材 / 人工补充"
                          />
                        </FieldCard>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>

          {error && <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">{error}</div>}

          {detailQuery.error && (
            <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
              {detailQuery.error.message}
            </div>
          )}

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending}
              className="btn-liquid inline-flex items-center gap-2 px-6 py-3 text-white disabled:opacity-60"
            >
              {saveMutation.isPending ? <LoaderCircle size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
              {editor.id ? '更新词对' : '创建词对'}
            </button>

            {editor.id && (
              <button
                type="button"
                onClick={() => setPendingDeleteId(editor.id as number)}
                disabled={deleteMutation.isPending}
                className="inline-flex items-center gap-2 rounded-2xl border border-rose-500/20 px-5 py-3 text-sm font-bold text-rose-500 disabled:opacity-60"
              >
                <Trash2 size={16} />
                删除
              </button>
            )}

            {mode === 'admin' && canAccessAdminConsole && (
              <Link
                to="/admin/config-center"
                className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-5 py-3 text-sm font-bold text-slate-600 dark:border-white/10 dark:text-white/60"
              >
                导入后去做 RAG reindex
                <ArrowRight size={16} />
              </Link>
            )}
          </div>
          </SectionCard>
        </div>
        )}
      </div>

      {pendingDeleteId !== null && (
        <>
          <div
            aria-hidden="true"
            onClick={closeDeleteDialog}
            className="fixed inset-0 z-[60] bg-slate-950/45 backdrop-blur-sm"
          />
          <div className="fixed inset-0 z-[70] flex items-center justify-center px-4">
            <div
              ref={deleteDialogRef}
              role="dialog"
              aria-modal="true"
              aria-labelledby={deleteDialogTitleId}
              aria-describedby={deleteDialogDescriptionId}
              tabIndex={-1}
              className="w-full max-w-lg rounded-[2rem] border border-white/10 bg-white/90 p-6 shadow-[0_30px_80px_rgba(15,23,42,0.28)] backdrop-blur-xl dark:bg-slate-950/90"
            >
              <div className="text-[11px] uppercase tracking-[0.28em] text-rose-500">Danger Zone</div>
              <div id={deleteDialogTitleId} className="mt-3 text-2xl font-black text-slate-900 dark:text-white">确认删除当前词对？</div>
              <div id={deleteDialogDescriptionId} className="mt-4 text-sm leading-6 text-slate-500 dark:text-white/50">
                词对 <span className="font-bold text-slate-900 dark:text-white">{editor.englishWord || '--'} / {editor.frenchWord || '--'}</span> 将被删除。
                这个操作会移除词对及其义项、例句和关联关系，误删后需要重新录入。
              </div>

              <div className="mt-6 flex flex-wrap justify-end gap-3">
                <button
                  ref={deleteCancelButtonRef}
                  type="button"
                  onClick={closeDeleteDialog}
                  className="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-bold text-slate-500 dark:border-white/10 dark:text-white/45"
                >
                  取消
                </button>
                <button
                  type="button"
                  onClick={() => deleteMutation.mutate(pendingDeleteId)}
                  disabled={deleteMutation.isPending}
                  className="inline-flex items-center gap-2 rounded-2xl border border-rose-500/20 bg-rose-500 px-5 py-3 text-sm font-bold text-white disabled:opacity-60"
                >
                  {deleteMutation.isPending ? <LoaderCircle size={16} className="animate-spin" /> : <Trash2 size={16} />}
                  确认删除
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default LexicalPairsWorkspace;

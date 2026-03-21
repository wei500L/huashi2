import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowRight,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Download,
  ExternalLink,
  FileSpreadsheet,
  Info,
  LoaderCircle,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  Upload,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import { contextLevelLabel, formatDateTime, lexicalPairTypeLabel, userHasCapability } from '@/lib/format';
import type {
  CsvImportFailureVO,
  CsvImportResultVO,
  CsvImportTemplateFieldVO,
  LexicalPairDetailVO,
  LexicalPairUpsertRequest,
} from '@/lib/contracts';
import { adminService, lexicalPairService } from '@/lib/services';
import { useAuthStore } from '@/store';

export type LexicalPairsWorkspaceMode = 'teacher' | 'admin';

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

type CsvPreview = {
  headers: string[];
  rows: string[][];
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

const templateFieldLabels: Record<string, string> = {
  english_word: '英语词',
  french_word: '法语词',
  chinese_gloss: '中文释义',
  lexical_pair_type: '词对类型',
  semantic_overlap_score: '语义重合度',
  false_friend_risk: '负迁移风险',
  default_context_support: '默认语境支持',
  difficulty_level: '难度等级',
  notes: '备注',
  source: '来源',
  active: '启用状态',
  tags: '标签',
  knowledge_status: '知识状态',
  embedding_status: '向量状态',
  sense_english_definition: '英语义项',
  sense_french_definition: '法语义项',
  sense_chinese_definition: '中文义项',
  example_english: '英语例句',
  example_french: '法语例句',
  example_chinese: '中文译文',
  example_context_support: '例句语境支持',
};

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
    subtitle: '支持结构化创建、CSV 导入和失败行回查，不再依赖手写 JSON。',
    guideTitle: '教师维护指南',
    guideDescription: '先做结构化录入，再用 CSV 批量补齐；导入完成后优先修正失败行，避免脏数据进入训练链路。',
    successHint: '导入成功后，建议抽样打开几条词对检查义项和例句是否符合预期。',
  },
  admin: {
    title: '语料库管理',
    subtitle: '管理员可直接在工作区完成词对创建、批量导入和导入复核，无需切换到教师视角。',
    guideTitle: '管理员操作指南',
    guideDescription: '先下载模板核对字段，再执行批量导入；若需要让新词条立即进入 RAG 检索，请在导入完成后触发 reindex。',
    successHint: '导入成功不等于检索立即可见。若要刷新向量库，请前往配置中心执行 RAG reindex。',
  },
};

const defaultFilters: FilterState = {
  keyword: '',
  lexicalPairType: 'ALL',
  active: 'ALL',
  embeddingStatus: 'ALL',
};

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

function fieldLabel(fieldName: string): string {
  return templateFieldLabels[fieldName] || fieldName;
}

function translateImportMessage(message: string): string {
  const trimmed = message.trim();
  if (!trimmed) {
    return '导入失败，请检查文件内容。';
  }
  if (trimmed === 'CSV file must not be empty') {
    return 'CSV 文件不能为空。';
  }
  if (trimmed === 'Failed to read CSV file') {
    return 'CSV 文件读取失败，请确认文件编码为 UTF-8 且内容未损坏。';
  }
  if (trimmed === 'Duplicate lexical pair in CSV file') {
    return '同一份 CSV 中出现了重复词对，请先去重。';
  }
  if (trimmed === 'Lexical pair already exists') {
    return '系统中已存在相同英语词 / 法语词的词对。';
  }
  if (trimmed === 'Each sense must have at least one definition') {
    return '每个义项至少需要填写一种释义。';
  }
  if (trimmed === 'Each example must contain at least one sentence or translation') {
    return '每条例句至少需要填写英文、法文或中文中的一项。';
  }
  if (trimmed === 'active must be true or false') {
    return '`active` 列只能填写 true 或 false。';
  }
  if (trimmed.startsWith('Missing required CSV headers: ')) {
    const headers = trimmed
      .replace('Missing required CSV headers: ', '')
      .split(',')
      .map((item) => fieldLabel(item.trim()))
      .filter(Boolean);
    return `CSV 缺少必填列：${headers.join('、')}。`;
  }
  if (trimmed.endsWith(' is required')) {
    return `${fieldLabel(trimmed.replace(/ is required$/, ''))}不能为空。`;
  }
  if (trimmed.endsWith(' must be between 0 and 1')) {
    return `${fieldLabel(trimmed.replace(/ must be between 0 and 1$/, ''))}必须填写 0 到 1 之间的小数。`;
  }
  if (trimmed.endsWith(' must be between 1 and 5')) {
    return `${fieldLabel(trimmed.replace(/ must be between 1 and 5$/, ''))}必须填写 1 到 5 之间的整数。`;
  }
  if (trimmed.endsWith(' must be a decimal number')) {
    return `${fieldLabel(trimmed.replace(/ must be a decimal number$/, ''))}必须是小数。`;
  }
  if (trimmed.endsWith(' must be an integer')) {
    return `${fieldLabel(trimmed.replace(/ must be an integer$/, ''))}必须是整数。`;
  }
  return trimmed;
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

function formatImportFailure(failure: CsvImportFailureVO): { rowNumber: number; pairLabel: string; reason: string } {
  const rowNumber = failure.rowNumber ?? failure.lineNo ?? 0;
  const pairLabel =
    [failure.englishWord, failure.frenchWord].filter(Boolean).join(' / ') || '未识别词对';
  const reason = translateImportMessage(failure.reason ?? failure.message ?? '导入失败');
  return { rowNumber, pairLabel, reason };
}

function parseCsvText(text: string): string[][] {
  const rows: string[][] = [];
  let currentRow: string[] = [];
  let currentValue = '';
  let inQuotes = false;

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index];
    const nextChar = text[index + 1];

    if (char === '"') {
      if (inQuotes && nextChar === '"') {
        currentValue += '"';
        index += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }

    if (char === ',' && !inQuotes) {
      currentRow.push(currentValue);
      currentValue = '';
      continue;
    }

    if ((char === '\n' || char === '\r') && !inQuotes) {
      if (char === '\r' && nextChar === '\n') {
        index += 1;
      }
      currentRow.push(currentValue);
      if (currentRow.some((cell) => cell.trim() !== '')) {
        rows.push(currentRow);
      }
      currentRow = [];
      currentValue = '';
      continue;
    }

    currentValue += char;
  }

  if (currentValue.length > 0 || currentRow.length > 0) {
    currentRow.push(currentValue);
    if (currentRow.some((cell) => cell.trim() !== '')) {
      rows.push(currentRow);
    }
  }

  return rows;
}

async function buildCsvPreview(file: File): Promise<CsvPreview> {
  const text = await file.text();
  const rows = parseCsvText(text);
  if (!rows.length) {
    throw new Error('CSV 文件为空，无法预览。');
  }
  const headers = rows[0].map((cell) => cell.trim());
  const previewRows = rows.slice(1, 6);
  return { headers, rows: previewRows };
}

function getInlineReindexMeta(status?: string | null): {
  label: string;
  progress: number;
  className: string;
  description: string;
} {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'SUCCEEDED') {
    return {
      label: '已完成',
      progress: 100,
      className: 'bg-emerald-500',
      description: '向量重建已完成，可以回到列表抽样确认 embedding 状态。',
    };
  }
  if (normalized === 'FAILED') {
    return {
      label: '失败',
      progress: 100,
      className: 'bg-rose-500',
      description: '任务失败，请查看错误信息后重试。',
    };
  }
  if (['RUNNING', 'PROCESSING', 'IN_PROGRESS'].includes(normalized)) {
    return {
      label: '执行中',
      progress: 68,
      className: 'bg-sky-500',
      description: '后台正在分页拉取词条并重算 embedding。',
    };
  }
  if (['PENDING', 'QUEUED', 'SUBMITTED', 'CREATED'].includes(normalized)) {
    return {
      label: '排队中',
      progress: 28,
      className: 'bg-primary',
      description: '任务已提交，等待后台 worker 处理。',
    };
  }
  return {
    label: normalized || '未开始',
    progress: 0,
    className: 'bg-slate-300 dark:bg-white/20',
    description: '尚未触发 reindex。',
  };
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
  <section className="rounded-[2.4rem] liquid-glass-panel p-6 md:p-8 space-y-6">
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
  <label className="block rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-4 py-4 dark:border-white/10 dark:bg-white/[0.03]">
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
}> = ({ value, onChange, type = 'text', placeholder, step, disabled }) => (
  <input
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
    className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none transition focus:border-primary/40 disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:bg-slate-950/45"
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

export const LexicalPairsWorkspace: React.FC<{ mode: LexicalPairsWorkspaceMode }> = ({ mode }) => {
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const meta = workspaceMeta[mode];
  const canAccessAdminConsole = userHasCapability(user, 'ADMIN_CONSOLE');
  const fileInputRef = React.useRef<HTMLInputElement | null>(null);
  const [filters, setFilters] = React.useState<FilterState>(defaultFilters);
  const deferredKeyword = React.useDeferredValue(filters.keyword);
  const [selectedId, setSelectedId] = React.useState<number | null>(null);
  const [editor, setEditor] = React.useState<PairEditorState>(createEmptyEditor);
  const [showStructuredEditor, setShowStructuredEditor] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [importFile, setImportFile] = React.useState<File | null>(null);
  const [importPreview, setImportPreview] = React.useState<CsvPreview | null>(null);
  const [importPreviewError, setImportPreviewError] = React.useState<string | null>(null);
  const [importResult, setImportResult] = React.useState<CsvImportResultVO | null>(null);
  const [importFeedback, setImportFeedback] = React.useState<string | null>(null);
  const [dragActive, setDragActive] = React.useState(false);
  const [reindexJobId, setReindexJobId] = React.useState<number | null>(null);
  const [pollReindexJob, setPollReindexJob] = React.useState(false);
  const [reindexFeedback, setReindexFeedback] = React.useState<string | null>(null);

  const templateQuery = useQuery({
    queryKey: ['lexical-pair-import-template'],
    queryFn: () => lexicalPairService.getImportTemplate(),
    staleTime: 5 * 60 * 1000,
  });

  const overviewQuery = useQuery({
    queryKey: ['lexical-pair-overview'],
    queryFn: () => lexicalPairService.getOverview(),
    enabled: mode === 'admin' || canAccessAdminConsole,
  });

  const listQuery = useQuery({
    queryKey: ['lexical-pairs', deferredKeyword, filters.lexicalPairType, filters.active, filters.embeddingStatus],
    queryFn: () =>
      lexicalPairService.pageQuery({
        pageNo: 1,
        pageSize: 100,
        keyword: deferredKeyword.trim() || undefined,
        lexicalPairType: filters.lexicalPairType === 'ALL' ? undefined : filters.lexicalPairType,
        active: filters.active === 'ALL' ? undefined : filters.active === 'ACTIVE',
        embeddingStatus: filters.embeddingStatus === 'ALL' ? undefined : filters.embeddingStatus,
      }),
  });

  const detailQuery = useQuery({
    queryKey: ['lexical-pair-detail', selectedId],
    queryFn: () => lexicalPairService.getDetail(selectedId as number),
    enabled: selectedId !== null,
  });

  React.useEffect(() => {
    if (!detailQuery.data) {
      return;
    }
    setEditor(toEditor(detailQuery.data));
    setShowStructuredEditor(detailQuery.data.senses.length > 0);
    setError(null);
  }, [detailQuery.data]);

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
      setSelectedId(null);
      setEditor(createEmptyEditor());
      setShowStructuredEditor(false);
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-pair-overview'] });
    },
    onError: (mutationError) => {
      setError(mutationError instanceof Error ? mutationError.message : '词对删除失败。');
    },
  });

  const importMutation = useMutation({
    mutationFn: async () => {
      if (!importFile) {
        throw new Error('请先选择要上传的 CSV 文件。');
      }
      const formData = new FormData();
      formData.append('file', importFile);
      return lexicalPairService.importCsv(formData);
    },
    onSuccess: async (result) => {
      setImportResult(result);
      setImportFeedback(`本次导入成功 ${result.successCount} 条，失败 ${result.failedCount} 条。`);
      setReindexJobId(null);
      setPollReindexJob(false);
      setReindexFeedback(null);
      await queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-pair-overview'] });
    },
    onError: (mutationError) => {
      setImportResult(null);
      setReindexJobId(null);
      setPollReindexJob(false);
      setReindexFeedback(null);
      setImportFeedback(
        mutationError instanceof Error ? translateImportMessage(mutationError.message) : 'CSV 导入失败。'
      );
    },
  });

  const inlineReindexMutation = useMutation({
    mutationFn: () =>
      adminService.triggerRagReindex({
        mode: 'INCREMENTAL',
        sourceTypes: ['LEXICAL_PAIR'],
        forceReembed: false,
      }),
    onSuccess: (result) => {
      setReindexJobId(result.jobId);
      setPollReindexJob(true);
      setReindexFeedback(`已提交 reindex 任务 #${result.jobId}，正在后台处理中。`);
    },
    onError: (mutationError) => {
      setReindexFeedback(mutationError instanceof Error ? mutationError.message : 'RAG reindex 提交失败。');
    },
  });

  const reindexJobQuery = useQuery({
    queryKey: ['inline-lexical-reindex-job', reindexJobId],
    queryFn: () => adminService.getRagReindexJob(reindexJobId as number),
    enabled: reindexJobId !== null,
    refetchInterval: pollReindexJob ? 2000 : false,
  });

  React.useEffect(() => {
    const data = reindexJobQuery.data;
    if (!data || !['SUCCEEDED', 'FAILED'].includes(data.status)) {
      return;
    }
    setPollReindexJob(false);
    setReindexFeedback(
      data.status === 'SUCCEEDED'
        ? `RAG reindex #${data.jobId} 已完成。`
        : data.errorMessage || `RAG reindex #${data.jobId} 执行失败。`
    );
    void queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
    void queryClient.invalidateQueries({ queryKey: ['lexical-pair-overview'] });
  }, [queryClient, reindexJobQuery.data]);

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

  const clearImportState = React.useCallback(() => {
    setImportFile(null);
    setImportPreview(null);
    setImportPreviewError(null);
    setImportResult(null);
    setImportFeedback(null);
    setReindexJobId(null);
    setPollReindexJob(false);
    setReindexFeedback(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, []);

  const handleImportFileChange = React.useCallback(async (file: File | null) => {
    setImportFile(file);
    setImportResult(null);
    setImportFeedback(null);
    setReindexJobId(null);
    setPollReindexJob(false);
    setReindexFeedback(null);
    if (!file) {
      setImportPreview(null);
      setImportPreviewError(null);
      return;
    }
    try {
      const preview = await buildCsvPreview(file);
      setImportPreview(preview);
      setImportPreviewError(null);
    } catch (previewError) {
      setImportPreview(null);
      setImportPreviewError(previewError instanceof Error ? previewError.message : 'CSV 预览失败。');
    }
  }, []);

  const resetEditor = () => {
    setSelectedId(null);
    setEditor(createEmptyEditor());
    setShowStructuredEditor(false);
    setError(null);
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

  const failures = React.useMemo(
    () => (importResult?.failures || []).map((failure) => formatImportFailure(failure)),
    [importResult]
  );

  const selectedRisk = riskMeta(editor.falseFriendRisk);
  const structuredExampleCount = React.useMemo(
    () => editor.senses.reduce((total, sense) => total + sense.examples.length, 0),
    [editor.senses]
  );
  const inlineReindexMeta = getInlineReindexMeta(reindexJobQuery.data?.status);

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
              新建词对
            </button>
          </div>
        }
      />

      {mode === 'admin' && (
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
              {overviewQuery.data?.totalCount ?? '--'}
            </div>
            <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
              启用中 {overviewQuery.data?.activeCount ?? '--'} 条
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
              {overviewQuery.data?.pendingEmbeddingCount ?? '--'}
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
              {overviewQuery.data?.failedEmbeddingCount ?? '--'}
            </div>
            <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
              已嵌入 {overviewQuery.data?.embeddedCount ?? '--'} 条
            </div>
          </button>

          <div className="rounded-[2rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
            <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">最近更新时间</div>
            <div className="mt-3 text-base font-black text-slate-900 dark:text-white">
              {formatDateTime(overviewQuery.data?.latestUpdatedAt)}
            </div>
            <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
              最近新增 {formatDateTime(overviewQuery.data?.latestCreatedAt)} · 最近嵌入 {formatDateTime(overviewQuery.data?.latestEmbeddedAt)}
            </div>
          </div>
        </div>
      )}

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
          <div className="grid gap-4 md:grid-cols-3">
            {[
              '1. 下载模板并核对必填列。',
              '2. 优先填写基础字段，义项和例句按需补充。',
              '3. 导入后先处理失败行，再决定是否执行 reindex。',
            ].map((step) => (
              <div
                key={step}
                className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-5 text-sm leading-6 text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60"
              >
                {step}
              </div>
            ))}
          </div>

          <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
            <div className="mb-3 flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white">
              <Info size={16} className="text-primary" />
              常见导入错误
            </div>
            <div className="space-y-2 text-sm leading-6 text-slate-500 dark:text-white/45">
              <div>字段名必须与模板首行完全一致，不能随意改列名。</div>
              <div>`semantic_overlap_score` 和 `false_friend_risk` 只能填 0 到 1。</div>
              <div>`difficulty_level` 只能填 1 到 5，`active` 只能填 true 或 false。</div>
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
                {(templateQuery.data?.fields || []).map((field: CsvImportTemplateFieldVO) => (
                  <tr key={field.fieldName || field.key} className="border-t border-slate-200/70 dark:border-white/10">
                    <td className="py-3 font-semibold text-slate-900 dark:text-white">
                      {fieldLabel(field.fieldName || field.key || '')}
                      <div className="mt-1 text-xs font-normal text-slate-400 dark:text-white/30">
                        {field.fieldName || field.key}
                      </div>
                    </td>
                    <td className="py-3 text-slate-500 dark:text-white/50">{field.required ? '必填' : '选填'}</td>
                    <td className="py-3 text-slate-500 dark:text-white/50">{field.description || field.label || '--'}</td>
                    <td className="py-3 text-slate-500 dark:text-white/50">{field.example || '--'}</td>
                  </tr>
                ))}
                {templateQuery.isLoading && (
                  <tr>
                    <td colSpan={4} className="py-4 text-slate-500 dark:text-white/45">
                      模板字段加载中...
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </details>
      </SectionCard>

      <div className="grid gap-8 xl:grid-cols-[0.92fr_1.08fr]">
        <div className="space-y-8">
          <SectionCard
            title="词对列表"
            description="支持关键词、词对类型、启用状态和向量状态过滤。左侧选中，右侧编辑。"
          >
            <div className={`grid gap-4 ${mode === 'admin' ? 'md:grid-cols-4' : 'md:grid-cols-3'}`}>
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

            <div className="flex flex-wrap items-center gap-3">
              <div className="rounded-full border border-slate-200/70 bg-white/70 px-4 py-2 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                共 {listQuery.data?.total ?? 0} 条词对
              </div>
              <button
                type="button"
                onClick={() => setFilters(defaultFilters)}
                className="rounded-full border border-slate-200/70 px-4 py-2 text-sm text-slate-500 dark:border-white/10 dark:text-white/45"
              >
                重置筛选
              </button>
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
                  当前筛选条件下没有词对记录。
                </div>
              )}

              {(listQuery.data?.records || []).map((item) => {
                const itemRisk = riskMeta(item.falseFriendRisk);
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setSelectedId(item.id)}
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
                        {lexicalPairTypeLabel(item.lexicalPairType)}
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
                  </button>
                );
              })}
            </div>
          </SectionCard>

          <SectionCard
            title="CSV 批量导入"
            description="支持拖拽上传、前 5 行预览和失败行回查。建议先确认表头和示例行，再执行正式导入。"
            actions={
              <div className="rounded-[1.5rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                <FileSpreadsheet size={16} className="mb-2 text-primary" />
                UTF-8 编码、首行必须是模板列名、支持拖拽 CSV
              </div>
            }
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              onChange={(event) => {
                void handleImportFileChange(event.target.files?.[0] || null);
              }}
              className="hidden"
            />

            <div
              onDragOver={(event) => {
                event.preventDefault();
                setDragActive(true);
              }}
              onDragLeave={(event) => {
                event.preventDefault();
                setDragActive(false);
              }}
              onDrop={(event) => {
                event.preventDefault();
                setDragActive(false);
                void handleImportFileChange(event.dataTransfer.files?.[0] || null);
              }}
              className={`rounded-[1.8rem] border border-dashed p-6 transition ${
                dragActive
                  ? 'border-primary/40 bg-primary/5'
                  : 'border-slate-300 bg-white/50 dark:border-white/15 dark:bg-white/[0.02]'
              }`}
            >
              <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <div className="text-sm font-bold text-slate-900 dark:text-white">待上传文件</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {importFile ? `${importFile.name} · ${(importFile.size / 1024).toFixed(1)} KB` : '拖拽 CSV 到这里，或点击按钮选择文件'}
                  </div>
                  <div className="mt-2 text-xs text-slate-400 dark:text-white/30">预览只展示前 5 行，正式校验仍以后端导入结果为准。</div>
                </div>

                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
                  >
                    选择 CSV
                  </button>
                  <button
                    type="button"
                    onClick={() => void downloadTemplate()}
                    className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
                  >
                    再看模板
                  </button>
                </div>
              </div>
            </div>

            {importPreviewError && (
              <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
                {importPreviewError}
              </div>
            )}

            {importPreview && (
              <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-bold text-slate-900 dark:text-white">CSV 预览</div>
                    <div className="mt-1 text-xs text-slate-500 dark:text-white/40">
                      当前展示表头和前 {importPreview.rows.length} 行数据，确认无误后再点击导入。
                    </div>
                  </div>
                  <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                    {importPreview.headers.length} 列
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full min-w-[720px] text-left text-sm">
                    <thead className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                      <tr>
                        <th className="pb-3">#</th>
                        {importPreview.headers.map((header) => (
                          <th key={header} className="pb-3">
                            {fieldLabel(header)}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {importPreview.rows.map((row, rowIndex) => (
                        <tr key={`preview-row-${rowIndex}`} className="border-t border-slate-200/70 dark:border-white/10">
                          <td className="py-3 text-slate-400 dark:text-white/30">{rowIndex + 1}</td>
                          {importPreview.headers.map((header, cellIndex) => (
                            <td key={`${header}-${cellIndex}`} className="py-3 text-slate-600 dark:text-white/55">
                              {row[cellIndex] || '--'}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => importMutation.mutate()}
                disabled={importMutation.isPending}
                className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:opacity-60"
              >
                <Upload size={16} />
                {importMutation.isPending ? '上传中...' : '开始导入'}
              </button>
              <button
                type="button"
                onClick={clearImportState}
                className="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-bold text-slate-500 dark:border-white/10 dark:text-white/45"
              >
                清空结果
              </button>
            </div>

            {importFeedback && (
              <div
                className={`rounded-[1.8rem] border px-5 py-4 text-sm ${
                  importResult
                    ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
                    : 'border-rose-500/20 bg-rose-500/5 text-rose-500'
                }`}
              >
                {importFeedback}
              </div>
            )}

            {importResult && (
              <div className="space-y-4">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-[1.8rem] border border-emerald-500/20 bg-emerald-500/5 px-5 py-5">
                    <div className="text-xs uppercase tracking-[0.24em] text-emerald-600/70 dark:text-emerald-400/70">Success</div>
                    <div className="mt-2 text-3xl font-black text-emerald-600 dark:text-emerald-400">{importResult.successCount}</div>
                  </div>
                  <div className="rounded-[1.8rem] border border-amber-500/20 bg-amber-500/5 px-5 py-5">
                    <div className="text-xs uppercase tracking-[0.24em] text-amber-600/70 dark:text-amber-400/70">Failed</div>
                    <div className="mt-2 text-3xl font-black text-amber-600 dark:text-amber-400">{importResult.failedCount}</div>
                  </div>
                </div>

                {mode === 'admin' && canAccessAdminConsole && importResult.successCount > 0 && (
                  <div className="rounded-[1.8rem] border border-primary/20 bg-primary/5 p-5">
                    <div className="flex flex-wrap items-start justify-between gap-4">
                      <div>
                        <div className="text-sm font-bold text-slate-900 dark:text-white">下一步：一键重建 RAG 索引</div>
                        <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                          本次导入有 {importResult.successCount} 条成功记录。若希望新词条尽快进入检索，请直接触发增量 reindex。
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => inlineReindexMutation.mutate()}
                        disabled={inlineReindexMutation.isPending || pollReindexJob}
                        className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:opacity-60"
                      >
                        <RefreshCw size={16} className={pollReindexJob ? 'animate-spin' : ''} />
                        {inlineReindexMutation.isPending ? '提交中...' : pollReindexJob ? '重建中...' : '一键增量 Reindex'}
                      </button>
                    </div>

                    {(reindexFeedback || reindexJobQuery.data) && (
                      <div className="mt-5 rounded-[1.4rem] border border-slate-200/70 bg-white/75 p-4 dark:border-white/10 dark:bg-slate-950/25">
                        {reindexFeedback && <div className="text-sm text-slate-600 dark:text-white/60">{reindexFeedback}</div>}
                        {reindexJobQuery.data && (
                          <>
                            <div className="mt-3 flex items-center justify-between gap-3">
                              <div className="font-bold text-slate-900 dark:text-white">
                                任务 #{reindexJobQuery.data.jobId} · {inlineReindexMeta.label}
                              </div>
                              <div className="text-xs text-slate-400 dark:text-white/30">{inlineReindexMeta.progress}%</div>
                            </div>
                            <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200/70 dark:bg-white/10">
                              <div
                                className={`h-full rounded-full transition-all ${inlineReindexMeta.className}`}
                                style={{ width: `${inlineReindexMeta.progress}%` }}
                              />
                            </div>
                            <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{inlineReindexMeta.description}</div>
                            {reindexJobQuery.data.errorMessage && (
                              <div className="mt-3 text-sm text-rose-500">{reindexJobQuery.data.errorMessage}</div>
                            )}
                          </>
                        )}
                      </div>
                    )}
                  </div>
                )}

                {failures.length > 0 && (
                  <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                    <div className="mb-4 flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white">
                      <Info size={16} className="text-primary" />
                      失败行详情
                    </div>
                    <div className="space-y-3">
                      {failures.map((failure) => (
                        <div
                          key={`${failure.rowNumber}-${failure.pairLabel}-${failure.reason}`}
                          className="rounded-[1.4rem] border border-slate-200/70 bg-white/80 px-4 py-4 text-sm dark:border-white/10 dark:bg-slate-950/30"
                        >
                          <div className="font-bold text-slate-900 dark:text-white">
                            第 {failure.rowNumber} 行 · {failure.pairLabel}
                          </div>
                          <div className="mt-2 text-slate-500 dark:text-white/45">{failure.reason}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </SectionCard>
        </div>

        <SectionCard
          title={editor.id ? `编辑词对 #${editor.id}` : '新建词对'}
          description="基础信息优先填写，义项和例句按卡片结构维护。空白义项或空白例句会在保存时自动忽略。"
          actions={
            detailQuery.isFetching ? (
              <div className="inline-flex items-center gap-2 rounded-full border border-slate-200/70 px-4 py-2 text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
                <LoaderCircle size={14} className="animate-spin" />
                正在加载详情
              </div>
            ) : (
              <div className={`rounded-full border px-4 py-2 text-sm font-bold ${selectedRisk.className}`}>
                当前风险 {selectedRisk.label}
              </div>
            )
          }
        >
          <div className="grid gap-4 md:grid-cols-2">
            <FieldCard label="英语词" hint="如 coin。用于训练展示和去重。">
              <TextInput value={editor.englishWord} onChange={(value) => updateEditor('englishWord', value)} placeholder="coin" />
            </FieldCard>
            <FieldCard label="法语词" hint="如 coin。与英语词组成唯一词对。">
              <TextInput value={editor.frenchWord} onChange={(value) => updateEditor('frenchWord', value)} placeholder="coin" />
            </FieldCard>
          </div>

          <FieldCard label="中文释义" hint="面向运营和教师，建议用分号区分多义。">
            <TextInput value={editor.chineseGloss} onChange={(value) => updateEditor('chineseGloss', value)} placeholder="硬币；角落" />
          </FieldCard>

          <div className="grid gap-4 md:grid-cols-3">
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

                <div className="grid gap-4 md:grid-cols-3">
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
                onClick={() => deleteMutation.mutate(editor.id as number)}
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
    </div>
  );
};

export default LexicalPairsWorkspace;

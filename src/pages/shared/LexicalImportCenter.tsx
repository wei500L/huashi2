import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, FileSpreadsheet, LoaderCircle, RefreshCw, Save, Upload } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Pagination } from '@/components/common';
import type {
  LexicalImportBatchDetailVO,
  LexicalImportBatchStatus,
  LexicalImportRowStatus,
  LexicalImportRowUpdateRequest,
  LexicalImportRowVO,
  RagReindexJobResponse,
} from '@/lib/contracts';
import { saveBlob } from '@/lib/api';
import { contextLevelLabel, formatDateTime, lexicalPairTypeLabel } from '@/lib/format';
import { formatFileSize, translateImportMessage } from '@/lib/lexical-import';
import { adminService, lexicalPairService } from '@/lib/services';

type LexicalImportCenterMode = 'teacher' | 'admin';
type ImportView = 'all' | 'pending' | 'failed';

type ImportRowFormState = LexicalImportRowUpdateRequest;
type EditableRowFieldKey = Exclude<keyof ImportRowFormState, 'skipped'>;

type StatusMeta = {
  label: string;
  className: string;
};

const batchStatusOptions: Array<{ value: string; label: string }> = [
  { value: '', label: '全部批次状态' },
  { value: 'PARSING', label: '解析中' },
  { value: 'DRAFT', label: '待确认' },
  { value: 'IMPORTING', label: '导入中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'FAILED', label: '失败' },
];

const rowStatusOptions: Array<{ value: string; label: string }> = [
  { value: '', label: '全部行状态' },
  { value: 'READY', label: '可导入' },
  { value: 'INVALID', label: '需修正' },
  { value: 'SKIPPED', label: '已跳过' },
  { value: 'IMPORTED', label: '已导入' },
];

const lexicalPairTypeOptions = [
  { value: 'COGNATE', label: '同源词' },
  { value: 'FALSE_FRIEND', label: '同形异义' },
  { value: 'PARTIAL_COGNATE', label: '部分同源' },
  { value: 'ORTHOGRAPHIC_SIMILAR', label: '近形词' },
];

const contextSupportOptions = [
  { value: 'LOW', label: '低语境' },
  { value: 'MEDIUM', label: '中语境' },
  { value: 'HIGH', label: '高语境' },
];

const knowledgeStatusOptions = [
  { value: '', label: '默认草稿（DRAFT）' },
  { value: 'DRAFT', label: '草稿（DRAFT）' },
  { value: 'READY', label: '就绪（READY）' },
  { value: 'DISABLED', label: '停用（DISABLED）' },
];

const embeddingStatusOptions = [
  { value: '', label: '默认待嵌入（PENDING）' },
  { value: 'PENDING', label: '待嵌入（PENDING）' },
  { value: 'EMBEDDED', label: '已嵌入（EMBEDDED）' },
  { value: 'FAILED', label: '嵌入失败（FAILED）' },
];

const activeOptions = [
  { value: '', label: '默认启用（true）' },
  { value: 'true', label: '启用（true）' },
  { value: 'false', label: '停用（false）' },
];

const rowFieldGroups: Array<{
  title: string;
  fields: Array<{ key: EditableRowFieldKey; label: string; type?: 'text' | 'textarea' | 'select'; options?: Array<{ value: string; label: string }> }>;
}> = [
  {
    title: '基础字段',
    fields: [
      { key: 'englishWord', label: '英语词' },
      { key: 'frenchWord', label: '法语词' },
      { key: 'chineseGloss', label: '中文释义' },
      { key: 'lexicalPairType', label: '词对类型', type: 'select', options: lexicalPairTypeOptions },
      { key: 'semanticOverlapScore', label: '语义重合度' },
      { key: 'falseFriendRisk', label: '负迁移风险' },
      { key: 'defaultContextSupport', label: '默认语境支持', type: 'select', options: contextSupportOptions },
      { key: 'difficultyLevel', label: '难度等级' },
      { key: 'active', label: '启用状态', type: 'select', options: activeOptions },
      { key: 'tags', label: '标签（| 分隔）' },
      { key: 'knowledgeStatus', label: '知识状态', type: 'select', options: knowledgeStatusOptions },
      { key: 'embeddingStatus', label: '向量状态', type: 'select', options: embeddingStatusOptions },
    ],
  },
  {
    title: '释义与例句',
    fields: [
      { key: 'senseEnglishDefinition', label: '英语义项', type: 'textarea' },
      { key: 'senseFrenchDefinition', label: '法语义项', type: 'textarea' },
      { key: 'senseChineseDefinition', label: '中文义项', type: 'textarea' },
      { key: 'exampleEnglish', label: '英语例句', type: 'textarea' },
      { key: 'exampleFrench', label: '法语例句', type: 'textarea' },
      { key: 'exampleChinese', label: '中文译文', type: 'textarea' },
      { key: 'exampleContextSupport', label: '例句语境支持', type: 'select', options: contextSupportOptions },
      { key: 'source', label: '来源' },
      { key: 'notes', label: '备注', type: 'textarea' },
    ],
  },
];

function createEmptyRowForm(): ImportRowFormState {
  return {
    englishWord: '',
    frenchWord: '',
    chineseGloss: '',
    lexicalPairType: 'COGNATE',
    semanticOverlapScore: '0.50',
    falseFriendRisk: '0.10',
    defaultContextSupport: 'LOW',
    difficultyLevel: '3',
    notes: '',
    source: '',
    active: 'true',
    tags: '',
    knowledgeStatus: null,
    embeddingStatus: null,
    senseEnglishDefinition: '',
    senseFrenchDefinition: '',
    senseChineseDefinition: '',
    exampleEnglish: '',
    exampleFrench: '',
    exampleChinese: '',
    exampleContextSupport: 'MEDIUM',
    skipped: false,
  };
}

function toRowForm(row?: LexicalImportRowVO | null): ImportRowFormState {
  if (!row) {
    return createEmptyRowForm();
  }
  return {
    englishWord: row.draft.englishWord || '',
    frenchWord: row.draft.frenchWord || '',
    chineseGloss: row.draft.chineseGloss || '',
    lexicalPairType: row.draft.lexicalPairType || 'COGNATE',
    semanticOverlapScore: row.draft.semanticOverlapScore || '0.50',
    falseFriendRisk: row.draft.falseFriendRisk || '0.10',
    defaultContextSupport: row.draft.defaultContextSupport || 'LOW',
    difficultyLevel: row.draft.difficultyLevel || '3',
    notes: row.draft.notes || '',
    source: row.draft.source || '',
    active: row.draft.active || 'true',
    tags: row.draft.tags || '',
    knowledgeStatus: row.draft.knowledgeStatus ?? null,
    embeddingStatus: row.draft.embeddingStatus ?? null,
    senseEnglishDefinition: row.draft.senseEnglishDefinition || '',
    senseFrenchDefinition: row.draft.senseFrenchDefinition || '',
    senseChineseDefinition: row.draft.senseChineseDefinition || '',
    exampleEnglish: row.draft.exampleEnglish || '',
    exampleFrench: row.draft.exampleFrench || '',
    exampleChinese: row.draft.exampleChinese || '',
    exampleContextSupport: row.draft.exampleContextSupport || 'MEDIUM',
    skipped: row.status === 'SKIPPED',
  };
}

function normalizeEditableRowFieldValue(key: EditableRowFieldKey, value: string): ImportRowFormState[EditableRowFieldKey] {
  if (key === 'knowledgeStatus' || key === 'embeddingStatus') {
    return (value || null) as ImportRowFormState[EditableRowFieldKey];
  }
  return value as ImportRowFormState[EditableRowFieldKey];
}

function buildBatchStatusMeta(status?: string | null): StatusMeta {
  const normalized = String(status || '').toUpperCase() as LexicalImportBatchStatus | '';
  switch (normalized) {
    case 'PARSING':
      return { label: '解析中', className: 'border-sky-500/20 bg-sky-500/10 text-sky-600 dark:text-sky-400' };
    case 'DRAFT':
      return { label: '待确认', className: 'border-amber-500/20 bg-amber-500/10 text-amber-600 dark:text-amber-400' };
    case 'IMPORTING':
      return { label: '导入中', className: 'border-primary/20 bg-primary/10 text-primary' };
    case 'COMPLETED':
      return { label: '已完成', className: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' };
    case 'FAILED':
      return { label: '失败', className: 'border-rose-500/20 bg-rose-500/10 text-rose-500' };
    default:
      return { label: status || '--', className: 'border-slate-200/70 bg-white/70 text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45' };
  }
}

function buildRowStatusMeta(status?: string | null): StatusMeta {
  const normalized = String(status || '').toUpperCase() as LexicalImportRowStatus | '';
  switch (normalized) {
    case 'READY':
      return { label: '可导入', className: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' };
    case 'INVALID':
      return { label: '需修正', className: 'border-rose-500/20 bg-rose-500/10 text-rose-500' };
    case 'SKIPPED':
      return { label: '已跳过', className: 'border-slate-300/70 bg-slate-200/60 text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45' };
    case 'IMPORTED':
      return { label: '已导入', className: 'border-primary/20 bg-primary/10 text-primary' };
    default:
      return { label: status || '--', className: 'border-slate-200/70 bg-white/70 text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45' };
  }
}

const Panel: React.FC<{
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

const MetricCard: React.FC<{ label: string; value: number; className: string }> = ({ label, value, className }) => (
  <div className={`rounded-[1.8rem] border px-5 py-5 ${className}`}>
    <div className="text-[11px] uppercase tracking-[0.24em] opacity-70">{label}</div>
    <div className="mt-2 text-3xl font-black">{value}</div>
  </div>
);

const TextField: React.FC<{
  label: string;
  value: string | boolean | null | undefined;
  onChange: (value: string) => void;
  type?: 'text' | 'textarea' | 'select';
  options?: Array<{ value: string; label: string }>;
}> = ({ label, value, onChange, type = 'text', options }) => (
  <label className="block min-w-0 rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 dark:border-white/10 dark:bg-white/[0.03]">
    <div className="mb-3 text-[11px] font-bold uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{label}</div>
    {type === 'textarea' ? (
      <textarea
        value={String(value ?? '')}
        onChange={(event) => onChange(event.target.value)}
        rows={3}
        className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none transition focus:border-primary/40 dark:border-white/10 dark:bg-slate-950/45"
      />
    ) : type === 'select' ? (
      <select
        value={String(value ?? '')}
        onChange={(event) => onChange(event.target.value)}
        className="native-select w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none transition focus:border-primary/40 dark:border-white/10 dark:bg-slate-950/45"
      >
        {(options || []).map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    ) : (
      <input
        value={String(value ?? '')}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-2xl border border-slate-200 bg-white/80 px-4 py-3 text-sm outline-none transition focus:border-primary/40 dark:border-white/10 dark:bg-slate-950/45"
      />
    )}
  </label>
);

function isBatchProcessing(batch?: Pick<LexicalImportBatchDetailVO, 'status'> | null): boolean {
  return Boolean(batch && ['PARSING', 'IMPORTING'].includes(batch.status));
}

function buildEmbeddingSyncHint(batch: Pick<LexicalImportBatchDetailVO, 'importedRows' | 'pendingEmbeddingCount' | 'embeddedCount' | 'failedEmbeddingCount'>): string {
  if (batch.importedRows <= 0) {
    return '这批数据还没有写入词库，知识同步统计会在正式导入后出现。';
  }
  if (batch.failedEmbeddingCount > 0) {
    return `已有 ${batch.failedEmbeddingCount} 条词对嵌入失败，建议先回到词库总览检查失败原因。`;
  }
  if (batch.pendingEmbeddingCount > 0) {
    return `仍有 ${batch.pendingEmbeddingCount} 条词对等待嵌入，后台知识同步还在继续。`;
  }
  return `这批已导入词对已全部进入知识库，当前已嵌入 ${batch.embeddedCount} 条。`;
}

function isTerminalRagJobStatus(status?: string | null): boolean {
  return status === 'SUCCEEDED' || status === 'FAILED' || status === 'CANCELLED';
}

function readNumericStat(stats: Record<string, unknown> | undefined, key: string): number | null {
  const value = stats?.[key];
  return typeof value === 'number' ? value : null;
}

function buildRagJobSummary(job?: RagReindexJobResponse | null): string {
  if (!job) {
    return '提交定向重建后，这里会显示本批次最近一次手动任务的进度。';
  }
  const documentsProcessed = readNumericStat(job.stats, 'documentsProcessed');
  const chunksProcessed = readNumericStat(job.stats, 'chunksProcessed');
  const embeddedChunks = readNumericStat(job.stats, 'embeddedChunks');
  const statsSummary = [documentsProcessed, chunksProcessed, embeddedChunks].every((value) => value === null)
    ? null
    : `文档 ${documentsProcessed ?? '--'} · 分块 ${chunksProcessed ?? '--'} · 新嵌入 ${embeddedChunks ?? '--'}`;
  if (job.status === 'FAILED') {
    return job.errorMessage || '任务执行失败，请查看 ai-gateway 或 app-server 日志。';
  }
  if (job.status === 'SUCCEEDED') {
    return statsSummary ? `任务已完成。${statsSummary}` : '任务已完成。';
  }
  return statsSummary ? `任务进行中。${statsSummary}` : '任务已提交，正在等待 ai-gateway 完成重建。';
}

function normalizeImportView(value?: string | null): ImportView {
  if (value === 'pending' || value === 'failed') {
    return value;
  }
  return 'all';
}

export const LexicalImportCenter: React.FC<{ mode: LexicalImportCenterMode }> = ({ mode }) => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const fileInputRef = React.useRef<HTMLInputElement | null>(null);
  const lastBatchStatusRef = React.useRef<LexicalImportBatchStatus | null>(null);
  const [uploadFile, setUploadFile] = React.useState<File | null>(null);
  const [selectedBatchId, setSelectedBatchId] = React.useState<number | null>(() => {
    const parsed = Number(searchParams.get('batchId') || '');
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  });
  const [selectedRowId, setSelectedRowId] = React.useState<number | null>(null);
  const [batchPageNo, setBatchPageNo] = React.useState(1);
  const [batchPageSize, setBatchPageSize] = React.useState(12);
  const [rowPageNo, setRowPageNo] = React.useState(1);
  const [rowPageSize, setRowPageSize] = React.useState(20);
  const [view, setView] = React.useState<ImportView>(() => normalizeImportView(searchParams.get('view')));
  const [batchStatus, setBatchStatus] = React.useState(() => searchParams.get('status') || (searchParams.get('view') === 'failed' ? 'FAILED' : ''));
  const [batchKeyword, setBatchKeyword] = React.useState('');
  const [batchOwnerUserId, setBatchOwnerUserId] = React.useState('');
  const [rowStatus, setRowStatus] = React.useState('');
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [rowForm, setRowForm] = React.useState<ImportRowFormState>(createEmptyRowForm);
  const [reindexJobIdsByBatch, setReindexJobIdsByBatch] = React.useState<Record<number, string>>({});
  const [settledReindexJobs, setSettledReindexJobs] = React.useState<Record<string, boolean>>({});
  const isAdmin = mode === 'admin';
  const [source] = React.useState(() => searchParams.get('source') || '');
  const activeReindexJobId = selectedBatchId === null ? null : reindexJobIdsByBatch[selectedBatchId] || null;

  const usersQuery = useQuery({
    queryKey: ['admin-users-for-import-history'],
    queryFn: ({ signal }) => adminService.listUsers({ pageNo: 1, pageSize: 200 }, { signal }),
    enabled: isAdmin,
    staleTime: 5 * 60 * 1000,
  });

  const batchesQuery = useQuery({
    queryKey: ['lexical-import-batches', mode, view, batchStatus, batchKeyword, batchOwnerUserId, batchPageNo, batchPageSize],
    queryFn: ({ signal }) =>
      lexicalPairService.listImportBatches(
        {
          pageNo: batchPageNo,
          pageSize: batchPageSize,
          view,
          status: batchStatus || undefined,
          keyword: batchKeyword.trim() || undefined,
          ownerUserId: batchOwnerUserId ? Number(batchOwnerUserId) : undefined,
        },
        { signal }
      ),
  });

  React.useEffect(() => {
    const records = batchesQuery.data?.records || [];
    if (!records.length) {
      setSelectedBatchId(null);
      return;
    }
    if (selectedBatchId !== null && records.some((batch) => batch.id === selectedBatchId)) {
      return;
    }
    setSelectedBatchId(records[0].id);
  }, [batchesQuery.data?.records, selectedBatchId]);

  React.useEffect(() => {
    const nextSearchParams = new URLSearchParams(searchParams);
    if (view !== 'all') {
      nextSearchParams.set('view', view);
    } else {
      nextSearchParams.delete('view');
    }
    if (batchStatus) {
      nextSearchParams.set('status', batchStatus);
    } else {
      nextSearchParams.delete('status');
    }
    if (selectedBatchId) {
      nextSearchParams.set('batchId', String(selectedBatchId));
    } else {
      nextSearchParams.delete('batchId');
    }
    if (source) {
      nextSearchParams.set('source', source);
    }
    if (nextSearchParams.toString() !== searchParams.toString()) {
      setSearchParams(nextSearchParams, { replace: true });
    }
  }, [batchStatus, searchParams, selectedBatchId, setSearchParams, source, view]);

  const batchDetailQuery = useQuery({
    queryKey: ['lexical-import-batch-detail', selectedBatchId],
    queryFn: ({ signal }) => lexicalPairService.getImportBatch(selectedBatchId as number, { signal }),
    enabled: selectedBatchId !== null,
    refetchInterval: 2000,
  });

  const rowsQuery = useQuery({
    queryKey: ['lexical-import-batch-rows', selectedBatchId, rowStatus, rowPageNo, rowPageSize],
    queryFn: ({ signal }) =>
      lexicalPairService.listImportRows(
        selectedBatchId as number,
        {
          pageNo: rowPageNo,
          pageSize: rowPageSize,
          status: rowStatus || undefined,
        },
        { signal }
      ),
    enabled: selectedBatchId !== null && batchDetailQuery.data?.status !== 'PARSING',
  });

  const reindexJobQuery = useQuery({
    queryKey: ['lexical-import-batch-reindex-job', activeReindexJobId],
    queryFn: ({ signal }) => adminService.getRagReindexJob(activeReindexJobId as string, { signal }),
    enabled: isAdmin && activeReindexJobId !== null && !settledReindexJobs[activeReindexJobId],
    refetchInterval: 2000,
  });

  React.useEffect(() => {
    const currentStatus = batchDetailQuery.data?.status ?? null;
    const previousStatus = lastBatchStatusRef.current;
    if (currentStatus === 'COMPLETED' && previousStatus === 'IMPORTING') {
      void queryClient.invalidateQueries({ queryKey: ['lexical-pairs'] });
      void queryClient.invalidateQueries({ queryKey: ['lexical-pair-overview'] });
      void queryClient.invalidateQueries({ queryKey: ['lexical-import-batches'] });
      setFeedback('导入任务已完成，词对列表已自动刷新。');
    }
    lastBatchStatusRef.current = currentStatus;
  }, [batchDetailQuery.data?.status, queryClient]);

  React.useEffect(() => {
    const job = reindexJobQuery.data;
    if (!job || !activeReindexJobId || !isTerminalRagJobStatus(job.status)) {
      return;
    }
    setSettledReindexJobs((current) => ({ ...current, [activeReindexJobId]: true }));
    void queryClient.invalidateQueries({ queryKey: ['lexical-import-batch-detail', selectedBatchId] });
    void queryClient.invalidateQueries({ queryKey: ['lexical-pair-overview'] });
    if (job.status === 'SUCCEEDED') {
      setFeedback(`批次 #${selectedBatchId} 的定向重建已完成，知识同步摘要已刷新。`);
      return;
    }
    setFeedback(job.errorMessage || `批次 #${selectedBatchId} 的定向重建失败。`);
  }, [activeReindexJobId, queryClient, reindexJobQuery.data, selectedBatchId]);

  React.useEffect(() => {
    if (!rowsQuery.data?.records.length) {
      setSelectedRowId(null);
      setRowForm(createEmptyRowForm());
      return;
    }
    const currentRow = rowsQuery.data.records.find((item) => item.id === selectedRowId);
    const nextRow = currentRow || rowsQuery.data.records[0];
    setSelectedRowId(nextRow.id);
    setRowForm(toRowForm(nextRow));
  }, [rowsQuery.data, selectedRowId]);

  const selectedRow = React.useMemo(
    () => rowsQuery.data?.records.find((item) => item.id === selectedRowId) || null,
    [rowsQuery.data, selectedRowId]
  );

  React.useEffect(() => {
    setBatchPageNo(1);
  }, [batchStatus, batchKeyword, batchOwnerUserId, batchPageSize]);

  React.useEffect(() => {
    setRowPageNo(1);
  }, [rowStatus, rowPageSize, selectedBatchId]);

  const createBatchMutation = useMutation({
    mutationFn: async () => {
      if (!uploadFile) {
        throw new Error('请先选择要上传的 CSV 或 XLSX 文件。');
      }
      const formData = new FormData();
      formData.append('file', uploadFile);
      return lexicalPairService.createImportBatch(formData, { timeout: 120000 });
    },
    onSuccess: async (result) => {
      setFeedback(`已创建导入批次 #${result.batchId}，后台正在解析文件。`);
      setSelectedBatchId(result.batchId);
      setSelectedRowId(null);
      setUploadFile(null);
      setBatchPageNo(1);
      setRowPageNo(1);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batches'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batch-detail', result.batchId] });
    },
    onError: (error) => {
      setFeedback(translateImportMessage(error instanceof Error ? error.message : '导入批次创建失败。'));
    },
  });

  const updateRowMutation = useMutation({
    mutationFn: () => {
      if (!selectedBatchId || !selectedRowId) {
        throw new Error('请先选择要编辑的导入行。');
      }
      return lexicalPairService.updateImportRow(selectedBatchId, selectedRowId, rowForm);
    },
    onSuccess: async (row) => {
      setFeedback(`已保存第 ${row.rowNumber} 行草稿。`);
      setRowForm(toRowForm(row));
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batch-rows', selectedBatchId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batch-detail', selectedBatchId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batches'] });
    },
    onError: (error) => {
      setFeedback(translateImportMessage(error instanceof Error ? error.message : '草稿保存失败。'));
    },
  });

  const commitBatchMutation = useMutation({
    mutationFn: () => {
      if (!selectedBatchId) {
        throw new Error('请先选择导入批次。');
      }
      return lexicalPairService.commitImportBatch(selectedBatchId);
    },
    onSuccess: async (result) => {
      setFeedback(`批次 #${result.batchId} 已提交导入，后台正在处理。`);
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batches'] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batch-detail', result.batchId] });
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batch-rows', result.batchId] });
    },
    onError: (error) => {
      setFeedback(translateImportMessage(error instanceof Error ? error.message : '提交导入失败。'));
    },
  });

  const reindexBatchMutation = useMutation({
    mutationFn: () => {
      if (!selectedBatchId) {
        throw new Error('请先选择导入批次。');
      }
      return lexicalPairService.reindexImportBatch(selectedBatchId);
    },
    onSuccess: async (result) => {
      if (!selectedBatchId) {
        return;
      }
      setReindexJobIdsByBatch((current) => ({ ...current, [selectedBatchId]: result.jobId }));
      setSettledReindexJobs((current) => ({ ...current, [result.jobId]: false }));
      setFeedback(`已提交批次 #${selectedBatchId} 的定向重建任务，任务 #${result.jobId}。`);
      await queryClient.invalidateQueries({ queryKey: ['lexical-import-batch-detail', selectedBatchId] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : '批次定向重建提交失败。');
    },
  });

  const handleDownloadFile = async () => {
    if (!selectedBatchId || !batchDetailQuery.data) {
      return;
    }
    try {
      const blob = await lexicalPairService.downloadImportFile(selectedBatchId);
      saveBlob(blob, batchDetailQuery.data.originalFilename);
    } catch (error) {
      setFeedback(translateImportMessage(error instanceof Error ? error.message : '原文件下载失败。'));
    }
  };

  const batchTotalPages = Math.max(1, Math.ceil((batchesQuery.data?.total || 0) / batchPageSize));
  const rowTotalPages = Math.max(1, Math.ceil((rowsQuery.data?.total || 0) / rowPageSize));
  const selectedBatch = batchDetailQuery.data;
  const batchStatusMeta = buildBatchStatusMeta(selectedBatch?.status);

  React.useEffect(() => {
    if (batchPageNo > batchTotalPages) {
      setBatchPageNo(batchTotalPages);
    }
  }, [batchPageNo, batchTotalPages]);

  React.useEffect(() => {
    if (rowPageNo > rowTotalPages) {
      setRowPageNo(rowTotalPages);
    }
  }, [rowPageNo, rowTotalPages]);

  return (
    <div className="space-y-8">
      {source && (
        <div className="rounded-[1.8rem] border border-slate-200/80 bg-white/70 px-5 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/70">
          当前从教师工作台进入。导入筛选和选中的批次会同步回 URL，方便你刷新后继续处理同一批次。
        </div>
      )}

      <Panel
        title="批量导入中心"
        description="支持 CSV / XLSX 上传、后台解析、可恢复草稿、逐行修正与异步正式导入。导入完成后数据会先进入词对库，后续还要接到模板或词表。"
        actions={
          <div className="rounded-[1.5rem] border border-slate-200/70 bg-white/70 px-4 py-3 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
            <FileSpreadsheet size={16} className="mb-2 text-primary" />
            支持 CSV / XLSX，单文件上限 50MB。上传后先生成草稿，不会直接暴露到学生端。
          </div>
        }
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          className="hidden"
          onChange={(event) => setUploadFile(event.target.files?.[0] || null)}
        />

        <div className="grid gap-4">
          <div className="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/50 p-6 dark:border-white/15 dark:bg-white/[0.02]">
            <div className="text-sm font-bold text-slate-900 dark:text-white">待上传文件</div>
            <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
              {uploadFile ? `${uploadFile.name} · ${formatFileSize(uploadFile.size)}` : '选择 CSV / XLSX 文件后创建导入批次'}
            </div>
            <div className="mt-2 text-xs text-slate-400 dark:text-white/30">
              文件上传后不会直接入库，而是先生成草稿并进入人工确认。
            </div>
            <div className="mt-5 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
              >
                选择文件
              </button>
              <button
                type="button"
                onClick={() => createBatchMutation.mutate()}
                disabled={createBatchMutation.isPending}
                className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:opacity-60"
              >
                <Upload size={16} />
                {createBatchMutation.isPending ? '上传中...' : '创建导入批次'}
              </button>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">处理方式</div>
              <div className="mt-3 text-sm leading-6 text-slate-600 dark:text-white/55">上传后后台解析，生成可恢复草稿；确认后异步正式导入。</div>
            </div>
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">文件留档</div>
              <div className="mt-3 text-sm leading-6 text-slate-600 dark:text-white/55">原始文件和导入记录都会保留，可从历史记录中下载回看。</div>
            </div>
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">草稿编辑</div>
              <div className="mt-3 text-sm leading-6 text-slate-600 dark:text-white/55">支持逐行修正、跳过和重新提交，不会影响已经成功导入的行。</div>
            </div>
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-5 dark:border-white/10 dark:bg-white/[0.03]">
              <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">后续接入</div>
              <div className="mt-3 text-sm leading-6 text-slate-600 dark:text-white/55">导入完成后只会进入词对库。若要进入学生链路，还需继续配置模板或词表。</div>
            </div>
          </div>
        </div>

        {feedback && (
          <div className="rounded-[1.8rem] border border-primary/20 bg-primary/5 px-5 py-4 text-sm text-slate-700 dark:text-white/75">
            {feedback}
          </div>
        )}
      </Panel>

      <div className="grid gap-8">
        <Panel title="导入历史" description="教师查看自己的上传记录；管理员可按操作者筛选全部批次。">
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField
              label="批次状态"
              value={batchStatus}
              onChange={(value) => {
                setBatchStatus(value);
                if (value === 'FAILED') {
                  setView('failed');
                  return;
                }
                if (value === 'PARSING' || value === 'DRAFT' || value === 'IMPORTING') {
                  setView('pending');
                  return;
                }
                setView('all');
              }}
              type="select"
              options={batchStatusOptions}
            />
            <TextField label="文件名检索" value={batchKeyword} onChange={setBatchKeyword} />
            {isAdmin && (
              <TextField
                label="操作者"
                value={batchOwnerUserId}
                onChange={setBatchOwnerUserId}
                type="select"
                options={[
                  { value: '', label: '全部操作者' },
                  ...((usersQuery.data?.records || []).map((user) => ({
                    value: String(user.id),
                    label: user.displayName || user.username,
                  })) || []),
                ]}
              />
            )}
            <TextField
              label="每页数量"
              value={String(batchPageSize)}
              onChange={(value) => {
                setBatchPageSize(Number(value));
                setBatchPageNo(1);
              }}
              type="select"
              options={[12, 24, 48].map((value) => ({ value: String(value), label: `每页 ${value} 条` }))}
            />
          </div>

          <div className="space-y-4">
            {(batchesQuery.data?.records || []).map((batch) => {
              const meta = buildBatchStatusMeta(batch.status);
              return (
                <button
                  key={batch.id}
                  type="button"
                  onClick={() => {
                    setSelectedBatchId(batch.id);
                    setRowPageNo(1);
                  }}
                  className={`w-full rounded-[1.8rem] border p-5 text-left transition ${
                    selectedBatchId === batch.id
                      ? 'border-primary/30 bg-primary/5'
                      : 'border-slate-200/70 bg-white/60 hover:border-primary/20 dark:border-white/10 dark:bg-white/[0.03]'
                  }`}
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div className="text-base font-black text-slate-900 dark:text-white">{batch.originalFilename}</div>
                      <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                        {batch.sourceFormat} · {formatFileSize(batch.fileSizeBytes)} · 创建于 {formatDateTime(batch.createdAt)}
                      </div>
                      <div className="mt-2 text-xs text-slate-400 dark:text-white/30">
                        {isAdmin ? `操作者 ${batch.ownerDisplayName || batch.ownerUserId}` : `导入批次 #${batch.id}`}
                      </div>
                    </div>
                    <div className={`rounded-full border px-3 py-1 text-xs font-bold ${meta.className}`}>{meta.label}</div>
                  </div>
                  <div className="mt-4 grid gap-2 text-xs text-slate-500 dark:text-white/45 sm:grid-cols-2">
                    <div>总行数 {batch.totalRows}</div>
                    <div>可导入 {batch.readyRows}</div>
                    <div>需修正 {batch.invalidRows}</div>
                    <div>已导入 {batch.importedRows}</div>
                  </div>
                  {batch.errorMessage && (
                    <div className="mt-3 text-sm text-rose-500">{translateImportMessage(batch.errorMessage)}</div>
                  )}
                </button>
              );
            })}

            {!batchesQuery.isLoading && !batchesQuery.data?.records.length && (
              <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-6 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                当前筛选条件下没有导入记录。
              </div>
            )}
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 rounded-[1.6rem] border border-slate-200/70 bg-white/55 px-4 py-3 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
            <div>
              第 {batchPageNo} / {batchTotalPages} 页
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setBatchPageNo((current) => Math.max(1, current - 1))}
                disabled={batchPageNo <= 1 || batchesQuery.isFetching}
                className="rounded-full border border-slate-200/70 px-4 py-2 disabled:opacity-40 dark:border-white/10"
              >
                上一页
              </button>
              <button
                type="button"
                onClick={() => setBatchPageNo((current) => Math.min(batchTotalPages, current + 1))}
                disabled={batchPageNo >= batchTotalPages || batchesQuery.isFetching}
                className="rounded-full border border-slate-200/70 px-4 py-2 disabled:opacity-40 dark:border-white/10"
              >
                下一页
              </button>
            </div>
          </div>
        </Panel>

        <Panel
          title="批次详情"
          description="解析完成后可查看统计、下载原文件、逐行修正草稿，并在确认后执行正式导入。"
          actions={
            selectedBatch && (
              <div className={`rounded-full border px-4 py-2 text-sm font-bold ${batchStatusMeta.className}`}>
                批次 #{selectedBatch.id} · {batchStatusMeta.label}
              </div>
            )
          }
        >
          {!selectedBatchId && (
            <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-8 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
              先从左侧选择一个导入批次，或直接上传新文件。
            </div>
          )}

          {selectedBatch && (
            <div className="space-y-6">
              <div className="flex flex-wrap items-start justify-between gap-4 rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                <div>
                  <div className="text-lg font-black text-slate-900 dark:text-white">{selectedBatch.originalFilename}</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    {selectedBatch.sourceFormat} · {formatFileSize(selectedBatch.fileSizeBytes)} · SHA-256 {selectedBatch.fileSha256 || '--'}
                  </div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                    操作者 {selectedBatch.ownerDisplayName || selectedBatch.ownerUserId} · 创建于 {formatDateTime(selectedBatch.createdAt)}
                  </div>
                  <div className="mt-2 text-xs text-slate-400 dark:text-white/30">
                    解析开始 {formatDateTime(selectedBatch.parserJobStartedAt)} · 解析结束 {formatDateTime(selectedBatch.parserJobFinishedAt)} · 导入结束 {formatDateTime(selectedBatch.importJobFinishedAt)}
                  </div>
                </div>
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => void handleDownloadFile()}
                    className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
                  >
                    <span className="inline-flex items-center gap-2">
                      <Download size={14} />
                      下载原文件
                    </span>
                  </button>
                  <button
                    type="button"
                    onClick={() => commitBatchMutation.mutate()}
                    disabled={commitBatchMutation.isPending || isBatchProcessing(selectedBatch) || selectedBatch.readyRows <= 0}
                    className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:opacity-60"
                  >
                    <RefreshCw size={14} className={isBatchProcessing(selectedBatch) ? 'animate-pulse' : ''} />
                    {commitBatchMutation.isPending || selectedBatch.status === 'IMPORTING' ? '提交中...' : '正式导入可用行'}
                  </button>
                  {isAdmin && (
                    <button
                      type="button"
                      onClick={() => reindexBatchMutation.mutate()}
                      disabled={reindexBatchMutation.isPending || selectedBatch.importedRows <= 0}
                      className="rounded-full border border-primary/20 bg-primary/10 px-5 py-3 text-sm font-bold text-primary disabled:opacity-60"
                    >
                      {reindexBatchMutation.isPending ? '提交重建中...' : '重建本批索引'}
                    </button>
                  )}
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <MetricCard label="总行数" value={selectedBatch.totalRows} className="border-slate-200/70 bg-white/60 text-slate-900 dark:border-white/10 dark:bg-white/[0.03] dark:text-white" />
                <MetricCard label="可导入" value={selectedBatch.readyRows} className="border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400" />
                <MetricCard label="需修正" value={selectedBatch.invalidRows} className="border-rose-500/20 bg-rose-500/5 text-rose-500" />
                <MetricCard label="已跳过" value={selectedBatch.skippedRows} className="border-slate-200/70 bg-white/60 text-slate-700 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/75" />
                <MetricCard label="已导入" value={selectedBatch.importedRows} className="border-primary/20 bg-primary/5 text-primary" />
              </div>

              <div className="space-y-4 rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                <div>
                  <div className="text-xs font-bold uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">知识同步概览</div>
                  <div className="mt-2 text-sm text-slate-500 dark:text-white/45">{buildEmbeddingSyncHint(selectedBatch)}</div>
                  <div className="mt-2 text-xs text-slate-400 dark:text-white/30">
                    最近成功嵌入 {formatDateTime(selectedBatch.latestEmbeddedAt)}
                  </div>
                </div>
                <div className="grid gap-4 sm:grid-cols-3">
                  <MetricCard label="待嵌入" value={selectedBatch.pendingEmbeddingCount} className="border-amber-500/20 bg-amber-500/5 text-amber-600 dark:text-amber-400" />
                  <MetricCard label="已嵌入" value={selectedBatch.embeddedCount} className="border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400" />
                  <MetricCard label="嵌入失败" value={selectedBatch.failedEmbeddingCount} className="border-rose-500/20 bg-rose-500/5 text-rose-500" />
                </div>
                {isAdmin && (
                  <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-4 py-4 text-sm text-slate-500 dark:border-white/10 dark:bg-slate-950/30 dark:text-white/55">
                    <div className="font-bold text-slate-900 dark:text-white">
                      最近一次定向重建任务 {activeReindexJobId ? `#${activeReindexJobId}` : '--'}
                    </div>
                    <div className="mt-2">{buildRagJobSummary(reindexJobQuery.data)}</div>
                    {reindexJobQuery.data?.finishedAt && (
                      <div className="mt-2 text-xs text-slate-400 dark:text-white/30">
                        结束时间 {formatDateTime(reindexJobQuery.data.finishedAt)}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {selectedBatch.errorMessage && (
                <div className="rounded-[1.8rem] border border-rose-500/20 bg-rose-500/5 px-5 py-4 text-sm text-rose-500">
                  {translateImportMessage(selectedBatch.errorMessage)}
                </div>
              )}

              {selectedBatch.status === 'PARSING' && (
                <div className="rounded-[1.8rem] border border-sky-500/20 bg-sky-500/5 px-5 py-4 text-sm text-sky-600 dark:text-sky-400">
                  文件正在后台解析中，页面会每 2 秒自动刷新一次状态。
                </div>
              )}

              {selectedBatch.status !== 'PARSING' && (
                <div className="space-y-6">
                  <div className="flex flex-wrap gap-4">
                    <TextField label="行状态筛选" value={rowStatus} onChange={setRowStatus} type="select" options={rowStatusOptions} />
                    <TextField
                      label="每页行数"
                      value={String(rowPageSize)}
                      onChange={(value) => {
                        setRowPageSize(Number(value));
                        setRowPageNo(1);
                      }}
                      type="select"
                      options={[20, 50, 100].map((value) => ({ value: String(value), label: `每页 ${value} 行` }))}
                    />
                  </div>

                  <div className="overflow-x-auto rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/[0.03]">
                    <table className="data-table-compact w-full min-w-[760px] text-left text-sm" aria-label="Import rows">
                      <caption className="sr-only">Import rows with validation status and edit actions</caption>
                      <thead className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">
                        <tr>
                          <th scope="col" className="pb-3">行号</th>
                          <th scope="col" className="pb-3">词对</th>
                          <th scope="col" className="pb-3">状态</th>
                          <th scope="col" className="pb-3">错误 / 结果</th>
                          <th scope="col" className="pb-3">操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(rowsQuery.data?.records || []).map((row) => {
                          const meta = buildRowStatusMeta(row.status);
                          const pairLabel = [row.draft.englishWord, row.draft.frenchWord].filter(Boolean).join(' / ') || '未填写词对';
                          return (
                            <tr key={row.id} className="border-t border-slate-200/70 dark:border-white/10">
                              <td className="py-3 text-slate-500 dark:text-white/45">{row.rowNumber}</td>
                              <td className="py-3">
                                <div className="font-semibold text-slate-900 dark:text-white">{pairLabel}</div>
                                <div className="mt-1 text-xs text-slate-400 dark:text-white/30">{row.draft.chineseGloss || '--'}</div>
                              </td>
                              <td className="py-3">
                                <span className={`rounded-full border px-3 py-1 text-xs font-bold ${meta.className}`}>{meta.label}</span>
                              </td>
                              <td className="py-3 text-slate-500 dark:text-white/45">
                                {row.validationErrors.length > 0 ? (
                                  <div className="space-y-1">
                                    {row.validationErrors.slice(0, 2).map((error) => (
                                      <div key={error} className="text-rose-500">
                                        {translateImportMessage(error)}
                                      </div>
                                    ))}
                                  </div>
                                ) : row.importedLexicalPairId ? (
                                  <div className="text-emerald-600 dark:text-emerald-400">已导入词对 #{row.importedLexicalPairId}</div>
                                ) : (
                                  '--'
                                )}
                              </td>
                              <td className="py-3">
                                <button
                                  type="button"
                                  onClick={() => {
                                    setSelectedRowId(row.id);
                                    setRowForm(toRowForm(row));
                                  }}
                                  className={`rounded-full border px-4 py-2 text-xs font-bold ${
                                    selectedRowId === row.id
                                      ? 'border-primary/30 bg-primary/10 text-primary'
                                      : 'border-slate-200/70 text-slate-500 dark:border-white/10 dark:text-white/45'
                                  }`}
                                >
                                  编辑
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>

                  <Pagination
                    page={rowPageNo}
                    pageCount={rowTotalPages}
                    onPageChange={setRowPageNo}
                    disabled={rowsQuery.isFetching}
                    label={t('ui.pagination.rows')}
                    previousLabel={t('ui.pagination.previous')}
                    nextLabel={t('ui.pagination.next')}
                    className="rounded-lg border border-border-subtle bg-surface-sunken px-4 py-2"
                  />

                  {selectedRow && (
                    <div className="space-y-4 rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03]">
                      <div className="flex flex-wrap items-start justify-between gap-4">
                        <div>
                          <div className="text-base font-black text-slate-900 dark:text-white">编辑第 {selectedRow.rowNumber} 行</div>
                          <div className="mt-2 text-sm text-slate-500 dark:text-white/45">
                            当前状态 {buildRowStatusMeta(selectedRow.status).label}
                            {selectedRow.draft.lexicalPairType ? ` · ${lexicalPairTypeLabel(selectedRow.draft.lexicalPairType)}` : ''}
                            {selectedRow.draft.defaultContextSupport
                              ? ` · ${contextLevelLabel(selectedRow.draft.defaultContextSupport)}`
                              : ''}
                          </div>
                        </div>
                        <label className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-4 py-2 text-sm dark:border-white/10">
                          <input
                            type="checkbox"
                            checked={Boolean(rowForm.skipped)}
                            onChange={(event) => setRowForm((current) => ({ ...current, skipped: event.target.checked }))}
                          />
                          跳过该行
                        </label>
                      </div>

                      {selectedRow.validationErrors.length > 0 && !rowForm.skipped && (
                        <div className="rounded-[1.4rem] border border-rose-500/20 bg-rose-500/5 px-4 py-4 text-sm text-rose-500">
                          {selectedRow.validationErrors.map((error) => (
                            <div key={error}>{translateImportMessage(error)}</div>
                          ))}
                        </div>
                      )}

                      {rowFieldGroups.map((group) => (
                        <div key={group.title} className="space-y-4">
                          <div className="text-sm font-bold text-slate-900 dark:text-white">{group.title}</div>
                          <div className="grid gap-4 md:grid-cols-2">
                            {group.fields.map((field) => (
                              <TextField
                                key={String(field.key)}
                                label={field.label}
                                value={rowForm[field.key]}
                                onChange={(value) => setRowForm((current) => ({ ...current, [field.key]: normalizeEditableRowFieldValue(field.key, value) }))}
                                type={field.type}
                                options={field.options}
                              />
                            ))}
                          </div>
                        </div>
                      ))}

                      <div className="flex flex-wrap gap-3">
                        <button
                          type="button"
                          onClick={() => updateRowMutation.mutate()}
                          disabled={updateRowMutation.isPending || selectedRow.status === 'IMPORTED'}
                          className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:opacity-60"
                        >
                          <Save size={16} />
                          {updateRowMutation.isPending ? '保存中...' : '保存草稿行'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setRowForm(toRowForm(selectedRow))}
                          className="rounded-full border border-slate-200 px-4 py-3 text-sm dark:border-white/10"
                        >
                          恢复当前行
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {batchDetailQuery.isLoading && (
            <div className="flex items-center gap-3 rounded-[1.8rem] border border-slate-200/70 bg-white/60 px-5 py-6 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                    <LoaderCircle size={16} className="animate-pulse" />
              正在加载批次详情...
            </div>
          )}
        </Panel>
      </div>
    </div>
  );
};

export default LexicalImportCenter;

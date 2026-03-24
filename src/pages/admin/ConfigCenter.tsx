import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, CheckCircle2, Info, LoaderCircle, Play, RefreshCw, Save, ShieldCheck } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { formatDateTime } from '@/lib/format';
import { adminService } from '@/lib/services';
import type {
  AdminAiConfigSaveRequest,
  AdminAiConfigViewVO,
  AdminAiSecretFieldVO,
  AdminOutboxRecordVO,
  AiGatewayHealthResponse,
  AiOpsConfigPayload,
  AiOpsConfigValidationResponse,
  AiOpsProviderDefinition,
  RagReindexJobResponse,
  RagReindexRequest,
} from '@/lib/contracts';

type ConfigTab = 'provider' | 'resilience' | 'rag' | 'operations';
type ProviderSecretKey = 'chatApiKey' | 'embeddingApiKey' | 'rerankApiKey';

type SecretEditorState = {
  retainExisting: boolean;
  value: string;
};

type ProviderSecretEditorMap = Record<ProviderSecretKey, SecretEditorState>;

type SecretEditorMap = {
  providers: Record<string, ProviderSecretEditorMap>;
  appServerInternalToken: SecretEditorState;
};

const tabs: Array<{ key: ConfigTab; label: string }> = [
  { key: 'provider', label: '模型接入' },
  { key: 'resilience', label: '稳定性' },
  { key: 'rag', label: 'RAG 参数' },
  { key: 'operations', label: '运维操作' },
];

const providerSecretMeta: Record<ProviderSecretKey, { label: string; hint: string }> = {
  chatApiKey: { label: 'Chat API Key', hint: '当前 provider 的文本生成密钥。active 与 fallback 会各自独立生效。' },
  embeddingApiKey: { label: 'Embedding API Key', hint: '当前 provider 的向量化服务密钥。当前 pgvector schema 固定为 1024 维。' },
  rerankApiKey: { label: 'Rerank API Key', hint: '当前 provider 的重排服务密钥。会随 provider failover 一起切换。' },
};

const appServerSecretMeta = {
  label: 'App Server Internal Token',
  hint: 'ai-gateway 拉取词条导出、配置和回源数据时使用的内部令牌。',
};

const finalStatuses = new Set(['SUCCEEDED', 'FAILED']);
const runningStatuses = new Set(['RUNNING', 'PROCESSING', 'IN_PROGRESS']);
const queuedStatuses = new Set(['PENDING', 'QUEUED', 'SUBMITTED', 'CREATED']);

const fieldTokenLabels: Record<string, string> = {
  provider: 'Provider',
  activeProvider: '当前 Provider',
  fallbackProvider: '备用 Provider',
  providers: 'Provider 定义',
  chat: 'Chat',
  embedding: 'Embedding',
  rerank: 'Rerank',
  baseUrl: '接口地址',
  apiKey: 'API Key',
  model: '模型名',
  timeout: '超时',
  maxTokens: '最大输出 Tokens',
  temperature: '温度',
  dimension: '向量维度',
  resilience: '稳定性',
  maxAttempts: '最大重试次数',
  waitDuration: '重试等待时长',
  failureRateThreshold: '熔断失败率阈值',
  slidingWindowSize: '滑动窗口大小',
  openStateDuration: '熔断打开时长',
  rag: 'RAG',
  appServer: 'App Server',
  internalToken: '内部令牌',
  connectTimeout: '连接超时',
  readTimeout: '读取超时',
  ingestion: '导入链路',
  retrieval: '召回链路',
  exportPageSize: '导出分页大小',
  embeddingBatchSize: '向量批大小',
  recallTopK: '初筛 Top K',
  recallThreshold: '初筛阈值',
  rerankTopN: '重排 Top N',
  rerankThreshold: '重排阈值',
  finalTopK: '最终返回 Top K',
  mode: '执行模式',
  sourceTypes: '数据源类型',
  sourceIds: '数据源 ID',
  forceReembed: '强制重嵌入',
};

const tabDescriptions: Record<ConfigTab, string> = {
  provider: '配置 active / fallback provider，以及每个 provider 独立的 chat、embedding、rerank 参数和密钥。',
  resilience: '控制 retry 与 circuit breaker，决定可重试故障时是否切备用 provider。',
  rag: '控制 app-server 回源、嵌入批次以及召回阈值，影响检索质量与吞吐。',
  operations: '用于健康检查、producer outbox 重放和 RAG reindex，适合配置变更后的运维验证。',
};

const statLabelMap: Record<string, string> = {
  processedCount: '已处理',
  successCount: '成功',
  failedCount: '失败',
  skippedCount: '跳过',
  totalCount: '总量',
  pageCount: '分页数',
  durationMs: '耗时(ms)',
  exportedCount: '已导出',
  embeddedCount: '已嵌入',
  upsertedCount: '已写入',
};

function cloneConfig<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function createSecretEditor(configured: boolean): SecretEditorState {
  return { retainExisting: configured, value: '' };
}

function createProviderSecretEditors(view: AdminAiConfigViewVO, providerName: string): ProviderSecretEditorMap {
  const providerSecrets = view.secrets.providers?.[providerName];
  return {
    chatApiKey: createSecretEditor(Boolean(providerSecrets?.chatApiKey.configured)),
    embeddingApiKey: createSecretEditor(Boolean(providerSecrets?.embeddingApiKey.configured)),
    rerankApiKey: createSecretEditor(Boolean(providerSecrets?.rerankApiKey.configured)),
  };
}

function buildSecretEditors(view: AdminAiConfigViewVO): SecretEditorMap {
  const providerNames = Array.from(new Set([
    ...Object.keys(view.config.provider.providers || {}),
    ...Object.keys(view.secrets.providers || {}),
  ]));

  return {
    providers: Object.fromEntries(providerNames.map((providerName) => [providerName, createProviderSecretEditors(view, providerName)])),
    appServerInternalToken: createSecretEditor(view.secrets.appServerInternalToken.configured),
  };
}

function buildSavePayload(config: AiOpsConfigPayload, secrets: SecretEditorMap): AdminAiConfigSaveRequest {
  return {
    config,
    secrets: {
      providers: Object.fromEntries(
        Object.entries(secrets.providers).map(([providerName, providerSecrets]) => [
          providerName,
          {
            chatApiKey: providerSecrets.chatApiKey,
            embeddingApiKey: providerSecrets.embeddingApiKey,
            rerankApiKey: providerSecrets.rerankApiKey,
          },
        ])
      ),
      appServerInternalToken: secrets.appServerInternalToken,
    },
  };
}

const SectionCard: React.FC<{ title: string; description?: string; children: React.ReactNode }> = ({ title, description, children }) => (
  <section className="rounded-[2.2rem] liquid-glass-panel p-6 md:p-8 space-y-6">
    <div className="space-y-2">
      <h2 className="text-lg font-black text-slate-900 dark:text-white">{title}</h2>
      {description && <p className="text-sm text-slate-500 dark:text-white/45 max-w-3xl">{description}</p>}
    </div>
    {children}
  </section>
);

const FieldGrid: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">{children}</div>
);

const FieldCard: React.FC<{
  label: string;
  hint?: string;
  detail?: React.ReactNode;
  children: React.ReactNode;
}> = ({ label, hint, detail, children }) => (
  <label className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 bg-white/55 dark:bg-white/[0.03] px-4 py-4 space-y-3 block">
    <div>
      <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{label}</div>
      {hint && (
        <div className="mt-3 inline-flex items-start gap-2 rounded-2xl border border-sky-500/15 bg-sky-500/[0.04] px-3 py-2 text-xs leading-5 text-slate-600 dark:text-sky-100/80">
          <Info size={14} className="mt-0.5 shrink-0 text-sky-500 dark:text-sky-300" />
          <span>{hint}</span>
        </div>
      )}
    </div>
    {children}
    {detail && <div className="rounded-2xl border border-slate-200/60 bg-white/70 px-3 py-3 text-xs leading-5 text-slate-500 dark:border-white/10 dark:bg-slate-950/25 dark:text-white/45">{detail}</div>}
  </label>
);

const TextInput: React.FC<{
  value: string | number;
  onChange: (value: string) => void;
  disabled?: boolean;
  type?: 'text' | 'number' | 'password';
  placeholder?: string;
  step?: string;
}> = ({ value, onChange, disabled, type = 'text', placeholder, step }) => (
  <input
    type={type}
    step={step}
    value={value}
    onChange={(event) => onChange(event.target.value)}
    disabled={disabled}
    placeholder={placeholder}
    className="w-full rounded-2xl bg-white/80 dark:bg-slate-950/45 border border-slate-200 dark:border-white/10 px-4 py-3 text-sm outline-none focus:border-primary/50 disabled:opacity-60 disabled:cursor-not-allowed"
  />
);

const TabButton: React.FC<{ active: boolean; label: string; onClick: () => void }> = ({ active, label, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`px-4 py-3 rounded-2xl text-sm font-bold transition-all border ${
      active
        ? 'bg-primary/10 text-primary border-primary/20'
        : 'bg-white/50 dark:bg-white/[0.03] text-slate-500 dark:text-white/45 border-slate-200/70 dark:border-white/10'
    }`}
  >
    {label}
  </button>
);

const SelectInput: React.FC<{
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  options: Array<{ value: string; label: string }>;
}> = ({ value, onChange, disabled, options }) => (
  <select
    value={value}
    onChange={(event) => onChange(event.target.value)}
    disabled={disabled}
    className="w-full rounded-2xl bg-white/80 dark:bg-slate-950/45 border border-slate-200 dark:border-white/10 px-4 py-3 text-sm outline-none focus:border-primary/50 disabled:opacity-60 disabled:cursor-not-allowed"
  >
    {options.map((option) => (
      <option key={option.value} value={option.value}>
        {option.label}
      </option>
    ))}
  </select>
);

const HealthBadge: React.FC<{ healthy: boolean; label: string }> = ({ healthy, label }) => (
  <div
    className={`rounded-2xl px-4 py-3 border text-sm font-semibold ${
      healthy
        ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
        : 'border-rose-500/20 bg-rose-500/5 text-rose-500'
    }`}
  >
    {label}
  </div>
);

function humanizeFieldName(field: string): string {
  return field
    .replace(/^config\./, '')
    .split('.')
    .map((segment) => fieldTokenLabels[segment] || segment)
    .join(' / ');
}

function translateConfigMessage(message: string): string {
  const trimmed = message.trim();
  if (!trimmed) {
    return '操作失败，请检查配置项。';
  }
  if (trimmed.includes('must not be blank')) {
    return '存在必填字段为空，请补齐后重试。';
  }
  if (trimmed.includes('must not be null')) {
    return '存在未填写的必填字段。';
  }
  if (trimmed.includes('must be greater than')) {
    return '存在数值过小的字段，请调整后再校验。';
  }
  if (trimmed.includes('must be less than')) {
    return '存在数值过大的字段，请调整后再校验。';
  }
  return trimmed;
}

function buildReindexStatusMeta(job?: RagReindexJobResponse | null): {
  label: string;
  description: string;
  progress: number;
  tone: 'success' | 'warning' | 'info' | 'neutral';
} {
  const status = (job?.status || '').toUpperCase();
  if (status === 'SUCCEEDED') {
    return {
      label: '已完成',
      description: '向量重建已结束，可以回到业务页抽样验证检索结果。',
      progress: 100,
      tone: 'success',
    };
  }
  if (status === 'FAILED') {
    return {
      label: '执行失败',
      description: '任务已终止，请结合错误信息和统计项排查。',
      progress: 100,
      tone: 'warning',
    };
  }
  if (runningStatuses.has(status)) {
    return {
      label: '执行中',
      description: '后台正在分页拉取词条、计算 embedding 并写入向量库。',
      progress: 68,
      tone: 'info',
    };
  }
  if (queuedStatuses.has(status)) {
    return {
      label: '已提交',
      description: '任务已入队，等待后台 worker 开始处理。',
      progress: 28,
      tone: 'info',
    };
  }
  return {
    label: status || '未开始',
    description: '尚未提交 reindex 任务。',
    progress: 0,
    tone: 'neutral',
  };
}

function normalizeStatValue(value: unknown): string {
  if (value === null || value === undefined) {
    return '--';
  }
  if (Array.isArray(value)) {
    return value.join(', ') || '--';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

function formatStats(stats?: Record<string, unknown> | null): Array<{ key: string; label: string; value: string }> {
  return Object.entries(stats || {}).map(([key, value]) => ({
    key,
    label: statLabelMap[key] || humanizeFieldName(key),
    value: normalizeStatValue(value),
  }));
}

function statusTone(status: string): 'success' | 'warning' | 'info' | 'neutral' {
  const normalized = status.toUpperCase();
  if (normalized === 'PUBLISHED') {
    return 'success';
  }
  if (normalized === 'FAILED') {
    return 'warning';
  }
  if (normalized === 'IN_PROGRESS' || normalized === 'PENDING') {
    return 'info';
  }
  return 'neutral';
}

function statusClasses(tone: 'success' | 'warning' | 'info' | 'neutral'): string {
  if (tone === 'success') {
    return 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400';
  }
  if (tone === 'warning') {
    return 'border-rose-500/20 bg-rose-500/5 text-rose-500';
  }
  if (tone === 'info') {
    return 'border-sky-500/20 bg-sky-500/5 text-sky-600 dark:text-sky-300';
  }
  return 'border-slate-200/70 bg-white/70 text-slate-500 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/45';
}

function getProviderSecretField(view: AdminAiConfigViewVO, providerName: string, key: ProviderSecretKey): AdminAiSecretFieldVO | null {
  return view.secrets.providers?.[providerName]?.[key] || null;
}

const AdminConfigCenterPage: React.FC = () => {
  const queryClient = useQueryClient();
  const configQuery = useQuery({
    queryKey: ['admin-ai-config'],
    queryFn: ({ signal }) => adminService.getAiConfig({ signal }),
  });

  const [activeTab, setActiveTab] = React.useState<ConfigTab>('provider');
  const [editing, setEditing] = React.useState(false);
  const [config, setConfig] = React.useState<AiOpsConfigPayload | null>(null);
  const [secrets, setSecrets] = React.useState<SecretEditorMap | null>(null);
  const [validation, setValidation] = React.useState<AiOpsConfigValidationResponse | null>(null);
  const [feedback, setFeedback] = React.useState<{ tone: 'success' | 'error'; message: string } | null>(null);
  const [healthState, setHealthState] = React.useState<AiGatewayHealthResponse | null>(null);
  const [reindexForm, setReindexForm] = React.useState<RagReindexRequest>({
    mode: 'INCREMENTAL',
    sourceTypes: ['LEXICAL_PAIR'],
    sourceIds: [],
    forceReembed: false,
  });
  const [jobId, setJobId] = React.useState<number | null>(null);
  const [pollJob, setPollJob] = React.useState(false);
  const [outboxStatus, setOutboxStatus] = React.useState('FAILED');
  const [outboxLimit, setOutboxLimit] = React.useState('20');
  const [replayingOutboxId, setReplayingOutboxId] = React.useState<number | null>(null);

  React.useEffect(() => {
    if (!configQuery.data) {
      return;
    }
    setConfig(cloneConfig(configQuery.data.config));
    setSecrets(buildSecretEditors(configQuery.data));
    setValidation(null);
    setFeedback(null);
  }, [configQuery.data]);

  const validateMutation = useMutation({
    mutationFn: (payload: AdminAiConfigSaveRequest) => adminService.validateAiConfig(payload),
    onSuccess: (response) => {
      setValidation(response);
      setFeedback({
        tone: response.valid ? 'success' : 'error',
        message: response.valid ? '配置校验通过。' : '配置校验未通过，请修正字段错误。',
      });
    },
    onError: (error: Error) => {
      setFeedback({ tone: 'error', message: translateConfigMessage(error.message) });
    },
  });

  const saveMutation = useMutation({
    mutationFn: (payload: AdminAiConfigSaveRequest) => adminService.saveAiConfig(payload),
    onSuccess: (response) => {
      setEditing(false);
      setValidation(null);
      setFeedback({ tone: 'success', message: '配置已保存并下发到 ai-gateway 运行时。' });
      setConfig(cloneConfig(response.config));
      setSecrets(buildSecretEditors(response));
      void queryClient.invalidateQueries({ queryKey: ['admin-ai-config'] });
    },
    onError: (error: Error) => {
      setFeedback({ tone: 'error', message: translateConfigMessage(error.message) });
    },
  });

  const healthMutation = useMutation({
    mutationFn: () => adminService.getAiHealth(),
    onSuccess: (response) => {
      setHealthState(response);
      setFeedback({ tone: 'success', message: '健康检查已刷新，可根据运行态结果继续排查。' });
    },
    onError: (error: Error) => {
      setFeedback({ tone: 'error', message: translateConfigMessage(error.message) });
    },
  });

  const reindexMutation = useMutation({
    mutationFn: (payload: RagReindexRequest) => adminService.triggerRagReindex(payload),
    onSuccess: (response) => {
      setJobId(response.jobId);
      setPollJob(true);
      setFeedback({ tone: 'success', message: `已提交 RAG reindex，任务 #${response.jobId} 已进入后台队列。` });
    },
    onError: (error: Error) => setFeedback({ tone: 'error', message: translateConfigMessage(error.message) }),
  });

  const replayOutboxMutation = useMutation({
    mutationFn: (id: number) => adminService.replayOutboxRecord(id),
    onMutate: (id) => {
      setReplayingOutboxId(id);
    },
    onSuccess: (record) => {
      setFeedback({ tone: 'success', message: `Outbox 事件 #${record.id} 已重置为 ${record.status}，等待 relay 重发。` });
      void queryClient.invalidateQueries({ queryKey: ['admin-ai-outbox'] });
    },
    onError: (error: Error) => {
      setFeedback({ tone: 'error', message: translateConfigMessage(error.message) });
    },
    onSettled: () => {
      setReplayingOutboxId(null);
    },
  });

  const reindexJobQuery = useQuery({
    queryKey: ['admin-ai-reindex-job', jobId],
    queryFn: ({ signal }) => adminService.getRagReindexJob(jobId as number, { signal }),
    enabled: jobId !== null,
    refetchInterval: pollJob ? 2000 : false,
  });

  const outboxQuery = useQuery({
    queryKey: ['admin-ai-outbox', outboxStatus, outboxLimit],
    queryFn: ({ signal }) =>
      adminService.getOutboxRecords(
        outboxStatus || undefined,
        Number(outboxLimit) > 0 ? Number(outboxLimit) : 20,
        { signal }
      ),
    refetchInterval: activeTab === 'operations' ? 5000 : false,
  });

  React.useEffect(() => {
    const data = reindexJobQuery.data;
    if (!data || !finalStatuses.has(data.status)) {
      return;
    }
    setPollJob(false);
    setFeedback({
      tone: data.status === 'SUCCEEDED' ? 'success' : 'error',
      message:
        data.status === 'SUCCEEDED'
          ? `RAG reindex #${data.jobId} 已完成。`
          : data.errorMessage || `RAG reindex #${data.jobId} 执行失败。`,
    });
  }, [reindexJobQuery.data]);

  const updateConfig = React.useCallback((updater: (current: AiOpsConfigPayload) => AiOpsConfigPayload) => {
    setConfig((current) => (current ? updater(current) : current));
  }, []);

  const updateProviderDefinition = React.useCallback((
    providerName: string,
    updater: (current: AiOpsProviderDefinition) => AiOpsProviderDefinition
  ) => {
    updateConfig((current) => {
      const existing = current.provider.providers[providerName];
      if (!existing) {
        return current;
      }
      return {
        ...current,
        provider: {
          ...current.provider,
          providers: {
            ...current.provider.providers,
            [providerName]: updater(existing),
          },
        },
      };
    });
  }, [updateConfig]);

  const updateProviderSecret = React.useCallback((providerName: string, key: ProviderSecretKey, patch: Partial<SecretEditorState>) => {
    setSecrets((current) => {
      if (!current) {
        return current;
      }
      const providerSecrets = current.providers[providerName] || {
        chatApiKey: createSecretEditor(false),
        embeddingApiKey: createSecretEditor(false),
        rerankApiKey: createSecretEditor(false),
      };
      return {
        ...current,
        providers: {
          ...current.providers,
          [providerName]: {
            ...providerSecrets,
            [key]: {
              ...providerSecrets[key],
              ...patch,
            },
          },
        },
      };
    });
  }, []);

  const updateAppServerSecret = React.useCallback((patch: Partial<SecretEditorState>) => {
    setSecrets((current) =>
      current
        ? {
            ...current,
            appServerInternalToken: {
              ...current.appServerInternalToken,
              ...patch,
            },
          }
        : current
    );
  }, []);

  const resetDraft = React.useCallback(() => {
    if (!configQuery.data) {
      return;
    }
    setConfig(cloneConfig(configQuery.data.config));
    setSecrets(buildSecretEditors(configQuery.data));
    setValidation(null);
    setFeedback(null);
  }, [configQuery.data]);

  const submitValidation = () => {
    if (!config || !secrets) {
      return;
    }
    setFeedback(null);
    validateMutation.mutate(buildSavePayload(config, secrets));
  };

  const submitSave = () => {
    if (!config || !secrets) {
      return;
    }
    setFeedback(null);
    saveMutation.mutate(buildSavePayload(config, secrets));
  };

  const currentIssues = validation?.issues ?? [];
  const validationNotices = validation?.notices ?? [];

  if (configQuery.error) {
    return (
      <div className="space-y-8 pb-20">
        <PageHeader title="运维管理员配置中心" subtitle="加载失败时不隐藏原因，直接显示后端返回错误。" />
        <div className="rounded-[2.5rem] border border-rose-500/20 bg-rose-500/5 p-8 text-rose-500">{configQuery.error.message}</div>
      </div>
    );
  }

  if (configQuery.isLoading || !config || !secrets) {
    return (
      <div className="space-y-8 pb-20">
        <PageHeader title="运维管理员配置中心" subtitle="正在加载 ai-gateway 当前配置和数据库覆盖配置。" />
        <div className="rounded-[2.5rem] liquid-glass-panel p-10 flex items-center gap-3 text-slate-500 dark:text-white/45">
          <LoaderCircle className="animate-spin" size={18} />
          <span>配置加载中...</span>
        </div>
      </div>
    );
  }

  const view = configQuery.data as AdminAiConfigViewVO;
  const providerEntries = Object.entries(config.provider.providers || {});
  const activeProviderDefinition = config.provider.providers?.[config.provider.activeProvider];
  const sourceMeta = `来源 ${view.source} · 版本 ${view.version ?? '--'} · 更新时间 ${formatDateTime(view.updatedAt)}`;
  const activeTabDescription = tabDescriptions[activeTab];
  const busyMessage = saveMutation.isPending
    ? '正在保存配置并热更新 ai-gateway 运行态，请勿重复提交。'
    : validateMutation.isPending
      ? '正在校验配置，校验结果会直接展示在当前页。'
      : pollJob && reindexJobQuery.data
        ? `RAG reindex #${reindexJobQuery.data.jobId} 正在执行，页面每 2 秒自动刷新一次状态。`
        : pollJob && jobId !== null
          ? `RAG reindex #${jobId} 正在执行，页面每 2 秒自动刷新一次状态。`
          : outboxQuery.isFetching && activeTab === 'operations'
            ? '正在刷新 producer outbox 状态。'
            : null;
  const reindexStatusMeta = buildReindexStatusMeta(reindexJobQuery.data);
  const reindexStats = formatStats(reindexJobQuery.data?.stats);
  const outboxRecords = outboxQuery.data || [];

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="运维管理员配置中心"
        subtitle="数据库配置覆盖默认 yml / env。推荐流程是先编辑、再校验、最后保存并做健康检查、outbox 检查或 reindex 验证。"
        actions={
          <div className="flex flex-wrap gap-3">
            {!editing && (
              <button
                type="button"
                onClick={() => {
                  setEditing(true);
                  setFeedback(null);
                }}
                className="btn-liquid px-5 py-3 text-white"
              >
                进入编辑
              </button>
            )}
            {editing && (
              <>
                <button
                  type="button"
                  onClick={submitValidation}
                  disabled={validateMutation.isPending || saveMutation.isPending}
                  className="rounded-2xl border border-slate-200 dark:border-white/10 px-5 py-3 text-sm font-bold text-slate-700 dark:text-white/80 bg-white/70 dark:bg-white/[0.04]"
                >
                  {validateMutation.isPending ? '校验中...' : '校验配置'}
                </button>
                <button
                  type="button"
                  onClick={submitSave}
                  disabled={saveMutation.isPending || validateMutation.isPending}
                  className="btn-liquid px-5 py-3 text-white inline-flex items-center gap-2"
                >
                  <Save size={16} />
                  {saveMutation.isPending ? '保存中...' : '保存并生效'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEditing(false);
                    resetDraft();
                  }}
                  className="rounded-2xl border border-slate-200 dark:border-white/10 px-5 py-3 text-sm font-bold text-slate-500 dark:text-white/45 bg-white/60 dark:bg-white/[0.03]"
                >
                  取消
                </button>
              </>
            )}
          </div>
        }
      />

      <section className="rounded-[2.2rem] liquid-glass-panel p-5 md:p-6 space-y-5">
        <div className="flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4">
          <div>
            <div className="text-[11px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">Effective Source</div>
            <div className="text-sm text-slate-600 dark:text-white/55 mt-2">{sourceMeta}</div>
          </div>
          <div className="flex flex-wrap gap-3">
            <HealthBadge healthy={Boolean(config.provider.activeProvider)} label={`activeProvider: ${config.provider.activeProvider || '--'}`} />
            <HealthBadge healthy={Boolean(config.provider.fallbackProvider) && config.provider.fallbackProvider !== config.provider.activeProvider} label={`fallbackProvider: ${config.provider.fallbackProvider || '--'}`} />
            <HealthBadge healthy={(activeProviderDefinition?.embedding.dimension ?? 0) === 1024} label={`active embedding: ${activeProviderDefinition?.embedding.dimension ?? '--'} dim`} />
            <HealthBadge healthy={providerEntries.length > 0} label={`providers: ${providerEntries.length}`} />
          </div>
        </div>

        <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
          <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/55 p-5 dark:border-white/10 dark:bg-white/[0.03]">
            <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Recommended Flow</div>
            <div className="mt-4 grid gap-3 md:grid-cols-4 text-sm">
              {['1. 进入编辑', '2. 校验配置', '3. 保存并生效', '4. 健康检查 / Outbox / Reindex 验证'].map((item) => (
                <div
                  key={item}
                  className="rounded-2xl border border-slate-200/70 bg-white/70 px-4 py-4 text-slate-600 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/55"
                >
                  {item}
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-[1.8rem] border border-slate-200/70 bg-white/55 p-5 dark:border-white/10 dark:bg-white/[0.03]">
            <div className="flex items-center gap-2 text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">
              <Info size={14} />
              当前标签说明
            </div>
            <div className="mt-3 text-sm leading-6 text-slate-600 dark:text-white/55">{activeTabDescription}</div>
          </div>
        </div>

        {view.notices.length > 0 && (
          <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-600 dark:text-amber-400">
            {view.notices.map((notice) => (
              <div key={notice}>{notice}</div>
            ))}
          </div>
        )}

        {busyMessage && (
          <div className="rounded-[1.6rem] border border-sky-500/20 bg-sky-500/5 p-4 text-sm text-sky-600 dark:text-sky-300">
            {busyMessage}
          </div>
        )}

        {feedback && (
          <div
            className={`rounded-[1.6rem] p-4 text-sm border flex items-start gap-3 ${
              feedback.tone === 'success'
                ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
                : 'border-rose-500/20 bg-rose-500/5 text-rose-500'
            }`}
          >
            {feedback.tone === 'success' ? <CheckCircle2 size={18} className="shrink-0 mt-0.5" /> : <AlertTriangle size={18} className="shrink-0 mt-0.5" />}
            <span>{feedback.message}</span>
          </div>
        )}

        {!!currentIssues.length && (
          <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500 space-y-2">
            {currentIssues.map((issue) => (
              <div key={`${issue.field}-${issue.message}`}>
                <span className="font-bold">{humanizeFieldName(issue.field)}</span>
                <span className="mx-2">·</span>
                <span>{translateConfigMessage(issue.message)}</span>
              </div>
            ))}
          </div>
        )}

        {!!validationNotices.length && (
          <div className="rounded-[1.6rem] border border-sky-500/20 bg-sky-500/5 p-4 text-sm text-sky-700 dark:text-sky-300 space-y-2">
            {validationNotices.map((notice) => (
              <div key={notice}>{translateConfigMessage(notice)}</div>
            ))}
          </div>
        )}

        <div className="flex flex-wrap gap-3">
          {tabs.map((tab) => (
            <TabButton key={tab.key} active={activeTab === tab.key} label={tab.label} onClick={() => setActiveTab(tab.key)} />
          ))}
        </div>
      </section>

      {activeTab === 'provider' && (
        <SectionCard title="模型接入配置" description="active / fallback provider 现在都是运行时真实生效的配置。每个 provider 拥有独立的 chat、embedding、rerank 参数和密钥。">
          <FieldGrid>
            <FieldCard label="当前 Provider" hint="请求会先打到这个 provider。发生可重试故障、熔断打开等情况时，才会尝试 fallback。">
              <TextInput
                value={config.provider.activeProvider}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, activeProvider: value } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="备用 Provider" hint="仅在 active provider 遇到 retryable 失败、429/5xx 或 circuit open 时尝试一次切换。">
              <TextInput
                value={config.provider.fallbackProvider}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, fallbackProvider: value } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>

          {providerEntries.length === 0 && (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500">
              当前配置没有任何 provider 定义，保存前至少需要补齐一套 provider.providers 配置。
            </div>
          )}

          {providerEntries.map(([providerName, definition]) => {
            const providerSecrets = secrets.providers[providerName];
            const providerTone = providerName === config.provider.activeProvider
              ? 'border-primary/20 bg-primary/5 text-primary'
              : providerName === config.provider.fallbackProvider
                ? 'border-amber-500/20 bg-amber-500/5 text-amber-600 dark:text-amber-400'
                : 'border-slate-200/70 bg-white/70 text-slate-500 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/45';

            return (
              <div key={providerName} className="rounded-[1.9rem] border border-slate-200/70 bg-white/55 p-5 dark:border-white/10 dark:bg-white/[0.03] space-y-5">
                <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                  <div>
                    <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Provider Definition</div>
                    <div className="mt-2 text-lg font-black text-slate-900 dark:text-white">{providerName}</div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <div className={`rounded-2xl border px-3 py-2 text-xs font-bold ${providerTone}`}>{providerName}</div>
                    {providerName === config.provider.activeProvider && (
                      <div className="rounded-2xl border border-primary/20 bg-primary/5 px-3 py-2 text-xs font-bold text-primary">ACTIVE</div>
                    )}
                    {providerName === config.provider.fallbackProvider && (
                      <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 px-3 py-2 text-xs font-bold text-amber-600 dark:text-amber-400">FALLBACK</div>
                    )}
                  </div>
                </div>

                <FieldGrid>
                  <FieldCard label="Chat 接口地址" hint="用于文本生成。应填写模型服务的根地址或兼容 OpenAI 的 base URL。">
                    <TextInput
                      value={definition.chat.baseUrl}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, baseUrl: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label={providerSecretMeta.chatApiKey.label} hint={providerSecretMeta.chatApiKey.hint}>
                    <div className="space-y-3">
                      <div className="text-xs text-slate-500 dark:text-white/35">
                        当前状态: {getProviderSecretField(view, providerName, 'chatApiKey')?.configured ? getProviderSecretField(view, providerName, 'chatApiKey')?.maskedValue : '未配置'}
                      </div>
                      <div className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={providerSecrets?.chatApiKey.retainExisting ?? false}
                          disabled={!editing}
                          onChange={(event) => updateProviderSecret(providerName, 'chatApiKey', {
                            retainExisting: event.target.checked,
                            value: event.target.checked ? '' : providerSecrets?.chatApiKey.value || '',
                          })}
                        />
                        <span>保留原值</span>
                      </div>
                      <TextInput
                        type="password"
                        value={providerSecrets?.chatApiKey.value || ''}
                        onChange={(value) => updateProviderSecret(providerName, 'chatApiKey', { value, retainExisting: false })}
                        disabled={!editing || Boolean(providerSecrets?.chatApiKey.retainExisting)}
                        placeholder="仅在需要覆盖时填写新值"
                      />
                    </div>
                  </FieldCard>
                  <FieldCard label="Chat 模型名" hint="例如通义千问的具体模型标识。模型名错误会直接导致运行时调用失败。">
                    <TextInput
                      value={definition.chat.model}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, model: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label="Chat 超时" hint="使用 Duration 格式，例如 30s。过短会造成高峰期误判超时。">
                    <TextInput
                      value={definition.chat.timeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, timeout: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label="生成温度" hint="值越高越发散。对诊断/教学类输出通常建议保持低温。">
                    <TextInput
                      type="number"
                      step="0.1"
                      value={definition.chat.temperature}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, temperature: Number(value) } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label="最大输出 Tokens" hint="限制单次回答长度。值过低会造成回答截断，值过高会增加耗时和成本。">
                    <TextInput
                      type="number"
                      value={definition.chat.maxTokens}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, maxTokens: Number(value) } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                </FieldGrid>

                <FieldGrid>
                  <FieldCard label="Embedding 接口地址" hint="用于向量化。这里变更后会影响 RAG 导入和检索的一致性。">
                    <TextInput
                      value={definition.embedding.baseUrl}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, embedding: { ...current.embedding, baseUrl: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label={providerSecretMeta.embeddingApiKey.label} hint={providerSecretMeta.embeddingApiKey.hint}>
                    <div className="space-y-3">
                      <div className="text-xs text-slate-500 dark:text-white/35">
                        当前状态: {getProviderSecretField(view, providerName, 'embeddingApiKey')?.configured ? getProviderSecretField(view, providerName, 'embeddingApiKey')?.maskedValue : '未配置'}
                      </div>
                      <div className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={providerSecrets?.embeddingApiKey.retainExisting ?? false}
                          disabled={!editing}
                          onChange={(event) => updateProviderSecret(providerName, 'embeddingApiKey', {
                            retainExisting: event.target.checked,
                            value: event.target.checked ? '' : providerSecrets?.embeddingApiKey.value || '',
                          })}
                        />
                        <span>保留原值</span>
                      </div>
                      <TextInput
                        type="password"
                        value={providerSecrets?.embeddingApiKey.value || ''}
                        onChange={(value) => updateProviderSecret(providerName, 'embeddingApiKey', { value, retainExisting: false })}
                        disabled={!editing || Boolean(providerSecrets?.embeddingApiKey.retainExisting)}
                        placeholder="仅在需要覆盖时填写新值"
                      />
                    </div>
                  </FieldCard>
                  <FieldCard label="Embedding 模型名" hint="必须和当前向量维度配置匹配，否则向量入库会失败。">
                    <TextInput
                      value={definition.embedding.model}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, embedding: { ...current.embedding, model: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label="Embedding 超时" hint="使用 Duration 格式，例如 30s。批量导入时建议适度放宽。">
                    <TextInput
                      value={definition.embedding.timeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, embedding: { ...current.embedding, timeout: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label="向量维度" hint="当前版本数据库 schema 固定为 1024。修改成其他值会被校验拒绝。">
                    <TextInput
                      type="number"
                      value={definition.embedding.dimension}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, embedding: { ...current.embedding, dimension: Number(value) } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                </FieldGrid>

                <FieldGrid>
                  <FieldCard label="Rerank 接口地址" hint="用于召回后的重排序。若关闭或异常，会明显影响最终检索质量。">
                    <TextInput
                      value={definition.rerank.baseUrl}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, rerank: { ...current.rerank, baseUrl: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label={providerSecretMeta.rerankApiKey.label} hint={providerSecretMeta.rerankApiKey.hint}>
                    <div className="space-y-3">
                      <div className="text-xs text-slate-500 dark:text-white/35">
                        当前状态: {getProviderSecretField(view, providerName, 'rerankApiKey')?.configured ? getProviderSecretField(view, providerName, 'rerankApiKey')?.maskedValue : '未配置'}
                      </div>
                      <div className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={providerSecrets?.rerankApiKey.retainExisting ?? false}
                          disabled={!editing}
                          onChange={(event) => updateProviderSecret(providerName, 'rerankApiKey', {
                            retainExisting: event.target.checked,
                            value: event.target.checked ? '' : providerSecrets?.rerankApiKey.value || '',
                          })}
                        />
                        <span>保留原值</span>
                      </div>
                      <TextInput
                        type="password"
                        value={providerSecrets?.rerankApiKey.value || ''}
                        onChange={(value) => updateProviderSecret(providerName, 'rerankApiKey', { value, retainExisting: false })}
                        disabled={!editing || Boolean(providerSecrets?.rerankApiKey.retainExisting)}
                        placeholder="仅在需要覆盖时填写新值"
                      />
                    </div>
                  </FieldCard>
                  <FieldCard label="Rerank 模型名" hint="建议与当前服务可用模型保持一致，否则健康检查会提示降级。">
                    <TextInput
                      value={definition.rerank.model}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, rerank: { ...current.rerank, model: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label="Rerank 超时" hint="使用 Duration 格式，例如 15s。阈值过低会影响召回后精排稳定性。">
                    <TextInput
                      value={definition.rerank.timeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, rerank: { ...current.rerank, timeout: value } }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                </FieldGrid>
              </div>
            );
          })}
        </SectionCard>
      )}

      {activeTab === 'resilience' && (
        <SectionCard title="稳定性配置" description="这些参数会在保存后直接刷新 ai-gateway 内部 retry / circuit breaker 注册表，并参与 active 到 fallback 的自动切换判断。">
          <div className="grid gap-3 md:grid-cols-2">
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
              <div className="font-bold text-slate-900 dark:text-white">Circuit Breaker 是什么</div>
              <div className="mt-2">当某个模型服务持续失败时，熔断器会暂时停止继续打流量，并把请求切到 fallback provider。</div>
            </div>
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
              <div className="font-bold text-slate-900 dark:text-white">Failure Rate Threshold 怎么看</div>
              <div className="mt-2">可以理解成“最近一段请求中，失败比例达到多少就触发熔断”。值越低越敏感。</div>
            </div>
          </div>
          <FieldGrid>
            <FieldCard
              label="最大重试次数"
              hint="单次请求允许的总尝试次数，包含首次请求。过大可能放大雪崩。"
              detail="经验上 2 到 3 次更常见。若上游只是偶发抖动，可以适度调高；若上游持续失败，调高只会让请求更慢。"
            >
              <TextInput
                type="number"
                value={config.resilience.maxAttempts}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, maxAttempts: Number(value) } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="重试等待时长" hint="两次重试之间的等待时间，使用 Duration 格式，例如 2s。">
              <TextInput
                value={config.resilience.waitDuration}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, waitDuration: value } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard
              label="熔断失败率阈值"
              hint="达到该失败率后会打开熔断器，通常按百分比数值理解。"
              detail="可以理解成“最近一批请求里，坏请求占多少就先暂停”。值越低越保守，服务稍有波动就会熔断；值越高越激进，更可能把故障流量继续打给上游。"
            >
              <TextInput
                type="number"
                step="0.1"
                value={config.resilience.failureRateThreshold}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, failureRateThreshold: Number(value) } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard
              label="滑动窗口大小"
              hint="统计失败率时观察的请求数量。窗口越小，熔断器越敏感。"
              detail="窗口就是“拿多少次最近请求来做判断”。窗口过小容易误判，窗口过大又会让熔断反应变慢。"
            >
              <TextInput
                type="number"
                value={config.resilience.slidingWindowSize}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, slidingWindowSize: Number(value) } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="熔断打开时长" hint="熔断后保持 OPEN 状态的时间，使用 Duration 格式，例如 30s。">
              <TextInput
                value={config.resilience.openStateDuration}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, openStateDuration: value } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>
        </SectionCard>
      )}

      {activeTab === 'rag' && (
        <SectionCard title="RAG 运行参数" description="词条变更会走知识同步链路；这里仍保留 ai-gateway 回源、检索参数和手动 reindex 配置，作为本地联调或同步异常时的兜底。">
          <div className="grid gap-3 md:grid-cols-2">
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
              <div className="font-bold text-slate-900 dark:text-white">Top K 是什么</div>
              <div className="mt-2">可以理解成“先保留前 K 个候选”。K 越大，召回越全，但后续成本也越高。</div>
            </div>
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
              <div className="font-bold text-slate-900 dark:text-white">Threshold 是什么</div>
              <div className="mt-2">阈值可以理解成“分数低于这条线就不要”。值越高，返回结果越少但通常更保守。</div>
            </div>
          </div>
          <FieldGrid>
            <FieldCard label="App Server 地址" hint="ai-gateway 回源读取词条和配置时访问的 app-server 地址。">
              <TextInput
                value={config.rag.appServer.baseUrl}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, appServer: { ...current.rag.appServer, baseUrl: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label={appServerSecretMeta.label} hint={appServerSecretMeta.hint}>
              <div className="space-y-3">
                <div className="text-xs text-slate-500 dark:text-white/35">
                  当前状态: {view.secrets.appServerInternalToken.configured ? view.secrets.appServerInternalToken.maskedValue : '未配置'}
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={secrets.appServerInternalToken.retainExisting}
                    disabled={!editing}
                    onChange={(event) => updateAppServerSecret({ retainExisting: event.target.checked, value: event.target.checked ? '' : secrets.appServerInternalToken.value })}
                  />
                  <span>保留原值</span>
                </div>
                <TextInput
                  type="password"
                  value={secrets.appServerInternalToken.value}
                  onChange={(value) => updateAppServerSecret({ value, retainExisting: false })}
                  disabled={!editing || secrets.appServerInternalToken.retainExisting}
                  placeholder="仅在需要覆盖时填写新值"
                />
              </div>
            </FieldCard>
            <FieldCard label="连接超时" hint="ai-gateway 连接 app-server 的超时时间，使用 Duration 格式，例如 3s。">
              <TextInput
                value={config.rag.appServer.connectTimeout}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, appServer: { ...current.rag.appServer, connectTimeout: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="读取超时" hint="等待 app-server 返回分页数据的超时时间，导出大批量语料时会影响成败。">
              <TextInput
                value={config.rag.appServer.readTimeout}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, appServer: { ...current.rag.appServer, readTimeout: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>

          <FieldGrid>
            <FieldCard label="导出分页大小" hint="单次从 app-server 拉取多少条词条。值越大吞吐越高，但单次失败成本也越高。">
              <TextInput
                type="number"
                value={config.rag.ingestion.exportPageSize}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, ingestion: { ...current.rag.ingestion, exportPageSize: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="向量批大小" hint="一次提交给 embedding 服务的批量大小。过大容易超时，过小会拖慢重建。">
              <TextInput
                type="number"
                value={config.rag.ingestion.embeddingBatchSize}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, ingestion: { ...current.rag.ingestion, embeddingBatchSize: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard
              label="初筛 Top K"
              hint="向量召回阶段保留的候选条数。越大越全，但后续开销越高。"
              detail="可以理解成“先捞多少条可能相关的候选”。捞得太少可能漏掉正确答案，捞得太多会拖慢 rerank 并引入更多噪音。"
            >
              <TextInput
                type="number"
                value={config.rag.retrieval.recallTopK}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, recallTopK: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard
              label="初筛阈值"
              hint="向量相似度阈值。值越高越严格，召回结果越少。"
              detail="如果发现召回结果太杂，可以调高；如果发现明明有词条却经常召不回来，可以适度调低。它和 Top K 一起决定“先捞多少”和“捞得多宽”。"
            >
              <TextInput
                type="number"
                step="0.01"
                value={config.rag.retrieval.recallThreshold}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, recallThreshold: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard
              label="重排 Top N"
              hint="进入 rerank 的候选条数。通常不应大于 Recall Top K。"
              detail="这一步是把初筛结果交给更贵、更准的排序模型复核。N 太小会错过优质候选，N 太大则会增加延迟和成本。"
            >
              <TextInput
                type="number"
                value={config.rag.retrieval.rerankTopN}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, rerankTopN: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard
              label="重排阈值"
              hint="重排序得分阈值。值越高，最终保留结果越少。"
              detail="它决定 rerank 模型打低分的候选要不要直接丢掉。若线上常出现“回答里引了不相关词条”，可以优先调高这个值。"
            >
              <TextInput
                type="number"
                step="0.01"
                value={config.rag.retrieval.rerankThreshold}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, rerankThreshold: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard
              label="最终返回 Top K"
              hint="最终写入回答上下文的条数。过高会增加 token 压力。"
              detail="这是最终真正送进回答生成链路的知识片段数。一般不宜过大，否则提示词变长、成本更高，且模型更容易被噪音干扰。"
            >
              <TextInput
                type="number"
                value={config.rag.retrieval.finalTopK}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, finalTopK: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>
        </SectionCard>
      )}

      {activeTab === 'operations' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 xl:grid-cols-[1.1fr_0.9fr] gap-6">
            <SectionCard title="健康检查" description="按钮会读取 ai-gateway 当前运行态健康信息，不会触发计费型模型调用，适合保存后立即验证。">
              <div className="flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={() => healthMutation.mutate()}
                  disabled={healthMutation.isPending}
                  className="btn-liquid px-5 py-3 text-white inline-flex items-center gap-2"
                >
                  <ShieldCheck size={16} />
                  {healthMutation.isPending ? '检查中...' : '测试连接 / 健康检查'}
                </button>
                <button
                  type="button"
                  onClick={() => void queryClient.invalidateQueries({ queryKey: ['admin-ai-config'] })}
                  className="rounded-2xl border border-slate-200 dark:border-white/10 px-5 py-3 text-sm font-bold text-slate-600 dark:text-white/70 bg-white/70 dark:bg-white/[0.04] inline-flex items-center gap-2"
                >
                  <RefreshCw size={16} />
                  刷新配置
                </button>
              </div>

              {healthMutation.error && (
                <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500">
                  {healthMutation.error.message}
                </div>
              )}

              {healthState && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <HealthBadge healthy={healthState.status === 'UP'} label={`整体状态: ${healthState.status}`} />
                  <HealthBadge healthy={healthState.databaseReady} label={`Database: ${healthState.databaseReady ? 'READY' : 'DOWN'}`} />
                  <HealthBadge healthy={healthState.providerReady} label={`Provider: ${healthState.providerReady ? 'READY' : 'DEGRADED'}`} />
                  <HealthBadge healthy={healthState.rerankReady} label={`Rerank: ${healthState.rerankReady ? 'READY' : 'DEGRADED'}`} />
                  <div className="rounded-2xl border border-slate-200/70 dark:border-white/10 px-4 py-4 bg-white/55 dark:bg-white/[0.03] text-sm text-slate-600 dark:text-white/60">
                    <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30 mb-2">Runtime Models</div>
                    <div>Chat: {healthState.chatModel}</div>
                    <div>Embedding: {healthState.embeddingModel}</div>
                    <div>Rerank: {healthState.rerankModel}</div>
                  </div>
                  <div className="rounded-2xl border border-slate-200/70 dark:border-white/10 px-4 py-4 bg-white/55 dark:bg-white/[0.03] text-sm text-slate-600 dark:text-white/60">
                    <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30 mb-2">Environment</div>
                    <div>Provider: {healthState.provider}</div>
                    <div>Fallback: {healthState.fallbackProvider}</div>
                    <div>Profiles: {healthState.activeProfiles.join(', ') || '--'}</div>
                  </div>
                </div>
              )}
            </SectionCard>

            <SectionCard title="RAG Reindex" description="正常情况下词条变更会发布知识同步事件；如果本地联调、RabbitMQ 或回源链路异常导致新词条没有进入检索，可在这里手动 reindex。">
              <div className="grid gap-3 md:grid-cols-3">
                <button
                  type="button"
                  onClick={() =>
                    setReindexForm({
                      mode: 'INCREMENTAL',
                      sourceTypes: ['LEXICAL_PAIR'],
                      sourceIds: [],
                      forceReembed: false,
                    })
                  }
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 text-left text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55"
                >
                  <div className="font-bold text-slate-900 dark:text-white">推荐：增量同步</div>
                  <div className="mt-2">用于刚导入一批新语料后的常规更新。</div>
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setReindexForm({
                      mode: 'FULL',
                      sourceTypes: ['LEXICAL_PAIR'],
                      sourceIds: [],
                      forceReembed: false,
                    })
                  }
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 text-left text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55"
                >
                  <div className="font-bold text-slate-900 dark:text-white">全量重建</div>
                  <div className="mt-2">适合模型或阈值发生较大变化后的完整重建。</div>
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setReindexForm((current) => ({
                      ...current,
                      mode: current.mode || 'INCREMENTAL',
                      sourceTypes: current.sourceTypes?.length ? current.sourceTypes : ['LEXICAL_PAIR'],
                      forceReembed: true,
                    }))
                  }
                  className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 px-4 py-4 text-left text-sm text-slate-600 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55"
                >
                  <div className="font-bold text-slate-900 dark:text-white">强制重嵌入</div>
                  <div className="mt-2">忽略已有 hash，适合替换 embedding 模型后使用。</div>
                </button>
              </div>

              <FieldGrid>
                <FieldCard label="执行模式" hint="推荐优先使用 INCREMENTAL；只有在索引结构或模型明显变更时再做 FULL。">
                  <SelectInput
                    value={reindexForm.mode || 'INCREMENTAL'}
                    onChange={(value) => setReindexForm((current) => ({ ...current, mode: value }))}
                    options={[
                      { value: 'INCREMENTAL', label: 'INCREMENTAL 增量更新' },
                      { value: 'FULL', label: 'FULL 全量重建' },
                    ]}
                  />
                </FieldCard>
                <FieldCard label="数据源类型" hint="逗号分隔，例如 LEXICAL_PAIR,LEXICAL_SENSE。通常只填 LEXICAL_PAIR 即可。">
                  <TextInput
                    value={(reindexForm.sourceTypes || []).join(',')}
                    onChange={(value) =>
                      setReindexForm((current) => ({
                        ...current,
                        sourceTypes: value
                          .split(',')
                          .map((item) => item.trim())
                          .filter(Boolean),
                      }))
                    }
                  />
                </FieldCard>
                <FieldCard label="数据源 ID" hint="可选，逗号分隔。为空时按 source type 全量处理；适合只补建某几条词对。">
                  <TextInput
                    value={(reindexForm.sourceIds || []).join(',')}
                    onChange={(value) =>
                      setReindexForm((current) => ({
                        ...current,
                        sourceIds: value
                          .split(',')
                          .map((item) => item.trim())
                          .filter(Boolean),
                      }))
                    }
                  />
                </FieldCard>
                <FieldCard label="强制重嵌入" hint="勾选后会忽略 chunk hash，适合切换 embedding 模型后使用。">
                  <div className="flex items-center gap-2 text-sm text-slate-600 dark:text-white/60">
                    <input
                      type="checkbox"
                      checked={Boolean(reindexForm.forceReembed)}
                      onChange={(event) => setReindexForm((current) => ({ ...current, forceReembed: event.target.checked }))}
                    />
                    <span>忽略 chunk hash，强制重算 embedding</span>
                  </div>
                </FieldCard>
              </FieldGrid>

              <button
                type="button"
                onClick={() => reindexMutation.mutate(reindexForm)}
                disabled={reindexMutation.isPending}
                className="btn-liquid px-5 py-3 text-white inline-flex items-center gap-2"
              >
                <Play size={16} />
                {reindexMutation.isPending ? '提交中...' : '触发 RAG Reindex'}
              </button>

              {reindexJobQuery.data && (
                <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 bg-white/55 dark:bg-white/[0.03] p-5 space-y-5 text-sm text-slate-600 dark:text-white/60">
                  <div className="flex items-center justify-between gap-4">
                    <div className="font-black text-slate-900 dark:text-white">任务 #{reindexJobQuery.data.jobId}</div>
                    <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{reindexJobQuery.data.status}</div>
                  </div>

                  <div className="rounded-[1.4rem] border border-slate-200/70 dark:border-white/10 bg-white/75 dark:bg-slate-950/25 p-4">
                    <div className="flex items-center justify-between gap-3">
                      <div className="font-bold text-slate-900 dark:text-white">{reindexStatusMeta.label}</div>
                      <div className="text-xs text-slate-400 dark:text-white/30">{reindexStatusMeta.progress}%</div>
                    </div>
                    <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200/70 dark:bg-white/10">
                      <div
                        className={`h-full rounded-full transition-all ${
                          reindexStatusMeta.tone === 'success'
                            ? 'bg-emerald-500'
                            : reindexStatusMeta.tone === 'warning'
                              ? 'bg-rose-500'
                              : 'bg-sky-500'
                        }`}
                        style={{ width: `${reindexStatusMeta.progress}%` }}
                      />
                    </div>
                    <div className="mt-3 text-sm text-slate-500 dark:text-white/45">{reindexStatusMeta.description}</div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-3">
                    {['已提交', '执行中', reindexJobQuery.data.status === 'FAILED' ? '失败' : '完成'].map((step, index) => {
                      const stepDone =
                        index === 0
                          ? reindexStatusMeta.progress > 0
                          : index === 1
                            ? reindexStatusMeta.progress >= 68
                            : finalStatuses.has(reindexJobQuery.data.status);
                      return (
                        <div
                          key={step}
                          className={`rounded-2xl border px-4 py-3 ${
                            stepDone
                              ? 'border-primary/20 bg-primary/5 text-primary'
                              : 'border-slate-200/70 bg-white/70 text-slate-500 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/45'
                          }`}
                        >
                          {step}
                        </div>
                      );
                    })}
                  </div>

                  <div>Mode: {reindexJobQuery.data.mode}</div>
                  <div>Source Types: {(reindexJobQuery.data.sourceTypes || []).join(', ') || '--'}</div>
                  <div>Source IDs: {(reindexJobQuery.data.sourceIds || []).join(', ') || '--'}</div>
                  <div>Cursor: {reindexJobQuery.data.lastCursor || '--'}</div>
                  <div>Last Source Update: {formatDateTime(reindexJobQuery.data.lastSourceUpdatedAt)}</div>
                  <div>Finished At: {formatDateTime(reindexJobQuery.data.finishedAt)}</div>

                  {!!reindexStats.length && (
                    <div className="grid gap-3 md:grid-cols-2">
                      {reindexStats.map((item) => (
                        <div key={item.key} className="rounded-2xl border border-slate-200/70 dark:border-white/10 px-4 py-4 bg-white/75 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{item.label}</div>
                          <div className="mt-2 font-bold text-slate-900 dark:text-white break-all">{item.value}</div>
                        </div>
                      ))}
                    </div>
                  )}

                  {reindexJobQuery.data.errorMessage && (
                    <div className="text-rose-500">Error: {translateConfigMessage(reindexJobQuery.data.errorMessage)}</div>
                  )}
                </div>
              )}
            </SectionCard>
          </div>

          <SectionCard title="Producer Outbox" description="这里展示生产侧待发送 / 失败事件，并提供人工重放入口，避免词汇知识同步静默丢失。">
            <div className="flex flex-wrap gap-3">
              <div className="min-w-[220px]">
                <SelectInput
                  value={outboxStatus}
                  onChange={setOutboxStatus}
                  options={[
                    { value: '', label: '全部状态' },
                    { value: 'FAILED', label: 'FAILED' },
                    { value: 'PENDING', label: 'PENDING' },
                    { value: 'IN_PROGRESS', label: 'IN_PROGRESS' },
                    { value: 'PUBLISHED', label: 'PUBLISHED' },
                  ]}
                />
              </div>
              <div className="w-28">
                <TextInput value={outboxLimit} onChange={setOutboxLimit} type="number" />
              </div>
              <button
                type="button"
                onClick={() => void queryClient.invalidateQueries({ queryKey: ['admin-ai-outbox'] })}
                className="rounded-2xl border border-slate-200 dark:border-white/10 px-5 py-3 text-sm font-bold text-slate-600 dark:text-white/70 bg-white/70 dark:bg-white/[0.04] inline-flex items-center gap-2"
              >
                <RefreshCw size={16} />
                刷新 Outbox
              </button>
            </div>

            {outboxQuery.error && (
              <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500">
                {outboxQuery.error.message}
              </div>
            )}

            {!outboxQuery.error && outboxRecords.length === 0 && (
              <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 text-sm text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45">
                当前筛选条件下没有 outbox 记录。
              </div>
            )}

            {!!outboxRecords.length && (
              <div className="grid gap-4">
                {outboxRecords.map((record: AdminOutboxRecordVO) => {
                  const tone = statusTone(record.status);
                  return (
                    <div key={record.id} className="rounded-[1.8rem] border border-slate-200/70 bg-white/60 p-5 dark:border-white/10 dark:bg-white/[0.03] space-y-4">
                      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                        <div className="space-y-2">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{record.eventType}</div>
                          <div className="text-lg font-black text-slate-900 dark:text-white break-all">{record.eventId}</div>
                          <div className="text-sm text-slate-500 dark:text-white/45">routingKey: {record.routingKey}</div>
                        </div>
                        <div className="flex flex-wrap items-center gap-2">
                          <div className={`rounded-2xl border px-3 py-2 text-xs font-bold ${statusClasses(tone)}`}>{record.status}</div>
                          <div className="rounded-2xl border border-slate-200/70 bg-white/70 px-3 py-2 text-xs font-bold text-slate-500 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/45">
                            attempts: {record.attemptCount}
                          </div>
                          {(record.status === 'FAILED' || record.status === 'PENDING') && (
                            <button
                              type="button"
                              onClick={() => replayOutboxMutation.mutate(record.id)}
                              disabled={replayOutboxMutation.isPending && replayingOutboxId === record.id}
                              className="btn-liquid px-4 py-2 text-white text-sm"
                            >
                              {replayOutboxMutation.isPending && replayingOutboxId === record.id ? '重放中...' : '重放'}
                            </button>
                          )}
                        </div>
                      </div>

                      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4 text-sm">
                        <div className="rounded-2xl border border-slate-200/70 bg-white/75 px-4 py-3 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">Created</div>
                          <div className="mt-2 text-slate-700 dark:text-white/70">{formatDateTime(record.createdAt)}</div>
                        </div>
                        <div className="rounded-2xl border border-slate-200/70 bg-white/75 px-4 py-3 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">Next Attempt</div>
                          <div className="mt-2 text-slate-700 dark:text-white/70">{formatDateTime(record.nextAttemptAt)}</div>
                        </div>
                        <div className="rounded-2xl border border-slate-200/70 bg-white/75 px-4 py-3 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">Published</div>
                          <div className="mt-2 text-slate-700 dark:text-white/70">{formatDateTime(record.publishedAt)}</div>
                        </div>
                        <div className="rounded-2xl border border-slate-200/70 bg-white/75 px-4 py-3 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">Trace Id</div>
                          <div className="mt-2 text-slate-700 dark:text-white/70 break-all">{record.traceId || '--'}</div>
                        </div>
                      </div>

                      {record.lastError && (
                        <div className="rounded-[1.4rem] border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500 break-all">
                          {record.lastError}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </SectionCard>
        </div>
      )}
    </div>
  );
};

export default AdminConfigCenterPage;

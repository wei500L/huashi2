import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, CheckCircle2, LoaderCircle, Play, RefreshCw, Save, ShieldCheck } from 'lucide-react';
import { PageHeader } from '@/components/common';
import { formatDateTime } from '@/lib/format';
import { adminService } from '@/lib/services';
import type {
  AdminAiConfigSaveRequest,
  AdminAiConfigViewVO,
  AdminAiSecretFieldsVO,
  AiGatewayHealthResponse,
  AiOpsConfigPayload,
  AiOpsConfigValidationResponse,
  RagReindexJobResponse,
  RagReindexRequest,
} from '@/lib/contracts';

type ConfigTab = 'provider' | 'resilience' | 'rag' | 'operations';
type SecretKey = keyof AdminAiSecretFieldsVO;

type SecretEditorState = {
  retainExisting: boolean;
  value: string;
};

type SecretEditorMap = Record<SecretKey, SecretEditorState>;

const tabs: Array<{ key: ConfigTab; label: string }> = [
  { key: 'provider', label: 'Provider' },
  { key: 'resilience', label: 'Resilience' },
  { key: 'rag', label: 'RAG' },
  { key: 'operations', label: 'Operations' },
];

const secretMeta: Record<SecretKey, { label: string; hint: string }> = {
  chatApiKey: { label: 'Chat API Key', hint: '当前仅支持 qwen 作为 activeProvider。' },
  embeddingApiKey: { label: 'Embedding API Key', hint: '当前 pgvector schema 固定为 1024 维。' },
  rerankApiKey: { label: 'Rerank API Key', hint: '与 DashScope / 兼容接口保持一致。' },
  appServerInternalToken: { label: 'App Server Internal Token', hint: '用于 ai-gateway 拉取词条导出和配置同步。' },
};

const finalStatuses = new Set(['SUCCEEDED', 'FAILED']);

function cloneConfig<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function buildSecretEditors(view: AdminAiConfigViewVO): SecretEditorMap {
  return {
    chatApiKey: { retainExisting: view.secrets.chatApiKey.configured, value: '' },
    embeddingApiKey: { retainExisting: view.secrets.embeddingApiKey.configured, value: '' },
    rerankApiKey: { retainExisting: view.secrets.rerankApiKey.configured, value: '' },
    appServerInternalToken: { retainExisting: view.secrets.appServerInternalToken.configured, value: '' },
  };
}

function buildSavePayload(config: AiOpsConfigPayload, secrets: SecretEditorMap): AdminAiConfigSaveRequest {
  return {
    config,
    secrets: {
      chatApiKey: secrets.chatApiKey,
      embeddingApiKey: secrets.embeddingApiKey,
      rerankApiKey: secrets.rerankApiKey,
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

const FieldCard: React.FC<{ label: string; hint?: string; children: React.ReactNode }> = ({ label, hint, children }) => (
  <label className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 bg-white/55 dark:bg-white/[0.03] px-4 py-4 space-y-3 block">
    <div>
      <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{label}</div>
      {hint && <div className="text-xs text-slate-500 dark:text-white/35 mt-2">{hint}</div>}
    </div>
    {children}
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

const AdminConfigCenterPage: React.FC = () => {
  const queryClient = useQueryClient();
  const configQuery = useQuery({
    queryKey: ['admin-ai-config'],
    queryFn: () => adminService.getAiConfig(),
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
      setFeedback({ tone: 'error', message: error.message });
    },
  });

  const healthMutation = useMutation({
    mutationFn: () => adminService.getAiHealth(),
    onSuccess: (response) => setHealthState(response),
  });

  const reindexMutation = useMutation({
    mutationFn: (payload: RagReindexRequest) => adminService.triggerRagReindex(payload),
    onSuccess: (response) => {
      setJobId(response.jobId);
      setPollJob(true);
      setFeedback({ tone: 'success', message: `已提交 RAG reindex，任务 #${response.jobId}。` });
    },
    onError: (error: Error) => setFeedback({ tone: 'error', message: error.message }),
  });

  const reindexJobQuery = useQuery({
    queryKey: ['admin-ai-reindex-job', jobId],
    queryFn: () => adminService.getRagReindexJob(jobId as number),
    enabled: jobId !== null,
    refetchInterval: pollJob ? 2000 : false,
  });

  React.useEffect(() => {
    const data = reindexJobQuery.data;
    if (!data || !finalStatuses.has(data.status)) {
      return;
    }
    setPollJob(false);
  }, [reindexJobQuery.data]);

  const updateConfig = React.useCallback((updater: (current: AiOpsConfigPayload) => AiOpsConfigPayload) => {
    setConfig((current) => (current ? updater(current) : current));
  }, []);

  const updateSecret = React.useCallback((key: SecretKey, patch: Partial<SecretEditorState>) => {
    setSecrets((current) =>
      current
        ? {
            ...current,
            [key]: {
              ...current[key],
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

  if (configQuery.error) {
    return (
      <div className="space-y-8 pb-20">
        <PageHeader title="运维管理员配置中心" subtitle="加载失败时不隐藏原因，直接显示后端返回错误。" />
        <div className="rounded-[2.5rem] border border-rose-500/20 bg-rose-500/5 p-8 text-rose-500">{configQuery.error.message}</div>
      </div>
    );
  }

  const view = configQuery.data as AdminAiConfigViewVO;
  const sourceMeta = `来源 ${view.source} · 版本 ${view.version ?? '--'} · 更新时间 ${formatDateTime(view.updatedAt)}`;

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        title="运维管理员配置中心"
        subtitle="数据库配置覆盖默认 yml / env。保存时先校验并热应用到 ai-gateway，成功后再持久化。"
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
            <HealthBadge healthy={config.provider.activeProvider === 'qwen'} label={`activeProvider: ${config.provider.activeProvider}`} />
            <HealthBadge healthy={config.provider.embedding.dimension === 1024} label={`embedding: ${config.provider.embedding.dimension} dim`} />
          </div>
        </div>

        {view.notices.length > 0 && (
          <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-600 dark:text-amber-400">
            {view.notices.map((notice) => (
              <div key={notice}>{notice}</div>
            ))}
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
                <span className="font-bold">{issue.field}</span>
                <span className="mx-2">·</span>
                <span>{issue.message}</span>
              </div>
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
        <SectionCard title="AI Provider" description="activeProvider 当前仅支持 qwen。fallbackProvider 会被保存和展示，但不会触发自动回切。">
          <FieldGrid>
            <FieldCard label="Active Provider" hint="当前仓库真实实现只有 qwen。">
              <TextInput
                value={config.provider.activeProvider}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, activeProvider: value } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Fallback Provider" hint="当前仅作配置保留，不代表 deepseek 已接入自动 failover。">
              <TextInput
                value={config.provider.fallbackProvider}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, fallbackProvider: value } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>

          <FieldGrid>
            <FieldCard label="Chat Base URL">
              <TextInput
                value={config.provider.chat.baseUrl}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, chat: { ...current.provider.chat, baseUrl: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label={secretMeta.chatApiKey.label} hint={secretMeta.chatApiKey.hint}>
              <div className="space-y-3">
                <div className="text-xs text-slate-500 dark:text-white/35">
                  当前状态: {view.secrets.chatApiKey.configured ? view.secrets.chatApiKey.maskedValue : '未配置'}
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={secrets.chatApiKey.retainExisting}
                    disabled={!editing}
                    onChange={(event) => updateSecret('chatApiKey', { retainExisting: event.target.checked, value: event.target.checked ? '' : secrets.chatApiKey.value })}
                  />
                  <span>保留原值</span>
                </div>
                <TextInput
                  type="password"
                  value={secrets.chatApiKey.value}
                  onChange={(value) => updateSecret('chatApiKey', { value, retainExisting: false })}
                  disabled={!editing || secrets.chatApiKey.retainExisting}
                  placeholder="仅在需要覆盖时填写新值"
                />
              </div>
            </FieldCard>
            <FieldCard label="Chat Model">
              <TextInput
                value={config.provider.chat.model}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, chat: { ...current.provider.chat, model: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Chat Timeout">
              <TextInput
                value={config.provider.chat.timeout}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, chat: { ...current.provider.chat, timeout: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Chat Temperature">
              <TextInput
                type="number"
                step="0.1"
                value={config.provider.chat.temperature}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, chat: { ...current.provider.chat, temperature: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Chat Max Tokens">
              <TextInput
                type="number"
                value={config.provider.chat.maxTokens}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, chat: { ...current.provider.chat, maxTokens: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>

          <FieldGrid>
            <FieldCard label="Embedding Base URL">
              <TextInput
                value={config.provider.embedding.baseUrl}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, embedding: { ...current.provider.embedding, baseUrl: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label={secretMeta.embeddingApiKey.label} hint={secretMeta.embeddingApiKey.hint}>
              <div className="space-y-3">
                <div className="text-xs text-slate-500 dark:text-white/35">
                  当前状态: {view.secrets.embeddingApiKey.configured ? view.secrets.embeddingApiKey.maskedValue : '未配置'}
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={secrets.embeddingApiKey.retainExisting}
                    disabled={!editing}
                    onChange={(event) => updateSecret('embeddingApiKey', { retainExisting: event.target.checked, value: event.target.checked ? '' : secrets.embeddingApiKey.value })}
                  />
                  <span>保留原值</span>
                </div>
                <TextInput
                  type="password"
                  value={secrets.embeddingApiKey.value}
                  onChange={(value) => updateSecret('embeddingApiKey', { value, retainExisting: false })}
                  disabled={!editing || secrets.embeddingApiKey.retainExisting}
                  placeholder="仅在需要覆盖时填写新值"
                />
              </div>
            </FieldCard>
            <FieldCard label="Embedding Model">
              <TextInput
                value={config.provider.embedding.model}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, embedding: { ...current.provider.embedding, model: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Embedding Timeout">
              <TextInput
                value={config.provider.embedding.timeout}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, embedding: { ...current.provider.embedding, timeout: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Embedding Dimension" hint="当前版本数据库 schema 固定为 1024，修改其他值会被校验拒绝。">
              <TextInput
                type="number"
                value={config.provider.embedding.dimension}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, embedding: { ...current.provider.embedding, dimension: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>

          <FieldGrid>
            <FieldCard label="Rerank Base URL">
              <TextInput
                value={config.provider.rerank.baseUrl}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, rerank: { ...current.provider.rerank, baseUrl: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label={secretMeta.rerankApiKey.label} hint={secretMeta.rerankApiKey.hint}>
              <div className="space-y-3">
                <div className="text-xs text-slate-500 dark:text-white/35">
                  当前状态: {view.secrets.rerankApiKey.configured ? view.secrets.rerankApiKey.maskedValue : '未配置'}
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={secrets.rerankApiKey.retainExisting}
                    disabled={!editing}
                    onChange={(event) => updateSecret('rerankApiKey', { retainExisting: event.target.checked, value: event.target.checked ? '' : secrets.rerankApiKey.value })}
                  />
                  <span>保留原值</span>
                </div>
                <TextInput
                  type="password"
                  value={secrets.rerankApiKey.value}
                  onChange={(value) => updateSecret('rerankApiKey', { value, retainExisting: false })}
                  disabled={!editing || secrets.rerankApiKey.retainExisting}
                  placeholder="仅在需要覆盖时填写新值"
                />
              </div>
            </FieldCard>
            <FieldCard label="Rerank Model">
              <TextInput
                value={config.provider.rerank.model}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, rerank: { ...current.provider.rerank, model: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Rerank Timeout">
              <TextInput
                value={config.provider.rerank.timeout}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, rerank: { ...current.provider.rerank, timeout: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>
        </SectionCard>
      )}

      {activeTab === 'resilience' && (
        <SectionCard title="AI Resilience" description="这些参数会在保存后直接刷新 ai-gateway 内部 retry / circuit breaker 注册表。">
          <FieldGrid>
            <FieldCard label="Max Attempts">
              <TextInput
                type="number"
                value={config.resilience.maxAttempts}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, maxAttempts: Number(value) } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Wait Duration">
              <TextInput
                value={config.resilience.waitDuration}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, waitDuration: value } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Failure Rate Threshold">
              <TextInput
                type="number"
                step="0.1"
                value={config.resilience.failureRateThreshold}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, failureRateThreshold: Number(value) } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Sliding Window Size">
              <TextInput
                type="number"
                value={config.resilience.slidingWindowSize}
                onChange={(value) => updateConfig((current) => ({ ...current, resilience: { ...current.resilience, slidingWindowSize: Number(value) } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Open State Duration">
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
        <SectionCard title="RAG Runtime" description="词条 CSV 导入和向量 reindex 仍然是两段链路。这里配置的是 ai-gateway 拉取 app-server 以及检索 / 入库参数。">
          <FieldGrid>
            <FieldCard label="App Server Base URL">
              <TextInput
                value={config.rag.appServer.baseUrl}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, appServer: { ...current.rag.appServer, baseUrl: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label={secretMeta.appServerInternalToken.label} hint={secretMeta.appServerInternalToken.hint}>
              <div className="space-y-3">
                <div className="text-xs text-slate-500 dark:text-white/35">
                  当前状态: {view.secrets.appServerInternalToken.configured ? view.secrets.appServerInternalToken.maskedValue : '未配置'}
                </div>
                <div className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={secrets.appServerInternalToken.retainExisting}
                    disabled={!editing}
                    onChange={(event) => updateSecret('appServerInternalToken', { retainExisting: event.target.checked, value: event.target.checked ? '' : secrets.appServerInternalToken.value })}
                  />
                  <span>保留原值</span>
                </div>
                <TextInput
                  type="password"
                  value={secrets.appServerInternalToken.value}
                  onChange={(value) => updateSecret('appServerInternalToken', { value, retainExisting: false })}
                  disabled={!editing || secrets.appServerInternalToken.retainExisting}
                  placeholder="仅在需要覆盖时填写新值"
                />
              </div>
            </FieldCard>
            <FieldCard label="App Server Connect Timeout">
              <TextInput
                value={config.rag.appServer.connectTimeout}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, appServer: { ...current.rag.appServer, connectTimeout: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="App Server Read Timeout">
              <TextInput
                value={config.rag.appServer.readTimeout}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, appServer: { ...current.rag.appServer, readTimeout: value } } }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>

          <FieldGrid>
            <FieldCard label="Export Page Size">
              <TextInput
                type="number"
                value={config.rag.ingestion.exportPageSize}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, ingestion: { ...current.rag.ingestion, exportPageSize: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Embedding Batch Size">
              <TextInput
                type="number"
                value={config.rag.ingestion.embeddingBatchSize}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, ingestion: { ...current.rag.ingestion, embeddingBatchSize: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Recall Top K">
              <TextInput
                type="number"
                value={config.rag.retrieval.recallTopK}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, recallTopK: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Recall Threshold">
              <TextInput
                type="number"
                step="0.01"
                value={config.rag.retrieval.recallThreshold}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, recallThreshold: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Rerank Top N">
              <TextInput
                type="number"
                value={config.rag.retrieval.rerankTopN}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, rerankTopN: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Rerank Threshold">
              <TextInput
                type="number"
                step="0.01"
                value={config.rag.retrieval.rerankThreshold}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, retrieval: { ...current.rag.retrieval, rerankThreshold: Number(value) } } }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="Final Top K">
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
        <div className="grid grid-cols-1 xl:grid-cols-[1.1fr_0.9fr] gap-6">
          <SectionCard title="Health Check" description="按钮会读取 ai-gateway 当前运行态健康信息，不会触发计费型模型调用。">
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

          <SectionCard title="RAG Reindex" description="CSV 导入并不会自动触发向量重建。这里提供手动 reindex 入口，用于词条导入后的运维操作。">
            <FieldGrid>
              <FieldCard label="Mode">
                <TextInput
                  value={reindexForm.mode || 'INCREMENTAL'}
                  onChange={(value) => setReindexForm((current) => ({ ...current, mode: value }))}
                />
              </FieldCard>
              <FieldCard label="Source Types" hint="逗号分隔，例如 LEXICAL_PAIR,LEXICAL_SENSE。">
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
              <FieldCard label="Source IDs" hint="可选，逗号分隔。为空时按 source type 全量处理。">
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
              <FieldCard label="Force Reembed">
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
              <div className="rounded-[1.6rem] border border-slate-200/70 dark:border-white/10 bg-white/55 dark:bg-white/[0.03] p-5 space-y-3 text-sm text-slate-600 dark:text-white/60">
                <div className="flex items-center justify-between gap-4">
                  <div className="font-black text-slate-900 dark:text-white">任务 #{reindexJobQuery.data.jobId}</div>
                  <div className="text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{reindexJobQuery.data.status}</div>
                </div>
                <div>Mode: {reindexJobQuery.data.mode}</div>
                <div>Source Types: {(reindexJobQuery.data.sourceTypes || []).join(', ') || '--'}</div>
                <div>Cursor: {reindexJobQuery.data.lastCursor || '--'}</div>
                <div>Finished At: {formatDateTime(reindexJobQuery.data.finishedAt)}</div>
                <div>Stats: {JSON.stringify(reindexJobQuery.data.stats || {})}</div>
                {reindexJobQuery.data.errorMessage && (
                  <div className="text-rose-500">Error: {reindexJobQuery.data.errorMessage}</div>
                )}
              </div>
            )}
          </SectionCard>
        </div>
      )}
    </div>
  );
};

export default AdminConfigCenterPage;

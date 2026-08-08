/* eslint-disable react-refresh/only-export-components */
import React from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, CheckCircle2, ChevronDown, Info, Pencil, Play, Plus, RefreshCw, Save, ShieldCheck, Trash2, X } from 'lucide-react';
import { z } from 'zod';
import { PageHeader, WorkflowStepper } from '@/components/common';
import type { WorkflowStage } from '@/components/common';
import { FeedbackState } from '@/components/common/FeedbackState';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { ApiError } from '@/lib/api';
import { getProductizedErrorState } from '@/lib/async-state';
import { formatDateTime } from '@/lib/format';
import { adminService } from '@/lib/services';
import { AiOpsProtocolValues } from '@/lib/contracts/generated/session-domain';
import type {
  AdminAiConfigDriftVO,
  AdminAiConfigSaveRequest,
  AdminAiRuntimeSyncRequest,
  AdminAiConfigViewVO,
  AdminAiEmbeddingProbeVO,
  AdminAiRerankProbeVO,
  AdminAiSecretFieldVO,
  AdminOutboxRecordVO,
  AiGatewayHealthResponse,
  AiOpsConfigIssue,
  AiOpsConfigNotice,
  AiOpsDraftConfigPayload,
  AiOpsDraftProviderDefinition,
  AiOpsConfigPayload,
  AiOpsProtocol,
  AiOpsConfigValidationResponse,
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
type ProviderOriginMap = Record<string, string>;

type SecretEditorMap = {
  providers: Record<string, ProviderSecretEditorMap>;
  appServerInternalToken: SecretEditorState;
};

const providerKeyPattern = /^[a-z0-9_-]+$/;
const protocolSchema = z.enum(AiOpsProtocolValues);
const chatProtocolValues = ['openai-compat', 'openai-responses'] as const satisfies readonly AiOpsProtocol[];
const rerankProtocolValues = ['openai-rerank', 'openai-chat-rerank'] as const satisfies readonly AiOpsProtocol[];
const providerProtocolOptions = {
  chat: chatProtocolValues.map((value) => ({ value, label: value })),
  embedding: [{ value: 'openai-compat', label: 'openai-compat' }],
  rerank: rerankProtocolValues.map((value) => ({ value, label: value })),
} as const;

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
  hint: '仅用于 ai-gateway 调用 app-server 内部接口的客户端令牌，不影响管理员访问本页。',
};

const defaultLexicalSourceTypes = ['LEXICAL_PAIR', 'LEXICAL_SENSE', 'LEXICAL_EXAMPLE'];
const appServerReindexSourceTypes = ['LEXICAL_PAIR', 'LEXICAL_SENSE', 'LEXICAL_EXAMPLE'];
const seedReindexSourceTypes = ['ERROR_TYPE', 'INTERVENTION_TEMPLATE', 'TRAINING_GUIDE', 'COURSE_GUIDE'];
const allReindexSourceTypes = [...appServerReindexSourceTypes, ...seedReindexSourceTypes];

const reindexSourceTypeLabels: Record<string, string> = {
  LEXICAL_PAIR: 'LEXICAL_PAIR 词对主表',
  LEXICAL_SENSE: 'LEXICAL_SENSE 义项',
  LEXICAL_EXAMPLE: 'LEXICAL_EXAMPLE 例句',
  ERROR_TYPE: 'ERROR_TYPE 错误类型 Seed',
  INTERVENTION_TEMPLATE: 'INTERVENTION_TEMPLATE 干预模板 Seed',
  TRAINING_GUIDE: 'TRAINING_GUIDE 训练指南 Seed',
  COURSE_GUIDE: 'COURSE_GUIDE 课程指南 Seed',
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
  protocol: '协议',
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
  hnswEfSearch: 'HNSW ef_search',
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

type UnknownRecord = Record<string, unknown>;

export class AdminAiConfigContractError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'AdminAiConfigContractError';
  }
}

function cloneConfig<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function isRecord(value: unknown): value is UnknownRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function requireRecord(value: unknown, path: string): UnknownRecord {
  if (!isRecord(value)) {
    throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：缺少 ${path}。`);
  }
  return value;
}

function requireArray(value: unknown, path: string): unknown[] {
  if (!Array.isArray(value)) {
    throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：缺少 ${path}。`);
  }
  return value;
}

function requireBoolean(value: unknown, path: string): boolean {
  if (typeof value !== 'boolean') {
    throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：缺少 ${path}。`);
  }
  return value;
}

function requireString(value: unknown, path: string): string {
  if (typeof value !== 'string') {
    throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：缺少 ${path}。`);
  }
  return value;
}

function requireNullableString(value: unknown, path: string): string | null | undefined {
  if (value !== null && value !== undefined && typeof value !== 'string') {
    throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：缺少 ${path}。`);
  }
  return value as string | null | undefined;
}

function requireNullableNumber(value: unknown, path: string): number | null | undefined {
  if (value !== null && value !== undefined && typeof value !== 'number') {
    throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：缺少 ${path}。`);
  }
  return value as number | null | undefined;
}

function requireNullableBoolean(value: unknown, path: string): boolean | null | undefined {
  if (value !== null && value !== undefined && typeof value !== 'boolean') {
    throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：缺少 ${path}。`);
  }
  return value as boolean | null | undefined;
}

function assertSecretField(field: unknown, path: string): void {
  const secretField = requireRecord(field, path);
  requireBoolean(secretField.configured, `${path}.configured`);
  requireString(secretField.maskedValue, `${path}.maskedValue`);
  requireNullableNumber(secretField.valueLength, `${path}.valueLength`);
}

function assertConfigNotice(notice: unknown, path: string): void {
  const configNotice = requireRecord(notice, path);
  requireString(configNotice.code, `${path}.code`);
  requireString(configNotice.severity, `${path}.severity`);
  requireString(configNotice.defaultMessage, `${path}.defaultMessage`);
  if (configNotice.args !== undefined && configNotice.args !== null) {
    requireRecord(configNotice.args, `${path}.args`);
  }
}

function assertProviderSecretGroup(group: unknown, path: string): void {
  const secretGroup = requireRecord(group, path);
  assertSecretField(secretGroup.chatApiKey, `${path}.chatApiKey`);
  assertSecretField(secretGroup.embeddingApiKey, `${path}.embeddingApiKey`);
  assertSecretField(secretGroup.rerankApiKey, `${path}.rerankApiKey`);
}

function assertProviderDefinition(definition: unknown, path: string): void {
  const providerDefinition = requireRecord(definition, path);
  const chat = requireRecord(providerDefinition.chat, `${path}.chat`);
  requireNullableString(chat.protocol, `${path}.chat.protocol`);
  requireNullableString(chat.baseUrl, `${path}.chat.baseUrl`);
  requireNullableString(chat.apiKey, `${path}.chat.apiKey`);
  requireNullableString(chat.model, `${path}.chat.model`);
  requireNullableString(chat.connectTimeout, `${path}.chat.connectTimeout`);
  requireNullableString(chat.readTimeout, `${path}.chat.readTimeout`);
  requireNullableNumber(chat.temperature, `${path}.chat.temperature`);
  requireNullableNumber(chat.maxTokens, `${path}.chat.maxTokens`);

  const embedding = requireRecord(providerDefinition.embedding, `${path}.embedding`);
  requireNullableString(embedding.protocol, `${path}.embedding.protocol`);
  requireNullableString(embedding.baseUrl, `${path}.embedding.baseUrl`);
  requireNullableString(embedding.apiKey, `${path}.embedding.apiKey`);
  requireNullableString(embedding.model, `${path}.embedding.model`);
  requireNullableString(embedding.multimodalModel, `${path}.embedding.multimodalModel`);
  requireNullableString(embedding.connectTimeout, `${path}.embedding.connectTimeout`);
  requireNullableString(embedding.readTimeout, `${path}.embedding.readTimeout`);
  requireNullableNumber(embedding.dimension, `${path}.embedding.dimension`);

  const rerank = requireRecord(providerDefinition.rerank, `${path}.rerank`);
  requireNullableString(rerank.protocol, `${path}.rerank.protocol`);
  requireNullableString(rerank.baseUrl, `${path}.rerank.baseUrl`);
  requireNullableString(rerank.apiKey, `${path}.rerank.apiKey`);
  requireNullableString(rerank.model, `${path}.rerank.model`);
  requireNullableString(rerank.multimodalModel, `${path}.rerank.multimodalModel`);
  requireNullableString(rerank.connectTimeout, `${path}.rerank.connectTimeout`);
  requireNullableString(rerank.readTimeout, `${path}.rerank.readTimeout`);
}

function assertProviderConfig(provider: unknown, path: string): string[] {
  const providerConfig = requireRecord(provider, path);
  requireNullableString(providerConfig.activeProvider, `${path}.activeProvider`);
  requireNullableString(providerConfig.fallbackProvider, `${path}.fallbackProvider`);
  const providers = requireRecord(providerConfig.providers, `${path}.providers`);
  const providerNames = Object.keys(providers);
  providerNames.forEach((providerName) =>
    assertProviderDefinition(providers[providerName], `${path}.providers.${providerName}`)
  );
  return providerNames;
}

function assertAiOpsConfigPayload(payload: unknown, path: string): string[] {
  const config = requireRecord(payload, path);
  const providerNames = assertProviderConfig(config.provider, `${path}.provider`);

  const resilience = requireRecord(config.resilience, `${path}.resilience`);
  requireNullableNumber(resilience.maxAttempts, `${path}.resilience.maxAttempts`);
  requireNullableString(resilience.waitDuration, `${path}.resilience.waitDuration`);
  requireNullableNumber(resilience.failureRateThreshold, `${path}.resilience.failureRateThreshold`);
  requireNullableNumber(resilience.slidingWindowSize, `${path}.resilience.slidingWindowSize`);
  requireNullableString(resilience.openStateDuration, `${path}.resilience.openStateDuration`);

  const rag = requireRecord(config.rag, `${path}.rag`);
  const appServer = requireRecord(rag.appServer, `${path}.rag.appServer`);
  requireNullableString(appServer.baseUrl, `${path}.rag.appServer.baseUrl`);
  requireNullableString(appServer.internalToken, `${path}.rag.appServer.internalToken`);
  requireNullableString(appServer.connectTimeout, `${path}.rag.appServer.connectTimeout`);
  requireNullableString(appServer.readTimeout, `${path}.rag.appServer.readTimeout`);

  const ingestion = requireRecord(rag.ingestion, `${path}.rag.ingestion`);
  requireNullableNumber(ingestion.exportPageSize, `${path}.rag.ingestion.exportPageSize`);
  requireNullableNumber(ingestion.embeddingBatchSize, `${path}.rag.ingestion.embeddingBatchSize`);
  requireNullableBoolean(ingestion.failedRetryEnabled, `${path}.rag.ingestion.failedRetryEnabled`);
  requireNullableNumber(ingestion.failedRetryLimit, `${path}.rag.ingestion.failedRetryLimit`);

  const retrieval = requireRecord(rag.retrieval, `${path}.rag.retrieval`);
  requireNullableNumber(retrieval.recallTopK, `${path}.rag.retrieval.recallTopK`);
  requireNullableNumber(retrieval.recallThreshold, `${path}.rag.retrieval.recallThreshold`);
  requireNullableNumber(retrieval.rerankTopN, `${path}.rag.retrieval.rerankTopN`);
  requireNullableNumber(retrieval.rerankThreshold, `${path}.rag.retrieval.rerankThreshold`);
  requireNullableNumber(retrieval.finalTopK, `${path}.rag.retrieval.finalTopK`);
  requireNullableNumber(retrieval.hnswEfSearch, `${path}.rag.retrieval.hnswEfSearch`);

  return providerNames;
}

function assertSecretFields(secrets: unknown, expectedProviderNames: string[], path: string): void {
  const secretFields = requireRecord(secrets, path);
  const providers = requireRecord(secretFields.providers, `${path}.providers`);
  const secretProviderNames = Object.keys(providers);
  expectedProviderNames.forEach((providerName) => {
    if (!(providerName in providers)) {
      throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：缺少 ${path}.providers.${providerName}。`);
    }
    assertProviderSecretGroup(providers[providerName], `${path}.providers.${providerName}`);
  });
  secretProviderNames.forEach((providerName) => {
    if (!expectedProviderNames.includes(providerName)) {
      throw new AdminAiConfigContractError(`AI 管理员配置响应契约异常：存在未定义的 ${path}.providers.${providerName}。`);
    }
  });
  assertSecretField(secretFields.appServerInternalToken, `${path}.appServerInternalToken`);
}

function assertRuntimeState(runtime: unknown, path: string): void {
  const runtimeState = requireRecord(runtime, path);
  requireBoolean(runtimeState.available, `${path}.available`);
  requireNullableString(runtimeState.source, `${path}.source`);
  requireNullableString(runtimeState.version, `${path}.version`);
  requireNullableString(runtimeState.appliedAt, `${path}.appliedAt`);
  requireBoolean(runtimeState.inSync, `${path}.inSync`);
}

function assertStoredState(stored: unknown, path: string): void {
  const storedState = requireRecord(stored, path);
  requireBoolean(storedState.present, `${path}.present`);
  requireNullableString(storedState.version, `${path}.version`);
  requireNullableString(storedState.updatedAt, `${path}.updatedAt`);
}

function assertAdminAiConfigViewEnvelope(view: unknown): asserts view is Partial<AdminAiConfigViewVO> {
  const root = requireRecord(view, 'data');
  requireNullableString(root.source, 'source');
  requireNullableString(root.version, 'version');
  requireNullableString(root.updatedAt, 'updatedAt');
  const providerNames = assertAiOpsConfigPayload(root.config, 'config');
  assertSecretFields(root.secrets, providerNames, 'secrets');
  assertRuntimeState(root.runtime, 'runtime');
  assertStoredState(root.stored, 'stored');
  requireArray(root.notices, 'notices').forEach((notice, index) => assertConfigNotice(notice, `notices[${index}]`));
}

function assertAdminAiConfigDriftEnvelope(view: unknown): asserts view is Partial<AdminAiConfigDriftVO> {
  const root = requireRecord(view, 'data');
  assertRuntimeState(root.runtime, 'runtime');
  assertStoredState(root.stored, 'stored');
  requireBoolean(root.driftDetected, 'driftDetected');
  requireString(root.syncJobStatus, 'syncJobStatus');
  requireNullableNumber(root.attemptCount, 'attemptCount');
  requireNullableString(root.nextAttemptAt, 'nextAttemptAt');
  requireArray(root.notices, 'notices').forEach((notice, index) => assertConfigNotice(notice, `notices[${index}]`));
}

function normalizeProviderDefinition(definition?: Partial<AiOpsDraftProviderDefinition> | null): AiOpsDraftProviderDefinition {
  return {
    chat: {
      protocol: definition?.chat?.protocol ?? 'openai-compat',
      baseUrl: definition?.chat?.baseUrl ?? null,
      apiKey: definition?.chat?.apiKey ?? null,
      model: definition?.chat?.model ?? null,
      connectTimeout: definition?.chat?.connectTimeout ?? null,
      readTimeout: definition?.chat?.readTimeout ?? null,
      temperature: definition?.chat?.temperature ?? null,
      maxTokens: definition?.chat?.maxTokens ?? null,
    },
    embedding: {
      protocol: definition?.embedding?.protocol ?? 'openai-compat',
      baseUrl: definition?.embedding?.baseUrl ?? null,
      apiKey: definition?.embedding?.apiKey ?? null,
      model: definition?.embedding?.model ?? null,
      multimodalModel: definition?.embedding?.multimodalModel ?? null,
      connectTimeout: definition?.embedding?.connectTimeout ?? null,
      readTimeout: definition?.embedding?.readTimeout ?? null,
      dimension: definition?.embedding?.dimension ?? null,
    },
    rerank: {
      protocol: definition?.rerank?.protocol ?? 'openai-rerank',
      baseUrl: definition?.rerank?.baseUrl ?? null,
      apiKey: definition?.rerank?.apiKey ?? null,
      model: definition?.rerank?.model ?? null,
      multimodalModel: definition?.rerank?.multimodalModel ?? null,
      connectTimeout: definition?.rerank?.connectTimeout ?? null,
      readTimeout: definition?.rerank?.readTimeout ?? null,
    },
  };
}

function sortProviderNames(providerNames: string[], activeProvider?: string | null, fallbackProvider?: string | null): string[] {
  const uniqueNames = Array.from(new Set(providerNames));
  const ordered: string[] = [];
  const pushIfPresent = (providerName?: string | null) => {
    if (!providerName || !uniqueNames.includes(providerName) || ordered.includes(providerName)) {
      return;
    }
    ordered.push(providerName);
  };

  pushIfPresent(activeProvider);
  pushIfPresent(fallbackProvider);
  uniqueNames
    .filter((providerName) => !ordered.includes(providerName))
    .sort((left, right) => left.localeCompare(right))
    .forEach((providerName) => ordered.push(providerName));
  return ordered;
}

function canonicalizeProviderRecord<T>(
  providers: Record<string, T>,
  activeProvider?: string | null,
  fallbackProvider?: string | null
): Record<string, T> {
  return Object.fromEntries(
    sortProviderNames(Object.keys(providers || {}), activeProvider, fallbackProvider)
      .map((providerName) => [providerName, providers[providerName]])
  );
}

function canonicalizeConfigPayload(config: AiOpsDraftConfigPayload): AiOpsDraftConfigPayload {
  return {
    ...config,
    provider: {
      ...config.provider,
      providers: canonicalizeProviderRecord(
        config.provider.providers || {},
        config.provider.activeProvider,
        config.provider.fallbackProvider
      ),
    },
  };
}

function normalizeAiOpsConfigPayload(payload?: Partial<AiOpsDraftConfigPayload> | null): AiOpsDraftConfigPayload {
  return canonicalizeConfigPayload({
    provider: {
      activeProvider: payload?.provider?.activeProvider ?? null,
      fallbackProvider: payload?.provider?.fallbackProvider ?? null,
      providers: Object.fromEntries(
        Object.entries(payload?.provider?.providers || {}).map(([providerName, definition]) => [
          providerName,
          normalizeProviderDefinition(definition),
        ])
      ),
    },
    resilience: {
      maxAttempts: payload?.resilience?.maxAttempts ?? null,
      waitDuration: payload?.resilience?.waitDuration ?? null,
      failureRateThreshold: payload?.resilience?.failureRateThreshold ?? null,
      slidingWindowSize: payload?.resilience?.slidingWindowSize ?? null,
      openStateDuration: payload?.resilience?.openStateDuration ?? null,
    },
    rag: {
      appServer: {
        baseUrl: payload?.rag?.appServer?.baseUrl ?? null,
        internalToken: payload?.rag?.appServer?.internalToken ?? null,
        connectTimeout: payload?.rag?.appServer?.connectTimeout ?? null,
        readTimeout: payload?.rag?.appServer?.readTimeout ?? null,
      },
      ingestion: {
        exportPageSize: payload?.rag?.ingestion?.exportPageSize ?? null,
        embeddingBatchSize: payload?.rag?.ingestion?.embeddingBatchSize ?? null,
        failedRetryEnabled: payload?.rag?.ingestion?.failedRetryEnabled ?? true,
        failedRetryLimit: payload?.rag?.ingestion?.failedRetryLimit ?? 64,
      },
      retrieval: {
        recallTopK: payload?.rag?.retrieval?.recallTopK ?? null,
        recallThreshold: payload?.rag?.retrieval?.recallThreshold ?? null,
        rerankTopN: payload?.rag?.retrieval?.rerankTopN ?? null,
        rerankThreshold: payload?.rag?.retrieval?.rerankThreshold ?? null,
        finalTopK: payload?.rag?.retrieval?.finalTopK ?? null,
        hnswEfSearch: payload?.rag?.retrieval?.hnswEfSearch ?? null,
      },
    },
  });
}

function normalizeSecretField(field?: Partial<AdminAiSecretFieldVO> | null): AdminAiSecretFieldVO {
  return {
    configured: Boolean(field?.configured),
    maskedValue: field?.maskedValue ?? '',
    valueLength: field?.valueLength ?? null,
  };
}

function normalizeConfigNotice(notice?: Partial<AiOpsConfigNotice> | null): AiOpsConfigNotice {
  return {
    code: notice?.code ?? 'legacy_notice',
    severity: notice?.severity ?? 'info',
    defaultMessage: notice?.defaultMessage ?? '',
    args: notice?.args ?? {},
  };
}

export function normalizeAdminAiConfigView(view: unknown): AdminAiConfigViewVO {
  assertAdminAiConfigViewEnvelope(view);
  const normalizedConfig = normalizeAiOpsConfigPayload(view?.config);
  const orderedProviderNames = sortProviderNames(
    [...Object.keys(normalizedConfig.provider.providers || {}), ...Object.keys(view?.secrets?.providers || {})],
    normalizedConfig.provider.activeProvider,
    normalizedConfig.provider.fallbackProvider
  );
  return {
    config: normalizedConfig,
    secrets: {
      providers: Object.fromEntries(
        orderedProviderNames.map((providerName) => {
          const providerSecrets = view?.secrets?.providers?.[providerName];
          return [
          providerName,
          {
            chatApiKey: normalizeSecretField(providerSecrets?.chatApiKey),
            embeddingApiKey: normalizeSecretField(providerSecrets?.embeddingApiKey),
            rerankApiKey: normalizeSecretField(providerSecrets?.rerankApiKey),
          },
          ];
        })
      ),
      appServerInternalToken: normalizeSecretField(view?.secrets?.appServerInternalToken),
    },
    source: view?.source ?? '',
    version: view?.version ?? null,
    updatedAt: view?.updatedAt ?? null,
    notices: Array.isArray(view?.notices) ? view.notices.map((notice) => normalizeConfigNotice(notice as Partial<AiOpsConfigNotice>)) : [],
    runtime: {
      available: Boolean(view?.runtime?.available),
      source: view?.runtime?.source ?? null,
      version: view?.runtime?.version ?? null,
      appliedAt: view?.runtime?.appliedAt ?? null,
      inSync: Boolean(view?.runtime?.inSync),
    },
    stored: {
      present: Boolean(view?.stored?.present),
      version: view?.stored?.version ?? null,
      updatedAt: view?.stored?.updatedAt ?? null,
    },
  };
}

function normalizeAdminAiConfigDrift(view: unknown): AdminAiConfigDriftVO {
  assertAdminAiConfigDriftEnvelope(view);
  return {
    runtime: {
      available: Boolean(view?.runtime?.available),
      source: view?.runtime?.source ?? null,
      version: view?.runtime?.version ?? null,
      appliedAt: view?.runtime?.appliedAt ?? null,
      inSync: Boolean(view?.runtime?.inSync),
    },
    stored: {
      present: Boolean(view?.stored?.present),
      version: view?.stored?.version ?? null,
      updatedAt: view?.stored?.updatedAt ?? null,
    },
    driftDetected: Boolean(view?.driftDetected),
    syncJobStatus: view?.syncJobStatus ?? 'NONE',
    attemptCount: view?.attemptCount ?? null,
    nextAttemptAt: view?.nextAttemptAt ?? null,
    notices: Array.isArray(view?.notices) ? view.notices.map((notice) => normalizeConfigNotice(notice as Partial<AiOpsConfigNotice>)) : [],
  };
}

function createSecretEditor(configured: boolean): SecretEditorState {
  return { retainExisting: configured, value: '' };
}

function createEmptyProviderSecretEditors(): ProviderSecretEditorMap {
  return {
    chatApiKey: createSecretEditor(false),
    embeddingApiKey: createSecretEditor(false),
    rerankApiKey: createSecretEditor(false),
  };
}

function createProviderSecretEditors(view: AdminAiConfigViewVO, providerName: string): ProviderSecretEditorMap {
  const providerSecrets = view.secrets.providers?.[providerName];
  return {
    chatApiKey: createSecretEditor(Boolean(providerSecrets?.chatApiKey?.configured)),
    embeddingApiKey: createSecretEditor(Boolean(providerSecrets?.embeddingApiKey?.configured)),
    rerankApiKey: createSecretEditor(Boolean(providerSecrets?.rerankApiKey?.configured)),
  };
}

function buildSecretEditors(view: AdminAiConfigViewVO): SecretEditorMap {
  const providerNames = sortProviderNames(
    [...Object.keys(view.config.provider.providers || {}), ...Object.keys(view.secrets.providers || {})],
    view.config.provider.activeProvider,
    view.config.provider.fallbackProvider
  );

  return {
    providers: Object.fromEntries(providerNames.map((providerName) => [providerName, createProviderSecretEditors(view, providerName)])),
    appServerInternalToken: createSecretEditor(Boolean(view.secrets.appServerInternalToken?.configured)),
  };
}

function createEmptyProviderDefinition(): AiOpsDraftProviderDefinition {
  return {
    chat: {
      protocol: 'openai-compat',
      baseUrl: '',
      apiKey: null,
      model: '',
      connectTimeout: '',
      readTimeout: '',
      temperature: null,
      maxTokens: null,
    },
    embedding: {
      protocol: 'openai-compat',
      baseUrl: '',
      apiKey: null,
      model: '',
      multimodalModel: null,
      connectTimeout: '',
      readTimeout: '',
      dimension: 1024,
    },
    rerank: {
      protocol: 'openai-rerank',
      baseUrl: '',
      apiKey: null,
      model: '',
      multimodalModel: null,
      connectTimeout: '',
      readTimeout: '',
    },
  };
}

function parseNullableInteger(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const parsed = Number.parseInt(trimmed, 10);
  return Number.isFinite(parsed) ? parsed : null;
}

function parseNullableFloat(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const parsed = Number.parseFloat(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}

function normalizeProviderKey(value: string): string {
  return value.trim();
}

function validateProviderKeyDraft(value: string, existingProviderNames: string[], currentProviderName?: string): string | null {
  const normalized = normalizeProviderKey(value);
  if (!normalized) {
    return 'Provider key 不能为空。';
  }
  if (!providerKeyPattern.test(normalized)) {
    return 'Provider key 仅支持小写字母、数字、连字符和下划线。';
  }
  if (existingProviderNames.includes(normalized) && normalized !== currentProviderName) {
    return `Provider key ${normalized} 已存在。`;
  }
  return null;
}

function renameRecordKey<T>(record: Record<string, T>, currentKey: string, nextKey: string): Record<string, T> {
  return Object.fromEntries(
    Object.entries(record).map(([key, value]) => [key === currentKey ? nextKey : key, value])
  );
}

function sanitizeProviderOrigins(providerOrigins: ProviderOriginMap, providerNames: string[]): ProviderOriginMap | undefined {
  const filteredEntries = Object.entries(providerOrigins).filter(([providerName, originName]) =>
    providerNames.includes(providerName) && Boolean(originName) && providerName !== originName
  );
  return filteredEntries.length > 0 ? Object.fromEntries(filteredEntries) : undefined;
}

function materializeText(value?: string | null): string {
  return value ?? '';
}

function materializeNumber(value?: number | null): number {
  return value ?? 0;
}

function materializeProtocol(value: AiOpsProtocol | null | undefined, fallback: AiOpsProtocol): AiOpsProtocol {
  return value ?? fallback;
}

function materializeProviderDefinition(definition?: AiOpsDraftProviderDefinition | null): AiOpsConfigPayload['provider']['providers'][string] {
  return {
    chat: {
      protocol: materializeProtocol(definition?.chat?.protocol, 'openai-compat'),
      baseUrl: materializeText(definition?.chat?.baseUrl),
      apiKey: definition?.chat?.apiKey ?? null,
      model: materializeText(definition?.chat?.model),
      connectTimeout: materializeText(definition?.chat?.connectTimeout),
      readTimeout: materializeText(definition?.chat?.readTimeout),
      temperature: materializeNumber(definition?.chat?.temperature),
      maxTokens: materializeNumber(definition?.chat?.maxTokens),
    },
    embedding: {
      protocol: materializeProtocol(definition?.embedding?.protocol, 'openai-compat'),
      baseUrl: materializeText(definition?.embedding?.baseUrl),
      apiKey: definition?.embedding?.apiKey ?? null,
      model: materializeText(definition?.embedding?.model),
      multimodalModel: definition?.embedding?.multimodalModel ?? null,
      connectTimeout: materializeText(definition?.embedding?.connectTimeout),
      readTimeout: materializeText(definition?.embedding?.readTimeout),
      dimension: materializeNumber(definition?.embedding?.dimension),
    },
    rerank: {
      protocol: materializeProtocol(definition?.rerank?.protocol, 'openai-rerank'),
      baseUrl: materializeText(definition?.rerank?.baseUrl),
      apiKey: definition?.rerank?.apiKey ?? null,
      model: materializeText(definition?.rerank?.model),
      multimodalModel: definition?.rerank?.multimodalModel ?? null,
      connectTimeout: materializeText(definition?.rerank?.connectTimeout),
      readTimeout: materializeText(definition?.rerank?.readTimeout),
    },
  };
}

function materializeConfigPayload(config: AiOpsDraftConfigPayload): AiOpsConfigPayload {
  const activeProvider = materializeText(config.provider.activeProvider);
  const fallbackProvider = materializeText(config.provider.fallbackProvider);
  return {
    provider: {
      activeProvider,
      fallbackProvider,
      providers: canonicalizeProviderRecord(
        Object.fromEntries(
          Object.entries(config.provider.providers || {}).map(([providerName, definition]) => [
            providerName,
            materializeProviderDefinition(definition),
          ])
        ),
        activeProvider,
        fallbackProvider
      ),
    },
    resilience: {
      maxAttempts: materializeNumber(config.resilience.maxAttempts),
      waitDuration: materializeText(config.resilience.waitDuration),
      failureRateThreshold: materializeNumber(config.resilience.failureRateThreshold),
      slidingWindowSize: materializeNumber(config.resilience.slidingWindowSize),
      openStateDuration: materializeText(config.resilience.openStateDuration),
    },
    rag: {
      appServer: {
        baseUrl: materializeText(config.rag.appServer.baseUrl),
        internalToken: config.rag.appServer.internalToken ?? null,
        connectTimeout: materializeText(config.rag.appServer.connectTimeout),
        readTimeout: materializeText(config.rag.appServer.readTimeout),
      },
      ingestion: {
        exportPageSize: materializeNumber(config.rag.ingestion.exportPageSize),
        embeddingBatchSize: materializeNumber(config.rag.ingestion.embeddingBatchSize),
        failedRetryEnabled: config.rag.ingestion.failedRetryEnabled ?? true,
        failedRetryLimit: config.rag.ingestion.failedRetryLimit ?? 64,
      },
      retrieval: {
        recallTopK: materializeNumber(config.rag.retrieval.recallTopK),
        recallThreshold: materializeNumber(config.rag.retrieval.recallThreshold),
        rerankTopN: materializeNumber(config.rag.retrieval.rerankTopN),
        rerankThreshold: materializeNumber(config.rag.retrieval.rerankThreshold),
        finalTopK: materializeNumber(config.rag.retrieval.finalTopK),
        hnswEfSearch: materializeNumber(config.rag.retrieval.hnswEfSearch),
      },
    },
  };
}

export function buildSavePayload(
  config: AiOpsDraftConfigPayload,
  secrets: SecretEditorMap,
  expectedVersion?: string | null,
  providerOrigins: ProviderOriginMap = {}
): AdminAiConfigSaveRequest {
  const canonicalConfig = canonicalizeConfigPayload(config);
  const strictConfig = materializeConfigPayload(canonicalConfig);
  const orderedProviderNames = sortProviderNames(
    Object.keys(strictConfig.provider.providers || {}),
    strictConfig.provider.activeProvider,
    strictConfig.provider.fallbackProvider
  );
  const sanitizedProviderOrigins = sanitizeProviderOrigins(providerOrigins, orderedProviderNames);
  return {
    config: strictConfig,
    expectedVersion: expectedVersion ?? null,
    providerOrigins: sanitizedProviderOrigins,
    secrets: {
      providers: Object.fromEntries(
        orderedProviderNames.map((providerName) => {
          const providerSecrets = secrets.providers[providerName] || createEmptyProviderSecretEditors();
          return [
          providerName,
          {
            chatApiKey: providerSecrets.chatApiKey,
            embeddingApiKey: providerSecrets.embeddingApiKey,
            rerankApiKey: providerSecrets.rerankApiKey,
          },
          ];
        })
      ),
      appServerInternalToken: secrets.appServerInternalToken,
    },
  };
}

const SectionCard: React.FC<{ title: string; description?: string; children: React.ReactNode }> = ({ title, description, children }) => (
  <section className="min-w-0 space-y-5 rounded-2xl border border-border-subtle bg-surface p-4 shadow-sm sm:space-y-6 sm:rounded-3xl sm:p-5 md:p-6">
    <div className="min-w-0 space-y-2">
      <h2 className="break-words text-lg font-black text-slate-900 dark:text-white">{title}</h2>
      {description && <p className="max-w-3xl break-words text-sm text-slate-500 dark:text-white/45">{description}</p>}
    </div>
    {children}
  </section>
);

const FieldGrid: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div className="grid min-w-0 grid-cols-1 gap-3 sm:gap-4 xl:grid-cols-2">{children}</div>
);

const FieldCard: React.FC<{
  label: string;
  hint?: string;
  detail?: React.ReactNode;
  children: React.ReactNode;
}> = ({ label, hint, detail, children }) => (
  <label className="block min-w-0 space-y-3 rounded-2xl border border-slate-200/70 bg-surface-sunken px-3 py-3 sm:rounded-[1.6rem] sm:px-4 sm:py-4 dark:border-white/10">
    <div className="min-w-0">
      <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">{label}</div>
      {hint && (
        <div className="mt-3 inline-flex min-w-0 items-start gap-2 rounded-2xl border border-sky-500/15 bg-sky-500/[0.04] px-3 py-2 text-xs leading-5 text-slate-600 dark:text-sky-100/80">
          <Info size={14} className="mt-0.5 shrink-0 text-sky-500 dark:text-sky-300" />
          <span className="min-w-0 break-words">{hint}</span>
        </div>
      )}
    </div>
    {children}
    {detail && <div className="rounded-2xl border border-slate-200/60 bg-white/70 px-3 py-3 text-xs leading-5 text-slate-500 dark:border-white/10 dark:bg-slate-950/25 dark:text-white/45">{detail}</div>}
  </label>
);

const TextInput: React.FC<{
  value: string | number | null | undefined;
  onChange: (value: string) => void;
  disabled?: boolean;
  type?: 'text' | 'number' | 'password';
  placeholder?: string;
  step?: string;
}> = ({ value, onChange, disabled, type = 'text', placeholder, step }) => (
  <input
    type={type}
    step={step}
    value={value ?? ''}
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
    className={`shrink-0 whitespace-nowrap rounded-2xl border px-4 py-3 text-sm font-bold transition-all ${
      active
        ? 'border-primary/20 bg-primary/10 text-primary'
        : 'border-slate-200/70 bg-white/50 text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45'
    }`}
  >
    {label}
  </button>
);

const SelectInput: React.FC<{
  value: string | null | undefined;
  onChange: (value: string) => void;
  disabled?: boolean;
  options: ReadonlyArray<{ value: string; label: string }>;
}> = ({ value, onChange, disabled, options }) => (
  <select
    value={value ?? ''}
    onChange={(event) => onChange(event.target.value)}
    disabled={disabled}
    className="native-select w-full rounded-2xl bg-white/80 dark:bg-slate-950/45 border border-slate-200 dark:border-white/10 px-4 py-3 text-sm outline-none focus:border-primary/50 disabled:opacity-60 disabled:cursor-not-allowed"
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
    className={`min-w-0 break-words rounded-2xl border px-3 py-3 text-sm font-semibold sm:px-4 ${
      healthy
        ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400'
        : 'border-rose-500/20 bg-rose-500/5 text-rose-500'
    }`}
  >
    {label}
  </div>
);

const ProbeResultCard: React.FC<{
  title: string;
  result: AdminAiEmbeddingProbeVO | AdminAiRerankProbeVO | null;
  emptyHint: string;
  rows: ProbeMetaRow[];
}> = ({ title, result, emptyHint, rows }) => (
  <div className="min-w-0 space-y-3 rounded-2xl border border-slate-200/70 bg-white/60 p-3 sm:rounded-[1.6rem] sm:p-4 dark:border-white/10 dark:bg-white/[0.03]">
    <div className="flex min-w-0 items-center justify-between gap-3">
      <div className="min-w-0 text-sm font-black text-slate-900 dark:text-white">{title}</div>
      {result && (
        <span className={`shrink-0 rounded-full border px-3 py-1 text-xs font-bold ${result.ok ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-600 dark:text-emerald-400' : 'border-rose-500/20 bg-rose-500/5 text-rose-500'}`}>
          {result.ok ? 'SUCCESS' : 'FAILED'}
        </span>
      )}
    </div>
    {result ? (
      <>
        <div className={`break-words rounded-2xl border px-3 py-3 text-sm ${result.ok ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-700 dark:text-emerald-300' : 'border-rose-500/20 bg-rose-500/5 text-rose-600 dark:text-rose-300'}`}>
          {translateConfigMessage(result.message)}
        </div>
        <div className="grid min-w-0 grid-cols-1 gap-3 text-sm text-slate-600 dark:text-white/60 sm:grid-cols-2">
          {rows.map((row) => (
            <div key={`${title}-${row.label}`} className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/70 px-3 py-3 dark:border-white/10 dark:bg-slate-950/25">
              <div className="text-[11px] uppercase tracking-[0.22em] text-slate-400 dark:text-white/30">{row.label}</div>
              <div className="mt-2 break-all">{row.value}</div>
            </div>
          ))}
        </div>
      </>
    ) : (
      <div className="rounded-2xl border border-dashed border-slate-200 px-4 py-4 text-sm text-slate-500 dark:border-white/10 dark:text-white/45">
        {emptyHint}
      </div>
    )}
  </div>
);

function humanizeFieldName(field: string): string {
  return field
    .replace(/^config\./, '')
    .split('.')
    .map((segment) => fieldTokenLabels[segment] || segment)
    .join(' / ');
}

function formatConfigArgs(args?: Record<string, unknown>): Record<string, string | number> {
  const entries = Object.entries(args || {}).filter(
    (entry): entry is [string, string | number] => typeof entry[1] === 'string' || typeof entry[1] === 'number'
  );
  return Object.fromEntries(entries) as Record<string, string | number>;
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
  if (trimmed.includes('updated by another administrator')) {
    return '配置已被其他管理员更新，请先刷新页面，确认最新版本后再保存。';
  }
  if (trimmed.includes('must be a valid duration')) {
    return '存在非法时长格式，请使用 30s、500ms 或 PT30S 这类格式。';
  }
  if (trimmed.includes('must be a positive duration')) {
    return '时长字段必须大于 0。';
  }
  if (trimmed.includes('provider key must not be blank')) {
    return 'Provider key 不能为空。';
  }
  if (trimmed.includes('provider key must contain only lowercase letters, numbers, hyphen, or underscore')) {
    return 'Provider key 仅支持小写字母、数字、连字符和下划线。';
  }
  if (trimmed.includes('fallbackProvider requires at least two provider definitions')) {
    return 'activeProvider 与 fallbackProvider 必须引用两个不同 provider，至少需要两组 provider 定义。';
  }
  if (trimmed.includes('runtime is unavailable')) {
    return 'ai-gateway 运行态当前不可达，页面正在展示数据库权威快照。';
  }
  if (trimmed.includes('Local schema validation passed; runtime build confirmation is pending')) {
    return '本地结构校验已通过，但 ai-gateway 当前不可达，运行时构建确认将延后执行。';
  }
  if (trimmed.includes('Stored database config was saved locally, but ai-gateway runtime sync is pending')) {
    return '配置已写入数据库，但 ai-gateway 运行态同步待完成。';
  }
  if (trimmed.includes('Stored database config is authoritative but ai-gateway runtime sync is still pending')) {
    return '数据库快照已成为权威版本，但 ai-gateway 运行态仍待同步。';
  }
  if (trimmed.includes('No stored AI ops config exists yet')) {
    return '当前还没有数据库快照，页面展示的是未初始化草稿。';
  }
  if (trimmed.includes('not in sync')) {
    return '数据库配置与 ai-gateway 当前运行态版本不一致。';
  }
  if (trimmed.includes('rag_schema_metadata row was not found')) {
    return '数据库缺少 RAG schema metadata，请先按数据库结构执行手册完成 schema 初始化。';
  }
  if (trimmed.includes('rag_schema_metadata expects')) {
    return '当前数据库中的 pgvector schema 维度与应用配置不一致。';
  }
  if (trimmed.includes('Provider embedding dimensions must match')) {
    return '所有 provider 的 embedding 向量维度必须一致，并与数据库 schema 对齐。';
  }
  return trimmed;
}

function translateConfigIssue(issue: AiOpsConfigIssue): string {
  switch (issue.code) {
    case 'provider_key_invalid':
      return 'Provider key 仅支持小写字母、数字、连字符和下划线。';
    case 'provider_key_required':
      return 'Provider key 不能为空。';
    case 'absolute_url_required':
      return '请填写包含协议和主机名的绝对 URL。';
    case 'invalid_url':
      return 'URL 格式不合法。';
    case 'invalid_duration':
      return '时长格式不合法，请使用 30s、500ms 或 PT30S。';
    case 'positive_duration_required':
      return '时长必须大于 0。';
    case 'must_be_greater_than_zero':
      return '该字段必须大于 0。';
    case 'temperature_out_of_range':
      return '温度必须介于 0 到 2 之间。';
    case 'unsupported_protocol': {
      const args = formatConfigArgs(issue.args);
      return `当前仅支持 ${args.expected || '--'}，收到 ${args.actual || '--'}。`;
    }
    case 'embedding_dimension_mismatch':
      return '所有 provider 的 embedding 向量维度必须一致。';
    case 'embedding_space_model_mismatch':
      return '所有 active/fallback provider 必须使用同一个 embedding 模型，避免跨向量空间检索。';
    case 'embedding_space_multimodal_model_mismatch':
      return '所有 active/fallback provider 必须使用同一个多模态 embedding 模型。';
    case 'online_embedding_space_change_forbidden':
      return '生产向量空间已锁定为 Qwen3-Embedding-8B / 1024 维，不能通过管理端在线切换；请走维护版本与全量 reindex。';
    case 'rerank_top_n_exceeds_recall_top_k':
      return '重排 Top N 不能大于 Recall Top K。';
    case 'final_top_k_exceeds_rerank_top_n':
      return '最终返回 Top K 不能大于重排 Top N。';
    case 'fallback_provider_must_differ':
      return 'fallbackProvider 必须与 activeProvider 不同。';
    case 'provider_reference_missing':
      return '该字段必须引用已定义的 provider key。';
    case 'provider_definitions_required':
      return '至少需要定义一组 provider。';
    case 'provider_count_requires_fallback':
      return 'activeProvider 与 fallbackProvider 必须引用两个不同 provider，至少需要两组 provider 定义。';
    default:
      return translateConfigMessage(issue.defaultMessage);
  }
}

function translateConfigNotice(notice: AiOpsConfigNotice): string {
  const args = formatConfigArgs(notice.args);
  switch (notice.code) {
    case 'automatic_failover_enabled':
      return '当前启用了自动 failover：active provider 发生可重试错误或熔断打开时，会尝试切到 fallback provider。';
    case 'fallback_same_upstream_all':
      return `active=${args.activeProvider || '--'} 与 fallback=${args.fallbackProvider || '--'} 实际指向同一套 chat / embedding / rerank 上游；故障时不会形成真实降级。`;
    case 'fallback_same_upstream_chat':
      return `active=${args.activeProvider || '--'} 与 fallback=${args.fallbackProvider || '--'} 的 Chat 实际指向同一上游；故障时 Chat failover 不会生效。`;
    case 'fallback_same_upstream_embedding':
      return `active=${args.activeProvider || '--'} 与 fallback=${args.fallbackProvider || '--'} 的 Embedding 实际指向同一上游；故障时 Embedding failover 不会生效。`;
    case 'fallback_same_upstream_rerank':
      return `active=${args.activeProvider || '--'} 与 fallback=${args.fallbackProvider || '--'} 的 Rerank 实际指向同一上游；故障时 Rerank failover 不会生效。`;
    case 'runtime_switch_mixed_window':
      return `配置切换后约 ${args.stableWindowSeconds || '--'} 秒内，新旧 bundle 可能混用；建议在低峰时段操作。`;
    case 'runtime_validation_unavailable':
      return '本地结构校验已通过，但 ai-gateway 当前不可达，运行时构建确认将延后执行。';
    case 'runtime_sync_queued':
      return '数据库权威快照已入队，后台正在把该版本同步到 ai-gateway 运行态。';
    case 'runtime_sync_retry_scheduled':
      return '数据库权威快照已保存，但运行态同步上次失败，后台会自动重试。';
    case 'runtime_sync_dlq':
      return '数据库权威快照已保存，但运行态同步已进入终态失败队列，需要人工重放。';
    case 'runtime_unavailable_showing_stored':
      return 'ai-gateway 运行态当前不可达，页面正在展示数据库权威快照。';
    case 'stored_runtime_out_of_sync':
      return '数据库配置与 ai-gateway 当前运行态版本不一致。';
    case 'no_stored_snapshot_yet':
      return '当前还没有数据库快照，页面展示的是未初始化草稿。';
    default:
      return translateConfigMessage(notice.defaultMessage);
  }
}

function formatSecretStatus(field?: AdminAiSecretFieldVO | null): string {
  if (!field?.configured) {
    return '未配置';
  }
  const lengthLabel = typeof field.valueLength === 'number' ? `已配置 · 长度 ${field.valueLength}` : '已配置';
  return field.maskedValue ? `${lengthLabel} · ${field.maskedValue}` : lengthLabel;
}

function summarizeProviderValue(value?: string | number | null): string {
  if (value === null || value === undefined) {
    return '--';
  }
  const normalized = `${value}`.trim();
  return normalized || '--';
}

function describeStoredSyncStatus(status?: string | null): string {
  const normalized = (status || '').toUpperCase();
  if (normalized === 'IN_SYNC') {
    return '启动期数据库快照同步成功';
  }
  if (normalized === 'NO_STORED_CONFIG') {
    return '当前没有数据库快照可同步';
  }
  if (normalized === 'SYNC_FAILED') {
    return '启动期数据库快照同步失败';
  }
  return normalized || '--';
}

function isStoredSyncHealthy(status?: string | null): boolean {
  return (status || '').toUpperCase() !== 'SYNC_FAILED';
}

function describeSyncJobStatus(status?: string | null): string {
  const normalized = (status || '').toUpperCase();
  if (normalized === 'PENDING') {
    return '已入队，等待同步';
  }
  if (normalized === 'FAILED_RETRYING') {
    return '失败后自动重试中';
  }
  if (normalized === 'DLQ') {
    return '终态失败，等待人工重放';
  }
  if (normalized === 'NONE') {
    return '无活动同步任务';
  }
  return normalized || '--';
}

const requiredTextSchema = z.string().trim().min(1, 'value is required');

function parseDurationMillis(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const shortMatch = trimmed.match(/^(\d+(?:\.\d+)?)(ms|s|m|h)$/i);
  if (shortMatch) {
    const amount = Number.parseFloat(shortMatch[1]);
    const unit = shortMatch[2].toLowerCase();
    if (!Number.isFinite(amount)) {
      return null;
    }
    if (unit === 'ms') {
      return amount;
    }
    if (unit === 's') {
      return amount * 1000;
    }
    if (unit === 'm') {
      return amount * 60_000;
    }
    if (unit === 'h') {
      return amount * 3_600_000;
    }
  }
  const isoMatch = trimmed.match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/i);
  if (!isoMatch) {
    return null;
  }
  const hours = isoMatch[1] ? Number.parseInt(isoMatch[1], 10) : 0;
  const minutes = isoMatch[2] ? Number.parseInt(isoMatch[2], 10) : 0;
  const seconds = isoMatch[3] ? Number.parseFloat(isoMatch[3]) : 0;
  return (hours * 3600 + minutes * 60 + seconds) * 1000;
}

const absoluteUrlSchema = requiredTextSchema.superRefine((value, ctx) => {
  try {
    const parsed = new URL(value);
    if (!parsed.protocol || !parsed.hostname) {
      ctx.addIssue({ code: 'custom', message: 'must be an absolute URL' });
    }
  } catch {
    ctx.addIssue({ code: 'custom', message: 'must be a valid URL' });
  }
});

const durationSchema = requiredTextSchema.superRefine((value, ctx) => {
  const millis = parseDurationMillis(value);
  if (millis === null) {
    ctx.addIssue({ code: 'custom', message: 'must be a valid duration' });
    return;
  }
  if (millis <= 0) {
    ctx.addIssue({ code: 'custom', message: 'must be a positive duration' });
  }
});

const providerDefinitionSchema = z.object({
  chat: z.object({
    protocol: protocolSchema.refine(
      (value) => chatProtocolValues.includes(value as (typeof chatProtocolValues)[number]),
      'Unsupported protocol; expected openai-compat or openai-responses'
    ),
    baseUrl: absoluteUrlSchema,
    model: requiredTextSchema.max(128),
    connectTimeout: durationSchema,
    readTimeout: durationSchema,
    temperature: z.number().min(0, 'temperature must be between 0 and 2').max(2, 'temperature must be between 0 and 2'),
    maxTokens: z.number().int().positive('maxTokens must be greater than 0').max(32768),
  }),
  embedding: z.object({
    protocol: protocolSchema.refine((value) => value === 'openai-compat', 'Unsupported protocol \'openai-compat\''),
    baseUrl: absoluteUrlSchema,
    model: requiredTextSchema.max(128),
    multimodalModel: z.string().max(128).nullable().optional(),
    connectTimeout: durationSchema,
    readTimeout: durationSchema,
    dimension: z.number().int().positive('dimension must be greater than 0').max(4096),
  }),
  rerank: z.object({
    protocol: protocolSchema.refine(
      (value) => rerankProtocolValues.includes(value as (typeof rerankProtocolValues)[number]),
      'Unsupported protocol; expected openai-rerank or openai-chat-rerank'
    ),
    baseUrl: absoluteUrlSchema,
    model: requiredTextSchema.max(128),
    multimodalModel: z.string().max(128).nullable().optional(),
    connectTimeout: durationSchema,
    readTimeout: durationSchema,
  }),
});

const aiOpsDraftSchema = z.object({
  provider: z.object({
    activeProvider: requiredTextSchema,
    fallbackProvider: requiredTextSchema,
    providers: z.record(z.string(), providerDefinitionSchema),
  }).superRefine((provider, ctx) => {
    const providerNames = Object.keys(provider.providers || {});
    if (providerNames.length === 0) {
      ctx.addIssue({ code: 'custom', path: ['providers'], message: 'at least one provider definition is required' });
    }
    providerNames.forEach((providerName) => {
      if (!providerKeyPattern.test(providerName)) {
        ctx.addIssue({ code: 'custom', path: ['providers', providerName], message: 'provider key must contain only lowercase letters, numbers, hyphen, or underscore' });
      }
    });
    if (providerNames.length > 0 && providerNames.length < 2) {
      ctx.addIssue({ code: 'custom', path: ['providers'], message: 'fallbackProvider requires at least two provider definitions' });
    }
    if (!providerNames.includes(provider.activeProvider)) {
      ctx.addIssue({ code: 'custom', path: ['activeProvider'], message: 'must reference a configured provider' });
    }
    if (!providerNames.includes(provider.fallbackProvider)) {
      ctx.addIssue({ code: 'custom', path: ['fallbackProvider'], message: 'must reference a configured provider' });
    }
    if (provider.activeProvider === provider.fallbackProvider) {
      ctx.addIssue({ code: 'custom', path: ['fallbackProvider'], message: 'fallbackProvider must be different from activeProvider' });
    }
    const dimensions = providerNames
      .map((providerName) => ({ providerName, dimension: provider.providers[providerName]?.embedding?.dimension }))
      .filter((entry) => typeof entry.dimension === 'number');
    if (dimensions.length > 1) {
      const expectedDimension = dimensions[0].dimension;
      dimensions.slice(1).forEach((entry) => {
        if (entry.dimension !== expectedDimension) {
          ctx.addIssue({ code: 'custom', path: ['providers', entry.providerName, 'embedding', 'dimension'], message: 'all provider embedding dimensions must match' });
        }
      });
    }
    const embeddingModels = providerNames
      .map((providerName) => ({ providerName, model: provider.providers[providerName]?.embedding?.model }))
      .filter((entry) => typeof entry.model === 'string' && entry.model.length > 0);
    if (embeddingModels.length > 1) {
      const expectedModel = embeddingModels[0].model;
      embeddingModels.slice(1).forEach((entry) => {
        if (entry.model !== expectedModel) {
          ctx.addIssue({ code: 'custom', path: ['providers', entry.providerName, 'embedding', 'model'], message: 'all failover providers must use the same embedding model' });
        }
      });
    }
  }),
  resilience: z.object({
    maxAttempts: z.number().int().positive('maxAttempts must be greater than 0').max(5),
    waitDuration: durationSchema,
    failureRateThreshold: z.number().gt(0).lte(100),
    slidingWindowSize: z.number().int().positive('slidingWindowSize must be greater than 0').max(1000),
    openStateDuration: durationSchema,
  }),
  rag: z.object({
    appServer: z.object({
      baseUrl: absoluteUrlSchema,
      connectTimeout: durationSchema,
      readTimeout: durationSchema,
    }),
    ingestion: z.object({
      exportPageSize: z.number().int().positive('exportPageSize must be greater than 0').max(1000),
      embeddingBatchSize: z.number().int().positive('embeddingBatchSize must be greater than 0').max(128),
      failedRetryEnabled: z.boolean().nullable().optional(),
      failedRetryLimit: z.number().int().positive('failedRetryLimit must be greater than 0').max(256).nullable().optional(),
    }),
    retrieval: z.object({
      recallTopK: z.number().int().positive('recallTopK must be greater than 0').max(128),
      recallThreshold: z.number().min(0).max(1),
      rerankTopN: z.number().int().positive('rerankTopN must be greater than 0').max(128),
      rerankThreshold: z.number().min(0).max(1),
      finalTopK: z.number().int().positive('finalTopK must be greater than 0').max(32),
      hnswEfSearch: z.number().int().positive('hnswEfSearch must be greater than 0').max(1000),
    }).superRefine((retrieval, ctx) => {
      if (retrieval.rerankTopN > retrieval.recallTopK) {
        ctx.addIssue({ code: 'custom', path: ['rerankTopN'], message: 'rerankTopN must be less than or equal to recallTopK' });
      }
      if (retrieval.finalTopK > retrieval.rerankTopN) {
        ctx.addIssue({ code: 'custom', path: ['finalTopK'], message: 'finalTopK must be less than or equal to rerankTopN' });
      }
    }),
  }),
});

function localIssue(field: string, code: string, defaultMessage: string, args: Record<string, unknown> = {}): AiOpsConfigIssue {
  return { field, code, defaultMessage, args };
}

function mapLocalIssueCode(message: string): string {
  if (message === 'provider key must contain only lowercase letters, numbers, hyphen, or underscore') {
    return 'provider_key_invalid';
  }
  if (message === 'must be an absolute URL') {
    return 'absolute_url_required';
  }
  if (message === 'must be a valid URL') {
    return 'invalid_url';
  }
  if (message === 'must be a valid duration') {
    return 'invalid_duration';
  }
  if (message === 'must be a positive duration') {
    return 'positive_duration_required';
  }
  if (message === 'must reference a configured provider') {
    return 'provider_reference_missing';
  }
  if (message === 'fallbackProvider must be different from activeProvider') {
    return 'fallback_provider_must_differ';
  }
  if (message === 'rerankTopN must be less than or equal to recallTopK') {
    return 'rerank_top_n_exceeds_recall_top_k';
  }
  if (message === 'finalTopK must be less than or equal to rerankTopN') {
    return 'final_top_k_exceeds_rerank_top_n';
  }
  if (message === 'all provider embedding dimensions must match') {
    return 'embedding_dimension_mismatch';
  }
  if (message === 'all failover providers must use the same embedding model') {
    return 'embedding_space_model_mismatch';
  }
  if (message === 'at least one provider definition is required') {
    return 'provider_definitions_required';
  }
  if (message === 'fallbackProvider requires at least two provider definitions') {
    return 'provider_count_requires_fallback';
  }
  if (message === 'temperature must be between 0 and 2') {
    return 'temperature_out_of_range';
  }
  if (message.includes('must be greater than 0')) {
    return 'must_be_greater_than_zero';
  }
  return 'invalid_value';
}

function collectLocalConfigIssues(config: AiOpsDraftConfigPayload): AiOpsConfigValidationResponse['issues'] {
  const result = aiOpsDraftSchema.safeParse({
    provider: {
      activeProvider: config.provider.activeProvider ?? '',
      fallbackProvider: config.provider.fallbackProvider ?? '',
      providers: config.provider.providers,
    },
    resilience: config.resilience,
    rag: {
      appServer: {
        baseUrl: config.rag.appServer.baseUrl ?? '',
        connectTimeout: config.rag.appServer.connectTimeout ?? '',
        readTimeout: config.rag.appServer.readTimeout ?? '',
      },
      ingestion: config.rag.ingestion,
      retrieval: config.rag.retrieval,
    },
  });

  if (result.success) {
    return [];
  }

  const deduped = new Map<string, AiOpsConfigIssue>();
  result.error.issues.forEach((issue) => {
    const field = Array.isArray(issue.path) ? issue.path.join('.') : String(issue.path ?? 'config');
    const normalizedField = field === 'provider.providers' ? 'provider.providers' : field;
    const mapped = localIssue(normalizedField, mapLocalIssueCode(issue.message), issue.message);
    deduped.set(`${mapped.field}:${mapped.code}:${mapped.defaultMessage}`, mapped);
  });
  return Array.from(deduped.values());
}

function currentConfigVersion(view?: AdminAiConfigViewVO | null): string | null {
  return view?.stored.version ?? view?.runtime.version ?? view?.version ?? null;
}

function buildProviderOptions(providerNames: string[], currentValue: string | null | undefined): Array<{ value: string; label: string }> {
  const options = providerNames.map((providerName) => ({ value: providerName, label: providerName }));
  if (currentValue && !providerNames.includes(currentValue)) {
    return [{ value: currentValue, label: `${currentValue}（当前值无对应 provider）` }, ...options];
  }
  if (options.length === 0) {
    return [{ value: '', label: '暂无 provider 定义' }];
  }
  return options;
}

function normalizeSelectedSourceTypes(sourceTypes: string[]): string[] {
  return allReindexSourceTypes.filter((sourceType) => sourceTypes.includes(sourceType));
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
  if (normalized === 'FAILED' || normalized === 'DLQ') {
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

type ConfigDiffEntry = {
  field: string;
  before: string;
  after: string;
};

type SecretChangeSummary = {
  field: string;
  action: string;
};

type ProbeMetaRow = {
  label: string;
  value: string;
};

type ConfigPreset = {
  key: string;
  label: string;
  description: string;
  apply: (current: AiOpsDraftConfigPayload, providerNames: string[]) => AiOpsDraftConfigPayload;
};

function formatDiffValue(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '--';
  }
  if (typeof value === 'boolean') {
    return value ? 'true' : 'false';
  }
  if (Array.isArray(value)) {
    return value.join(', ') || '--';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

function collectConfigDiffs(base: unknown, next: unknown, path = 'config'): ConfigDiffEntry[] {
  if (isRecord(base) && isRecord(next)) {
    const keys = Array.from(new Set([...Object.keys(base), ...Object.keys(next)])).sort();
    return keys.flatMap((key) => collectConfigDiffs(base[key], next[key], `${path}.${key}`));
  }
  const before = formatDiffValue(base);
  const after = formatDiffValue(next);
  return before === after ? [] : [{ field: path, before, after }];
}

function parseDurationToMs(value?: string | null): number | null {
  const trimmed = value?.trim();
  if (!trimmed) {
    return null;
  }
  if (/^\d+ms$/i.test(trimmed)) {
    return Number(trimmed.replace(/ms/i, ''));
  }
  if (/^\d+(?:\.\d+)?s$/i.test(trimmed)) {
    return Number(trimmed.replace(/s/i, '')) * 1000;
  }
  if (/^\d+(?:\.\d+)?m$/i.test(trimmed)) {
    return Number(trimmed.replace(/m/i, '')) * 60_000;
  }
  const match = /^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/i.exec(trimmed);
  if (!match) {
    return null;
  }
  const hours = Number(match[1] || 0);
  const minutes = Number(match[2] || 0);
  const seconds = Number(match[3] || 0);
  return hours * 3_600_000 + minutes * 60_000 + seconds * 1000;
}

function collectSecretChanges(view: AdminAiConfigViewVO, secrets: SecretEditorMap): SecretChangeSummary[] {
  const changes: SecretChangeSummary[] = [];
  const providerNames = Array.from(
    new Set([...Object.keys(view.secrets.providers || {}), ...Object.keys(secrets.providers || {})])
  );

  providerNames.forEach((providerName) => {
    (['chatApiKey', 'embeddingApiKey', 'rerankApiKey'] as ProviderSecretKey[]).forEach((secretKey) => {
      const baseline = getProviderSecretField(view, providerName, secretKey);
      const editor = secrets.providers?.[providerName]?.[secretKey];
      if (!editor || editor.retainExisting) {
        return;
      }
      const nextValue = editor.value.trim();
      if (!nextValue && !baseline?.configured) {
        return;
      }
      changes.push({
        field: `secrets.providers.${providerName}.${secretKey}`,
        action: nextValue ? (baseline?.configured ? '覆盖现有密钥' : '写入新密钥') : '清空现有密钥',
      });
    });
  });

  if (!secrets.appServerInternalToken.retainExisting) {
    const nextValue = secrets.appServerInternalToken.value.trim();
    if (nextValue || view.secrets.appServerInternalToken.configured) {
      changes.push({
        field: 'secrets.appServerInternalToken',
        action: nextValue
          ? view.secrets.appServerInternalToken.configured
            ? '覆盖现有内部令牌'
            : '写入新的内部令牌'
          : '清空现有内部令牌',
      });
    }
  }

  return changes;
}

function buildConfigRiskHints(view: AdminAiConfigViewVO, config: AiOpsDraftConfigPayload, secrets: SecretEditorMap): string[] {
  const hints: string[] = [];

  if (view.config.provider.activeProvider !== config.provider.activeProvider) {
    hints.push(`当前 Provider 将从 ${view.config.provider.activeProvider || '--'} 切换到 ${config.provider.activeProvider || '--'}。`);
  }
  if (view.config.provider.fallbackProvider !== config.provider.fallbackProvider) {
    hints.push(`备用 Provider 将从 ${view.config.provider.fallbackProvider || '--'} 切换到 ${config.provider.fallbackProvider || '--'}。`);
  }
  if (
    config.resilience.failureRateThreshold !== null &&
    config.resilience.failureRateThreshold !== undefined &&
    view.config.resilience.failureRateThreshold !== null &&
    view.config.resilience.failureRateThreshold !== undefined &&
    config.resilience.failureRateThreshold < view.config.resilience.failureRateThreshold
  ) {
    hints.push('熔断失败率阈值被调低，网关会更早触发熔断和切换。');
  }

  const timeoutComparisons: Array<{ label: string; before?: string | null; after?: string | null }> = [
    { label: 'App Server 连接超时', before: view.config.rag.appServer.connectTimeout, after: config.rag.appServer.connectTimeout },
    { label: 'App Server 读取超时', before: view.config.rag.appServer.readTimeout, after: config.rag.appServer.readTimeout },
    { label: '重试等待时长', before: view.config.resilience.waitDuration, after: config.resilience.waitDuration },
    { label: '熔断打开时长', before: view.config.resilience.openStateDuration, after: config.resilience.openStateDuration },
  ];

  Object.entries(config.provider.providers || {}).forEach(([providerName, provider]) => {
    const baseline = view.config.provider.providers?.[providerName];
    if (!baseline) {
      return;
    }
    timeoutComparisons.push(
      { label: `${providerName} Chat 连接超时`, before: baseline.chat.connectTimeout, after: provider.chat.connectTimeout },
      { label: `${providerName} Chat 读取超时`, before: baseline.chat.readTimeout, after: provider.chat.readTimeout },
      { label: `${providerName} Embedding 连接超时`, before: baseline.embedding.connectTimeout, after: provider.embedding.connectTimeout },
      { label: `${providerName} Embedding 读取超时`, before: baseline.embedding.readTimeout, after: provider.embedding.readTimeout },
      { label: `${providerName} Rerank 连接超时`, before: baseline.rerank.connectTimeout, after: provider.rerank.connectTimeout },
      { label: `${providerName} Rerank 读取超时`, before: baseline.rerank.readTimeout, after: provider.rerank.readTimeout }
    );
  });

  timeoutComparisons.forEach((entry) => {
    const before = parseDurationToMs(entry.before);
    const after = parseDurationToMs(entry.after);
    if (before !== null && after !== null && after < before) {
      hints.push(`${entry.label} 已缩短，弱网或高峰期更容易触发超时失败。`);
    }
  });

  if (
    view.config.rag.retrieval.recallTopK !== config.rag.retrieval.recallTopK ||
    view.config.rag.retrieval.recallThreshold !== config.rag.retrieval.recallThreshold ||
    view.config.rag.retrieval.rerankTopN !== config.rag.retrieval.rerankTopN ||
    view.config.rag.retrieval.rerankThreshold !== config.rag.retrieval.rerankThreshold ||
    view.config.rag.retrieval.finalTopK !== config.rag.retrieval.finalTopK ||
    view.config.rag.retrieval.hnswEfSearch !== config.rag.retrieval.hnswEfSearch
  ) {
    hints.push('RAG 召回或重排参数已调整，建议保存后立刻抽样验证检索结果。');
  }

  const embeddingDimensionChanged = Object.entries(config.provider.providers || {}).some(([providerName, provider]) => {
    const baseline = view.config.provider.providers?.[providerName];
    return baseline && baseline.embedding.dimension !== provider.embedding.dimension;
  });
  if (embeddingDimensionChanged) {
    hints.push('Embedding 向量维度已调整，数据库迁移完成后必须执行全量 reindex。');
  }
  const embeddingModelChanged = Object.entries(config.provider.providers || {}).some(([providerName, provider]) => {
    const baseline = view.config.provider.providers?.[providerName];
    return baseline && baseline.embedding.model !== provider.embedding.model;
  });
  if (embeddingModelChanged) {
    hints.push('Embedding 模型已调整；新旧模型不共享向量空间，保存后必须执行全量强制 reindex。');
  }

  collectSecretChanges(view, secrets).forEach((change) => {
    hints.push(`${humanizeFieldName(change.field)}: ${change.action}。`);
  });

  return Array.from(new Set(hints));
}

function buildReindexRiskHints(form: RagReindexRequest): string[] {
  const hints: string[] = [];
  if ((form.mode || '').toUpperCase() === 'FULL') {
    hints.push('当前会执行 FULL 全量重建，耗时明显高于增量同步。');
  }
  if (form.forceReembed) {
    hints.push('已开启强制重嵌入，会忽略 chunk hash 并重新计算 embedding。');
  }
  if ((form.sourceTypes || []).some((sourceType) => seedReindexSourceTypes.includes(sourceType))) {
    hints.push('当前包含 Seed Source Type，会影响内置知识数据。');
  }
  if ((form.sourceIds || []).length === 0) {
    hints.push('当前没有限定 sourceIds，会按所选 sourceTypes 处理全部数据。');
  }
  return hints;
}

const configPresets: ConfigPreset[] = [
  {
    key: 'local-debug',
    label: '本地联调',
    description: '缩小批量和返回规模，保留现有密钥，适合开发机联调和排错。',
    apply: (current, providerNames) => ({
      ...current,
      provider: {
        ...current.provider,
        activeProvider: current.provider.activeProvider || providerNames[0] || '',
        fallbackProvider: providerNames[1] || current.provider.fallbackProvider || '',
      },
      resilience: {
        ...current.resilience,
        maxAttempts: 1,
        waitDuration: 'PT1S',
        failureRateThreshold: 60,
        slidingWindowSize: 10,
        openStateDuration: 'PT10S',
      },
      rag: {
        ...current.rag,
        ingestion: {
          ...current.rag.ingestion,
          exportPageSize: 50,
          embeddingBatchSize: 8,
        },
        retrieval: {
          ...current.rag.retrieval,
          recallTopK: 12,
          recallThreshold: 0.35,
          rerankTopN: 8,
          rerankThreshold: 0.18,
          finalTopK: 4,
          hnswEfSearch: 32,
        },
      },
    }),
  },
  {
    key: 'single-provider',
    label: '保守双 Provider 运行',
    description: '保留 active/fallback 双 provider，但采用更保守的阈值，适合稳定优先场景。',
    apply: (current, providerNames) => ({
      ...current,
      provider: {
        ...current.provider,
        activeProvider: current.provider.activeProvider || providerNames[0] || '',
        fallbackProvider:
          providerNames.find((providerName) => providerName !== (current.provider.activeProvider || providerNames[0])) ||
          current.provider.fallbackProvider ||
          '',
      },
      resilience: {
        ...current.resilience,
        maxAttempts: 2,
        waitDuration: 'PT2S',
        failureRateThreshold: 55,
        slidingWindowSize: 20,
        openStateDuration: 'PT30S',
      },
      rag: {
        ...current.rag,
        ingestion: {
          ...current.rag.ingestion,
          exportPageSize: 100,
          embeddingBatchSize: 16,
        },
        retrieval: {
          ...current.rag.retrieval,
          recallTopK: 24,
          recallThreshold: 0.3,
          rerankTopN: 12,
          rerankThreshold: 0.15,
          finalTopK: 6,
          hnswEfSearch: 64,
        },
      },
    }),
  },
  {
    key: 'dual-provider',
    label: '双 Provider 容灾',
    description: '显式启用 active/fallback 组合，保留较强恢复能力，适合生产容灾。',
    apply: (current, providerNames) => ({
      ...current,
      provider: {
        ...current.provider,
        activeProvider: current.provider.activeProvider || providerNames[0] || '',
        fallbackProvider:
          providerNames.find((providerName) => providerName !== (current.provider.activeProvider || providerNames[0])) ||
          current.provider.fallbackProvider ||
          '',
      },
      resilience: {
        ...current.resilience,
        maxAttempts: 3,
        waitDuration: 'PT2S',
        failureRateThreshold: 40,
        slidingWindowSize: 20,
        openStateDuration: 'PT45S',
      },
      rag: {
        ...current.rag,
        ingestion: {
          ...current.rag.ingestion,
          exportPageSize: 100,
          embeddingBatchSize: 16,
        },
        retrieval: {
          ...current.rag.retrieval,
          recallTopK: 50,
          recallThreshold: 0.45,
          rerankTopN: 16,
          rerankThreshold: 0.2,
          finalTopK: 8,
          hnswEfSearch: 128,
        },
      },
    }),
  },
];

function getProviderSecretField(view: AdminAiConfigViewVO, providerName: string, key: ProviderSecretKey): AdminAiSecretFieldVO | null {
  return view.secrets.providers?.[providerName]?.[key] || null;
}

const AdminConfigCenterPage: React.FC = () => {
  const queryClient = useQueryClient();
  const configQuery = useQuery({
    queryKey: ['admin-ai-config'],
    queryFn: async ({ signal }) => {
      try {
        return normalizeAdminAiConfigView(await adminService.getAiConfig({ signal }));
      } catch (error) {
        if (error instanceof AdminAiConfigContractError) {
          throw error;
        }
        throw error instanceof Error ? error : new Error('加载运维配置失败');
      }
    },
    retry: (failureCount, error) => {
      // Contract mismatches will not heal by retrying the same payload.
      if (error instanceof AdminAiConfigContractError) {
        return false;
      }
      return failureCount < 1;
    },
  });

  const [activeTab, setActiveTab] = React.useState<ConfigTab>('provider');
  const [editing, setEditing] = React.useState(false);
  const [config, setConfig] = React.useState<AiOpsDraftConfigPayload | null>(null);
  const [secrets, setSecrets] = React.useState<SecretEditorMap | null>(null);
  const [providerOrigins, setProviderOrigins] = React.useState<ProviderOriginMap>({});
  const [validation, setValidation] = React.useState<AiOpsConfigValidationResponse | null>(null);
  const [feedback, setFeedback] = React.useState<{ tone: 'success' | 'error'; message: string } | null>(null);
  const [saveReviewOpen, setSaveReviewOpen] = React.useState(false);
  const [newProviderName, setNewProviderName] = React.useState('');
  const [renamingProviderName, setRenamingProviderName] = React.useState<string | null>(null);
  const [renameProviderDraft, setRenameProviderDraft] = React.useState('');
  const [healthState, setHealthState] = React.useState<AiGatewayHealthResponse | null>(null);
  const [driftPollingActive, setDriftPollingActive] = React.useState(false);
  const [embeddingProbeResult, setEmbeddingProbeResult] = React.useState<AdminAiEmbeddingProbeVO | null>(null);
  const [rerankProbeResult, setRerankProbeResult] = React.useState<AdminAiRerankProbeVO | null>(null);
  const [reindexForm, setReindexForm] = React.useState<RagReindexRequest>({
    mode: 'INCREMENTAL',
    sourceTypes: defaultLexicalSourceTypes,
    sourceIds: [],
    forceReembed: false,
  });
  const [reindexConfirmOpen, setReindexConfirmOpen] = React.useState(false);
  const [jobId, setJobId] = React.useState<string | null>(null);
  const [pollJob, setPollJob] = React.useState(false);
  const [outboxStatus, setOutboxStatus] = React.useState('FAILED');
  const [outboxLimit, setOutboxLimit] = React.useState('20');
  const [replayingOutboxId, setReplayingOutboxId] = React.useState<number | null>(null);

  const driftQuery = useQuery({
    queryKey: ['admin-ai-drift'],
    queryFn: async ({ signal }) => normalizeAdminAiConfigDrift(await adminService.getAiDrift({ signal })),
    enabled: driftPollingActive,
    refetchInterval: driftPollingActive ? 3_000 : false,
    staleTime: 0,
  });

  React.useEffect(() => {
    if (!configQuery.data) {
      return;
    }
    setConfig(cloneConfig(configQuery.data.config));
    setSecrets(buildSecretEditors(configQuery.data));
    setProviderOrigins({});
    setValidation(null);
    setNewProviderName('');
    setRenamingProviderName(null);
    setRenameProviderDraft('');
    setEmbeddingProbeResult(null);
    setRerankProbeResult(null);
  }, [configQuery.data]);

  const clearProbeResults = React.useCallback(() => {
    setEmbeddingProbeResult(null);
    setRerankProbeResult(null);
  }, []);

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
      const normalizedResponse = normalizeAdminAiConfigView(response);
      queryClient.setQueryData<AdminAiConfigViewVO>(['admin-ai-config'], normalizedResponse);
      setEditing(false);
      setSaveReviewOpen(false);
      setValidation(null);
      setHealthState(null);
      setDriftPollingActive(false);
      clearProbeResults();
      setConfig(cloneConfig(normalizedResponse.config));
      setSecrets(buildSecretEditors(normalizedResponse));
      setProviderOrigins({});
      setNewProviderName('');
      setRenamingProviderName(null);
      setRenameProviderDraft('');
      setFeedback({
        tone: 'success',
        message:
          normalizedResponse.runtime.available && normalizedResponse.runtime.inSync
            ? '配置已保存，数据库与 ai-gateway 运行态已同步到同一版本。'
            : '配置已保存到数据库，后台正在同步 ai-gateway 运行态；如失败会自动重试。',
      });
      if (!normalizedResponse.runtime.inSync) {
        setDriftPollingActive(true);
        void driftQuery.refetch();
      }
    },
    onError: (error: Error, payload) => {
      if (error instanceof ApiError && error.status === 409) {
        setFeedback({ tone: 'error', message: '配置已被其他管理员更新，请刷新页面后重新比对并保存。' });
        return;
      }
      if (error instanceof ApiError && error.status === 400 && error.code === 'VALIDATION_ERROR') {
        setFeedback({ tone: 'error', message: '保存失败，已按当前草稿重新执行校验，请先修正字段错误。' });
        validateMutation.mutate(payload);
        return;
      }
      setFeedback({ tone: 'error', message: translateConfigMessage(error.message) });
    },
  });

  const syncRuntimeMutation = useMutation({
    mutationFn: (payload: AdminAiRuntimeSyncRequest) => adminService.syncAiRuntime(payload),
    onSuccess: (response) => {
      const normalizedResponse = normalizeAdminAiConfigView(response);
      queryClient.setQueryData<AdminAiConfigViewVO>(['admin-ai-config'], normalizedResponse);
      setConfig(cloneConfig(normalizedResponse.config));
      setSecrets(buildSecretEditors(normalizedResponse));
      setProviderOrigins({});
      setDriftPollingActive(!normalizedResponse.runtime.inSync);
      setFeedback({
        tone: normalizedResponse.runtime.available && normalizedResponse.runtime.inSync ? 'success' : 'error',
        message:
          normalizedResponse.runtime.available && normalizedResponse.runtime.inSync
            ? '数据库权威快照已重新同步到 ai-gateway 运行态。'
            : '已触发运行态同步；若本次未成功，后台会继续自动重试。',
      });
      if (!normalizedResponse.runtime.inSync) {
        void driftQuery.refetch();
      }
    },
    onError: (error: Error) => {
      setFeedback({ tone: 'error', message: translateConfigMessage(error.message) });
    },
  });

  const healthMutation = useMutation({
    mutationFn: () => adminService.getAiHealth(),
    onSuccess: (response) => {
      setHealthState(response);
      setFeedback({ tone: 'success', message: 'ai-gateway 运行态健康信息已刷新。' });
    },
    onError: (error: Error) => {
      setFeedback({ tone: 'error', message: translateConfigMessage(error.message) });
    },
  });

  React.useEffect(() => {
    if (!driftPollingActive || !driftQuery.data) {
      return;
    }
    void queryClient.invalidateQueries({ queryKey: ['admin-ai-outbox'] });
    if (!driftQuery.data.driftDetected || driftQuery.data.syncJobStatus === 'DLQ') {
      setDriftPollingActive(false);
      void queryClient.invalidateQueries({ queryKey: ['admin-ai-config'] });
    }
  }, [driftPollingActive, driftQuery.data, queryClient]);

  const embeddingProbeMutation = useMutation({
    mutationFn: (payload: AdminAiConfigSaveRequest) => adminService.probeAiEmbedding(payload),
    onSuccess: (response) => {
      setEmbeddingProbeResult(response);
      setFeedback({
        tone: response.ok ? 'success' : 'error',
        message: response.ok ? 'Embedding 测试连接成功。' : `Embedding 测试失败：${translateConfigMessage(response.message)}`,
      });
    },
    onError: (error: Error) => {
      setEmbeddingProbeResult(null);
      setFeedback({ tone: 'error', message: translateConfigMessage(error.message) });
    },
  });

  const rerankProbeMutation = useMutation({
    mutationFn: (payload: AdminAiConfigSaveRequest) => adminService.probeAiRerank(payload),
    onSuccess: (response) => {
      setRerankProbeResult(response);
      setFeedback({
        tone: response.ok ? 'success' : 'error',
        message: response.ok ? 'Rerank 测试连接成功。' : `Rerank 测试失败：${translateConfigMessage(response.message)}`,
      });
    },
    onError: (error: Error) => {
      setRerankProbeResult(null);
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
      setFeedback({
        tone: record.status === 'FAILED' ? 'error' : 'success',
        message:
          record.status === 'FAILED'
            ? `Outbox 事件 #${record.id} 已立即重放，但发送仍失败，请查看最新错误信息。`
            : `Outbox 事件 #${record.id} 已重新投递，当前状态：${record.status}。`,
      });
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
    queryFn: ({ signal }) => adminService.getRagReindexJob(jobId as string, { signal }),
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

  const updateConfig = React.useCallback((updater: (current: AiOpsDraftConfigPayload) => AiOpsDraftConfigPayload) => {
    clearProbeResults();
    setConfig((current) => (current ? canonicalizeConfigPayload(updater(current)) : current));
  }, [clearProbeResults]);

  const updateProviderDefinition = React.useCallback((
    providerName: string,
    updater: (current: AiOpsDraftProviderDefinition) => AiOpsDraftProviderDefinition
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
    clearProbeResults();
    setSecrets((current) => {
      if (!current) {
        return current;
      }
      const providerSecrets = current.providers[providerName] || createEmptyProviderSecretEditors();
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
  }, [clearProbeResults]);

  const addProvider = React.useCallback(() => {
    if (!config || !secrets) {
      return;
    }
    const providerName = normalizeProviderKey(newProviderName);
    const validationMessage = validateProviderKeyDraft(providerName, Object.keys(config.provider.providers || {}));
    if (validationMessage) {
      setFeedback({ tone: 'error', message: validationMessage });
      return;
    }
    updateConfig((current) => ({
      ...current,
      provider: {
        ...current.provider,
        providers: {
          ...current.provider.providers,
          [providerName]: createEmptyProviderDefinition(),
        },
      },
    }));
    setSecrets((current) => current ? {
      ...current,
      providers: {
        ...current.providers,
        [providerName]: createEmptyProviderSecretEditors(),
      },
    } : current);
    setNewProviderName('');
    setValidation(null);
    setFeedback({ tone: 'success', message: `已新增 provider：${providerName}。` });
  }, [config, newProviderName, secrets, updateConfig]);

  const startRenameProvider = React.useCallback((providerName: string) => {
    setRenamingProviderName(providerName);
    setRenameProviderDraft(providerName);
    setFeedback(null);
  }, []);

  const cancelRenameProvider = React.useCallback(() => {
    setRenamingProviderName(null);
    setRenameProviderDraft('');
  }, []);

  const renameProvider = React.useCallback((providerName: string) => {
    if (!config || !secrets) {
      return;
    }
    const nextProviderName = normalizeProviderKey(renameProviderDraft);
    const validationMessage = validateProviderKeyDraft(
      nextProviderName,
      Object.keys(config.provider.providers || {}),
      providerName
    );
    if (validationMessage) {
      setFeedback({ tone: 'error', message: validationMessage });
      return;
    }
    if (nextProviderName === providerName) {
      cancelRenameProvider();
      return;
    }
    updateConfig((current) => ({
      ...current,
      provider: {
        ...current.provider,
        activeProvider: current.provider.activeProvider === providerName ? nextProviderName : current.provider.activeProvider,
        fallbackProvider: current.provider.fallbackProvider === providerName ? nextProviderName : current.provider.fallbackProvider,
        providers: renameRecordKey(current.provider.providers, providerName, nextProviderName),
      },
    }));
    setSecrets((current) => current ? {
      ...current,
      providers: renameRecordKey(current.providers, providerName, nextProviderName),
    } : current);
    setProviderOrigins((current) => {
      const nextOrigins = { ...current };
      const originName = current[providerName] || providerName;
      delete nextOrigins[providerName];
      if (originName !== nextProviderName) {
        nextOrigins[nextProviderName] = originName;
      }
      return nextOrigins;
    });
    setValidation(null);
    setFeedback({ tone: 'success', message: `Provider 已重命名为 ${nextProviderName}。` });
    setRenamingProviderName(null);
    setRenameProviderDraft('');
  }, [cancelRenameProvider, config, renameProviderDraft, secrets, updateConfig]);

  const deleteProvider = React.useCallback((providerName: string) => {
    if (!config || !secrets) {
      return;
    }
    if (providerName === config.provider.activeProvider || providerName === config.provider.fallbackProvider) {
      setFeedback({ tone: 'error', message: '当前 active / fallback provider 不能直接删除，请先切换引用。' });
      return;
    }
    if (Object.keys(config.provider.providers || {}).length <= 2) {
      setFeedback({ tone: 'error', message: 'activeProvider 与 fallbackProvider 必须引用两个不同 provider，至少需要保留两组 provider 定义。' });
      return;
    }
    updateConfig((current) => {
      const nextProviders = { ...current.provider.providers };
      delete nextProviders[providerName];
      return {
        ...current,
        provider: {
          ...current.provider,
          providers: nextProviders,
        },
      };
    });
    setSecrets((current) => {
      if (!current) {
        return current;
      }
      const nextProviders = { ...current.providers };
      delete nextProviders[providerName];
      return {
        ...current,
        providers: nextProviders,
      };
    });
    setProviderOrigins((current) => {
      const nextOrigins = { ...current };
      delete nextOrigins[providerName];
      return nextOrigins;
    });
    if (renamingProviderName === providerName) {
      setRenamingProviderName(null);
      setRenameProviderDraft('');
    }
    setValidation(null);
    setFeedback({ tone: 'success', message: `已删除 provider：${providerName}。` });
  }, [config, renamingProviderName, secrets, updateConfig]);

  const updateAppServerSecret = React.useCallback((patch: Partial<SecretEditorState>) => {
    clearProbeResults();
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
  }, [clearProbeResults]);

  const toggleReindexSourceType = React.useCallback((sourceType: string, checked: boolean) => {
    setReindexForm((current) => {
      const selected = new Set(current.sourceTypes || []);
      if (checked) {
        selected.add(sourceType);
      } else {
        selected.delete(sourceType);
      }
      return {
        ...current,
        sourceTypes: normalizeSelectedSourceTypes(Array.from(selected)),
      };
    });
  }, []);

  const resetDraft = React.useCallback(() => {
    if (!configQuery.data) {
      return;
    }
    clearProbeResults();
    setConfig(cloneConfig(configQuery.data.config));
    setSecrets(buildSecretEditors(configQuery.data));
    setProviderOrigins({});
    setValidation(null);
    setFeedback(null);
    setNewProviderName('');
    setRenamingProviderName(null);
    setRenameProviderDraft('');
  }, [clearProbeResults, configQuery.data]);

  const localDraftIssues = React.useMemo(
    () => (config ? collectLocalConfigIssues(config) : []),
    [config]
  );

  const submitValidation = () => {
    if (!config || !secrets) {
      return;
    }
    const localIssues = collectLocalConfigIssues(config);
    if (localIssues.length > 0) {
      setValidation({ valid: false, issues: localIssues, notices: [] });
      setFeedback({ tone: 'error', message: '配置校验未通过，请先修正本地字段错误。' });
      return;
    }
    setFeedback(null);
    validateMutation.mutate(buildSavePayload(config, secrets, currentConfigVersion(configQuery.data), providerOrigins));
  };

  const submitSave = () => {
    if (!config || !secrets || !configQuery.data) {
      return;
    }
    const configDiffs = collectConfigDiffs(configQuery.data.config, config, 'config');
    const secretChanges = collectSecretChanges(configQuery.data, secrets);
    if (configDiffs.length === 0 && secretChanges.length === 0) {
      setFeedback({ tone: 'success', message: '当前没有未保存改动。' });
      return;
    }
    setFeedback(null);
    setSaveReviewOpen(true);
  };

  const confirmSave = () => {
    if (!config || !secrets) {
      return;
    }
    const localIssues = collectLocalConfigIssues(config);
    if (localIssues.length > 0) {
      setValidation({ valid: false, issues: localIssues, notices: [] });
      setFeedback({ tone: 'error', message: '保存前请先修正本地字段错误。' });
      return;
    }
    setFeedback(null);
    saveMutation.mutate(buildSavePayload(config, secrets, currentConfigVersion(configQuery.data), providerOrigins));
  };

  const triggerEmbeddingProbe = () => {
    if (!config || !secrets || !configQuery.data) {
      return;
    }
    const localIssues = collectLocalConfigIssues(config);
    if (localIssues.length > 0) {
      setValidation({ valid: false, issues: localIssues, notices: [] });
      setFeedback({ tone: 'error', message: '测试前请先修正本地字段错误。' });
      return;
    }
    setFeedback(null);
    embeddingProbeMutation.mutate(buildSavePayload(config, secrets, currentConfigVersion(configQuery.data), providerOrigins));
  };

  const triggerRerankProbe = () => {
    if (!config || !secrets || !configQuery.data) {
      return;
    }
    const localIssues = collectLocalConfigIssues(config);
    if (localIssues.length > 0) {
      setValidation({ valid: false, issues: localIssues, notices: [] });
      setFeedback({ tone: 'error', message: '测试前请先修正本地字段错误。' });
      return;
    }
    setFeedback(null);
    rerankProbeMutation.mutate(buildSavePayload(config, secrets, currentConfigVersion(configQuery.data), providerOrigins));
  };

  const currentIssues = validation?.issues ?? (editing ? localDraftIssues : []);
  const validationNotices = validation?.notices ?? [];

  if (configQuery.error) {
    const isContractError = configQuery.error instanceof AdminAiConfigContractError;
    const errorState = getProductizedErrorState(configQuery.error, {
      resourceLabel: '运维配置',
      taskLabel: '查看 AI 运行态',
      retryActionLabel: '重新加载配置',
    });
    return (
      <div className="page-stack pb-16 sm:pb-20">
        <PageHeader title="运维管理员配置中心" subtitle="加载失败时不隐藏原因，直接显示后端返回错误。" />
        <FeedbackState
          kind={isContractError ? 'error' : errorState.kind}
          title={isContractError ? '配置响应格式异常' : errorState.title}
          description={isContractError ? configQuery.error.message : errorState.description}
          impact={isContractError ? '页面已拦截异常响应，不会改写任何配置或运行态。' : errorState.impact}
          nextStep={isContractError ? '请刷新重试；若持续失败，检查 app-server / ai-gateway 契约版本是否一致。' : errorState.nextStep}
          primaryAction={{ label: '重新加载配置', onClick: () => void configQuery.refetch() }}
        />
      </div>
    );
  }

  if (configQuery.isLoading || !configQuery.data || !config || !secrets) {
    return (
      <div className="page-stack pb-16 sm:pb-20">
        <PageHeader title="运维管理员配置中心" subtitle="正在读取 ai-gateway 运行态和数据库存储快照。" />
        <FeedbackState
          kind="loading"
          title="正在加载运维配置"
          description="系统正在读取 ai-gateway 运行态和数据库存储快照。"
          impact="当前只读配置，不会修改任何运行参数或数据。"
          nextStep="请稍等；读取完成后会显示可编辑配置。"
        />
      </div>
    );
  }

  const view = configQuery.data;
  const providerEntries = Object.entries(config.provider.providers || {});
  const providerNames = providerEntries.map(([providerName]) => providerName);
  const activeProviderOptions = buildProviderOptions(providerNames, config.provider.activeProvider);
  const fallbackProviderOptions = buildProviderOptions(providerNames, config.provider.fallbackProvider);
  const activeProviderName = config.provider.activeProvider || undefined;
  const activeProviderDefinition = activeProviderName ? config.provider.providers?.[activeProviderName] : undefined;
  const runtimeUnavailable = !view.runtime.available;
  const runtimeOutOfSync = view.runtime.available && view.stored.present && !view.runtime.inSync;
  const displayedSnapshot = view.stored.present ? 'app-server 数据库权威快照' : view.runtime.available ? 'ai-gateway 启动初始化快照' : '未初始化草稿';
  const storedSyncLabel = !view.stored.present && !view.runtime.available
    ? 'DRAFT_ONLY'
    : !view.stored.present
      ? 'NO_DB_SNAPSHOT'
      : !view.runtime.available
        ? 'RUNTIME_UNKNOWN'
        : view.runtime.inSync
          ? 'IN_SYNC'
          : 'OUT_OF_SYNC';
  const activeTabDescription = tabDescriptions[activeTab];
  const busyMessage = saveMutation.isPending
    ? '正在保存配置，并尝试同步 ai-gateway 运行态，请勿重复提交。'
    : validateMutation.isPending
      ? '正在校验配置，校验结果会直接展示在当前页。'
      : embeddingProbeMutation.isPending
        ? '正在执行 Embedding 测试连接，这会消耗 1 次额度。'
        : rerankProbeMutation.isPending
          ? '正在执行 Rerank 测试连接，这会消耗 1 次额度。'
      : syncRuntimeMutation.isPending
        ? '正在将数据库权威快照重新同步到 ai-gateway 运行态。'
      : healthMutation.isPending
        ? '正在刷新 ai-gateway 运行态健康信息。'
      : pollJob && reindexJobQuery.data
        ? `RAG reindex #${reindexJobQuery.data.jobId} 正在执行，页面每 2 秒自动刷新一次状态。`
      : pollJob && jobId !== null
          ? `RAG reindex #${jobId} 正在执行，页面每 2 秒自动刷新一次状态。`
          : outboxQuery.isFetching && activeTab === 'operations'
            ? '正在刷新 producer outbox 状态。'
            : null;
  const embeddingProbeRows: ProbeMetaRow[] = embeddingProbeResult ? [
    { label: 'Provider', value: embeddingProbeResult.provider || '--' },
    { label: 'Model', value: embeddingProbeResult.model || '--' },
    { label: 'Latency', value: `${embeddingProbeResult.latencyMs} ms` },
    { label: 'Dimension', value: `${embeddingProbeResult.dimension ?? '--'} / ${embeddingProbeResult.expectedDimension ?? '--'}` },
    { label: 'Items', value: String(embeddingProbeResult.itemCount ?? '--') },
    { label: 'Relevant similarity', value: embeddingProbeResult.relatedSimilarity?.toFixed(4) ?? '--' },
    { label: 'Unrelated similarity', value: embeddingProbeResult.unrelatedSimilarity?.toFixed(4) ?? '--' },
    { label: 'Similarity margin', value: embeddingProbeResult.similarityMargin?.toFixed(4) ?? '--' },
    { label: 'Provider compatibility', value: embeddingProbeResult.providerCompatibility?.toFixed(4) ?? '--' },
    { label: 'Providers checked', value: String(embeddingProbeResult.providersChecked ?? '--') },
    { label: 'Tested At', value: formatDateTime(embeddingProbeResult.testedAt) },
  ] : [];
  const rerankProbeRows: ProbeMetaRow[] = rerankProbeResult ? [
    { label: 'Provider', value: rerankProbeResult.provider || '--' },
    { label: 'Model', value: rerankProbeResult.model || '--' },
    { label: 'Latency', value: `${rerankProbeResult.latencyMs} ms` },
    { label: 'Returned', value: `${rerankProbeResult.returnedCount ?? '--'} / ${rerankProbeResult.documentsCount ?? '--'}` },
    { label: 'Top Result', value: rerankProbeResult.topDocumentIndex == null ? '--' : `doc #${rerankProbeResult.topDocumentIndex}` },
    { label: 'Providers checked', value: String(rerankProbeResult.providersChecked ?? '--') },
    { label: 'Tested At', value: formatDateTime(rerankProbeResult.testedAt) },
  ] : [];
  const reindexStatusMeta = buildReindexStatusMeta(reindexJobQuery.data);
  const reindexStats = formatStats(reindexJobQuery.data?.stats);
  const outboxRecords = outboxQuery.data || [];
  const configDiffs = collectConfigDiffs(view.config, config, 'config');
  const secretChanges = collectSecretChanges(view, secrets);
  const draftRiskHints = buildConfigRiskHints(view, config, secrets);
  const reindexRiskHints = buildReindexRiskHints(reindexForm);
  const visibleDiffs = configDiffs.slice(0, 10);
  const visibleSecretChanges = secretChanges.slice(0, 6);
  const configWorkflowStages: WorkflowStage[] = [
    {
      key: 'view',
      label: '查看当前值',
      status: !editing ? 'current' : 'complete',
      statusLabel: !editing ? '当前步骤' : '已完成',
      reason: '以数据库快照为权威值，同时显示 runtime 是否同步。',
      fallback: 'runtime 不可用时仍保留快照，不覆盖已保存配置。',
      saveState: view.stored.present ? `已保存版本 ${view.stored.version ?? '--'}` : '尚无数据库快照',
      nextAction: '确认来源、版本和权限后进入编辑。',
      onSelect: !editing ? undefined : () => { setEditing(false); resetDraft(); },
    },
    {
      key: 'edit',
      label: '编辑草稿',
      status: editing && !validation ? 'current' : editing ? 'complete' : 'pending',
      statusLabel: editing && !validation ? '编辑中' : editing ? '已完成' : '待处理',
      reason: editing ? '只修改本地草稿，密钥默认保留已有值。' : '尚未进入编辑模式。',
      fallback: '取消即可丢弃未保存改动，不影响 runtime。',
      saveState: configDiffs.length + secretChanges.length > 0 ? `${configDiffs.length + secretChanges.length} 项改动待确认` : '暂无改动',
      nextAction: editing ? '完成后先执行配置校验。' : '点击“进入编辑”。',
      onSelect: !editing ? () => { setEditing(true); setFeedback(null); } : undefined,
    },
    {
      key: 'validate',
      label: '校验',
      status: validation?.valid ? 'complete' : editing && currentIssues.length > 0 ? 'blocked' : editing ? 'current' : 'pending',
      statusLabel: validation?.valid ? '通过' : currentIssues.length > 0 ? '需修正' : editing ? '待校验' : '待处理',
      reason: currentIssues.length > 0 ? '发现字段错误，保存被阻止。' : '校验会检查协议、provider 引用、超时和数值范围。',
      fallback: '校验失败不会写入数据库，也不会改变 runtime。',
      saveState: validation?.valid ? '最近一次校验通过' : '尚未形成可保存结果',
      nextAction: editing ? '点击“校验配置”查看完整结果。' : '先进入编辑。',
      onSelect: editing ? submitValidation : undefined,
      disabled: !editing || validateMutation.isPending,
    },
    {
      key: 'preview',
      label: '预览影响',
      status: saveReviewOpen ? 'current' : validation?.valid && (configDiffs.length + secretChanges.length > 0) ? 'warning' : 'pending',
      statusLabel: saveReviewOpen ? '预览中' : validation?.valid ? '可预览' : '待校验',
      reason: '保存前展示字段 diff、密钥动作和高风险提示。',
      fallback: '可返回继续编辑；预览不会触发 API 写入。',
      saveState: saveReviewOpen ? '变更评审窗口已打开' : '尚未打开变更评审',
      nextAction: validation?.valid ? '点击“保存并生效”打开变更评审。' : '先通过校验。',
      onSelect: validation?.valid ? submitSave : undefined,
      disabled: !validation?.valid || saveMutation.isPending,
    },
    {
      key: 'publish',
      label: '保存并发布',
      status: saveMutation.isPending ? 'current' : feedback?.tone === 'success' ? 'complete' : validation?.valid ? 'warning' : 'pending',
      statusLabel: saveMutation.isPending ? '保存中' : feedback?.tone === 'success' ? '已生效' : validation?.valid ? '待确认' : '待处理',
      reason: '确认后写入数据库，并尝试同步 ai-gateway；失败会保留可回退版本。',
      fallback: '同步失败不隐藏保存结果，可从运维操作重试。',
      saveState: saveMutation.isPending ? '正在提交' : feedback?.tone === 'success' ? '本次变更已记录' : '尚未提交',
      nextAction: validation?.valid ? '在评审窗口确认保存。' : '先完成校验与影响预览。',
      onSelect: validation?.valid ? submitSave : undefined,
      disabled: !validation?.valid || saveMutation.isPending,
    },
  ];

  return (
    <div className="page-stack pb-16 sm:pb-20">
      <PageHeader
        title="运维管理员配置中心"
        subtitle="页面以 app-server 数据库快照作为权威配置源，同时展示 ai-gateway 当前已应用的运行态。"
        actions={
          <div className="page-actions">
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
                  className="rounded-2xl border border-slate-200 bg-white/70 px-5 py-3 text-sm font-bold text-slate-700 dark:border-white/10 dark:bg-white/[0.04] dark:text-white/80"
                >
                  {validateMutation.isPending ? '校验中...' : '校验配置'}
                </button>
                <button
                  type="button"
                  onClick={submitSave}
                  disabled={saveMutation.isPending || validateMutation.isPending}
                  className="btn-liquid inline-flex items-center justify-center gap-2 px-5 py-3 text-white"
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
                  className="rounded-2xl border border-slate-200 bg-white/60 px-5 py-3 text-sm font-bold text-slate-500 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/45"
                >
                  取消
                </button>
              </>
            )}
          </div>
        }
      />

      <section className="min-w-0 space-y-5 rounded-2xl border border-border-subtle bg-surface p-4 shadow-sm sm:rounded-3xl sm:p-5 md:p-6">
        <WorkflowStepper
          title="配置变更流程"
          description="先查看当前值，再编辑、校验并预览影响；只有确认评审后才会保存并发布。每一步都保留失败原因和可回退路径。"
          stages={configWorkflowStages}
          className="border-border-subtle bg-surface-sunken"
        />
        <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
          <div className="grid min-w-0 gap-3 sm:gap-4 sm:grid-cols-2">
            <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-surface-sunken p-4 sm:rounded-[1.8rem] sm:p-5 dark:border-white/10">
              <div className="text-[11px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">Authority</div>
              <div className="mt-3 break-words text-lg font-black text-slate-900 dark:text-white">{displayedSnapshot}</div>
              <div className="mt-4 space-y-2 text-sm text-slate-600 dark:text-white/55">
                <div className="break-words">来源: {view.source || '--'}</div>
                <div>版本: {view.version ?? '--'}</div>
                <div className="break-words">更新时间: {formatDateTime(view.updatedAt)}</div>
                <div>数据库快照: {view.stored.present ? 'PRESENT' : 'NOT_SAVED'}</div>
              </div>
            </div>
            <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-surface-sunken p-4 sm:rounded-[1.8rem] sm:p-5 dark:border-white/10">
              <div className="text-[11px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">Runtime</div>
              <div className="mt-3 break-words text-lg font-black text-slate-900 dark:text-white">{view.runtime.available ? 'ai-gateway 当前运行态' : 'ai-gateway 运行态不可达'}</div>
              <div className="mt-4 space-y-2 text-sm text-slate-600 dark:text-white/55">
                <div className="break-words">来源: {view.runtime.source || '--'}</div>
                <div>版本: {view.runtime.version ?? '--'}</div>
                <div className="break-words">应用时间: {formatDateTime(view.runtime.appliedAt)}</div>
                <div className="break-words">同步状态: {view.runtime.available ? (view.runtime.inSync ? '与运行态一致' : '与运行态不一致') : '等待 runtime 恢复后比对'}</div>
              </div>
            </div>
          </div>
          <div className="grid min-w-0 grid-cols-1 gap-2 sm:grid-cols-2 sm:gap-3">
            <HealthBadge healthy={view.runtime.available} label={`runtime: ${view.runtime.available ? 'AVAILABLE' : 'UNAVAILABLE'}`} />
            <HealthBadge healthy={!view.stored.present || (view.runtime.available && view.runtime.inSync)} label={`stored sync: ${storedSyncLabel}`} />
            <HealthBadge healthy={Boolean(config.provider.activeProvider)} label={`activeProvider: ${config.provider.activeProvider || '--'}`} />
            <HealthBadge healthy={Boolean(config.provider.fallbackProvider) && config.provider.fallbackProvider !== config.provider.activeProvider} label={`fallbackProvider: ${config.provider.fallbackProvider || '--'}`} />
            <HealthBadge healthy={(activeProviderDefinition?.embedding.dimension ?? 0) > 0} label={`active embedding: ${activeProviderDefinition?.embedding.dimension ?? '--'} dim`} />
            <HealthBadge healthy={providerEntries.length > 0} label={`providers: ${providerEntries.length}`} />
          </div>
        </div>

        <div className="grid min-w-0 gap-4 xl:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)]">
          <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/55 p-4 sm:rounded-[1.8rem] sm:p-5 dark:border-white/10 dark:bg-white/[0.03]">
            <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Recommended Flow</div>
            <div className="mt-4 grid min-w-0 grid-cols-1 gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
              {['1. 进入编辑', '2. 校验配置', '3. 保存并生效', '4. 刷新运行态健康 / Outbox / Reindex 验证'].map((item) => (
                <div
                  key={item}
                  className="min-w-0 break-words rounded-2xl border border-slate-200/70 bg-white/70 px-3 py-3 text-slate-600 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/55"
                >
                  {item}
                </div>
              ))}
            </div>
          </div>

          <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/55 p-4 sm:rounded-[1.8rem] sm:p-5 dark:border-white/10 dark:bg-white/[0.03]">
            <div className="flex items-center gap-2 text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">
              <Info size={14} className="shrink-0" />
              当前标签说明
            </div>
            <div className="mt-3 text-sm leading-6 text-slate-600 dark:text-white/55">{activeTabDescription}</div>
          </div>
        </div>

        {runtimeUnavailable && view.stored.present && (
          <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-600 dark:text-amber-400">
            ai-gateway 运行态当前不可达，页面仍以数据库权威快照展示配置。当前无法确认网关是否已应用到同一版本，请先恢复 runtime 再继续核对。
          </div>
        )}

        {runtimeOutOfSync && (
          <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500">
            数据库存储版本 {view.stored.version ?? '--'} 与 ai-gateway 运行态版本 {view.runtime.version ?? '--'} 不一致。请先确认哪一侧应作为最新真相，再决定是否覆盖保存。
          </div>
        )}

        {view.notices.length > 0 && (
          <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-600 dark:text-amber-400">
            {view.notices.map((notice) => (
              <div key={notice.code}>{translateConfigNotice(notice)}</div>
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
              <div key={`${issue.field}-${issue.code}-${issue.defaultMessage}`}>
                <span className="font-bold">{humanizeFieldName(issue.field)}</span>
                <span className="mx-2">·</span>
                <span>{translateConfigIssue(issue)}</span>
              </div>
            ))}
          </div>
        )}

        {!!validationNotices.length && (
          <div className="rounded-[1.6rem] border border-sky-500/20 bg-sky-500/5 p-4 text-sm text-sky-700 dark:text-sky-300 space-y-2">
            {validationNotices.map((notice) => (
              <div key={notice.code}>{translateConfigNotice(notice)}</div>
            ))}
          </div>
        )}

        {editing && (
          <div className="min-w-0 space-y-4 rounded-2xl border border-slate-200/70 bg-white/55 p-3 sm:rounded-[1.8rem] sm:p-5 dark:border-white/10 dark:bg-white/[0.03]">
            <div className="flex min-w-0 flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Presets</div>
                <div className="mt-2 text-sm text-slate-600 dark:text-white/55">
                  预设只覆盖非密钥字段，当前密钥保留策略不会被改写。应用后仍建议先校验，再保存并生效。
                </div>
              </div>
              <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                当前草稿 {configDiffs.length + secretChanges.length} 项待确认改动
              </div>
            </div>
            <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3">
              {configPresets.map((preset) => {
                const disabled = (preset.key === 'dual-provider' || preset.key === 'single-provider') && providerNames.length < 2;
                return (
                  <button
                    key={preset.key}
                    type="button"
                    disabled={disabled}
                    onClick={() => {
                      clearProbeResults();
                      setConfig((current) => (current ? preset.apply(current, providerNames) : current));
                      setValidation(null);
                      setFeedback({
                        tone: 'success',
                        message: `已套用“${preset.label}”预设。请校验差异后再保存。`,
                      });
                    }}
                    className="rounded-[1.6rem] border border-slate-200/70 bg-white/70 px-4 py-4 text-left text-sm text-slate-600 transition-all disabled:cursor-not-allowed disabled:opacity-50 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/55"
                  >
                    <div className="font-bold text-slate-900 dark:text-white">{preset.label}</div>
                    <div className="mt-2">{preset.description}</div>
                    {disabled && <div className="mt-3 text-xs text-amber-600 dark:text-amber-400">至少需要两套 provider 定义才能启用该预设。</div>}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {editing && !!draftRiskHints.length && (
          <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-700 dark:text-amber-400 space-y-2">
            <div className="font-bold">当前草稿包含高风险改动</div>
            {draftRiskHints.map((hint) => (
              <div key={hint}>{hint}</div>
            ))}
          </div>
        )}

        <div className="page-tabs flex flex-nowrap gap-2 border-0 pb-1" role="tablist" aria-label="配置中心标签">
          {tabs.map((tab) => (
            <TabButton key={tab.key} active={activeTab === tab.key} label={tab.label} onClick={() => setActiveTab(tab.key)} />
          ))}
        </div>
      </section>

      {activeTab === 'provider' && (
        <SectionCard title="模型接入配置" description="此处草稿始终基于数据库权威快照。每个 provider 独立维护 chat、embedding、rerank 的 baseUrl、model、connect/read timeout 与密钥，provider key 本身不会锁定厂商地址。">
          <FieldGrid>
            <FieldCard label="当前 Provider" hint="只能从已定义 provider key 中选择。请求会先打到这个 provider，发生可重试故障时再尝试 fallback。">
              <SelectInput
                value={config.provider.activeProvider}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, activeProvider: value } }))}
                disabled={!editing || providerNames.length === 0}
                options={activeProviderOptions}
              />
            </FieldCard>
            <FieldCard label="备用 Provider" hint="只能从已定义 provider key 中选择。仅在 active provider 遇到 retryable 失败、429/5xx 或 circuit open 时尝试一次切换。">
              <SelectInput
                value={config.provider.fallbackProvider}
                onChange={(value) => updateConfig((current) => ({ ...current, provider: { ...current.provider, fallbackProvider: value } }))}
                disabled={!editing || providerNames.length === 0}
                options={fallbackProviderOptions}
              />
            </FieldCard>
          </FieldGrid>

          {editing && (
            <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/60 p-4 dark:border-white/10 dark:bg-white/[0.03] space-y-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Provider 管理</div>
                  <div className="mt-2 text-sm text-slate-600 dark:text-white/55">
                    分组名即 provider key，会进入保存配置、运行态标签和错误字段。建议使用技术 key，例如 `openai_main`、`backup-1`。
                  </div>
                </div>
                <div className="rounded-full border border-slate-200/70 px-3 py-1 text-xs text-slate-500 dark:border-white/10 dark:text-white/45">
                  现有 {providerNames.length} 组 provider
                </div>
              </div>
              <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-[minmax(0,1fr)_auto]">
                <TextInput
                  value={newProviderName}
                  onChange={setNewProviderName}
                  disabled={!editing}
                  placeholder="new_provider_key"
                />
                <button
                  type="button"
                  onClick={addProvider}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200/70 bg-white/80 px-4 py-3 text-sm font-bold text-slate-700 dark:border-white/10 dark:bg-slate-950/30 dark:text-white/75"
                >
                  <Plus size={16} />
                  新增 Provider
                </button>
              </div>
            </div>
          )}

          {(config.provider.activeProvider && !providerNames.includes(config.provider.activeProvider)) || (config.provider.fallbackProvider && !providerNames.includes(config.provider.fallbackProvider)) ? (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500">
              active / fallback provider 只能引用已有 provider key。当前配置里至少有一个引用已经失效，请改回现有 provider 定义后再保存。
            </div>
          ) : null}

          {providerEntries.length > 0 && providerEntries.length < 2 && (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500">
              当前 provider 数量少于 2。activeProvider 与 fallbackProvider 必须引用两个不同 provider，请至少补齐两组 provider 定义后再保存。
            </div>
          )}

          {providerEntries.length === 0 && (
            <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500">
              当前配置没有任何 provider 定义，保存前至少需要补齐一套 provider.providers 配置。
            </div>
          )}

          {providerEntries.map(([providerName, rawDefinition]) => {
            const definition = normalizeProviderDefinition(rawDefinition);
            const providerSecrets = secrets.providers[providerName] || createEmptyProviderSecretEditors();
            const providerTone = providerName === config.provider.activeProvider
              ? 'border-primary/20 bg-primary/5 text-primary'
              : providerName === config.provider.fallbackProvider
                ? 'border-amber-500/20 bg-amber-500/5 text-amber-600 dark:text-amber-400'
                : 'border-slate-200/70 bg-white/70 text-slate-500 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/45';
            const providerRole = providerName === config.provider.activeProvider
              ? '主用 Provider'
              : providerName === config.provider.fallbackProvider
                ? '故障切换备用 Provider'
                : '普通候选 Provider';

            return (
              <details
                key={providerName}
                open
                className="group min-w-0 rounded-2xl border border-slate-200/70 bg-surface p-3 sm:rounded-[1.9rem] sm:p-5 dark:border-white/10"
              >
                <summary className="list-none cursor-pointer">
                  <div className="flex min-w-0 flex-col gap-3 md:flex-row md:items-start md:justify-between">
                    <div className="min-w-0 space-y-2">
                      <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Provider Definition</div>
                      <div className="flex min-w-0 flex-wrap items-center gap-2">
                        <div className="min-w-0 break-all text-lg font-black text-slate-900 dark:text-white">{providerName}</div>
                        <div className={`max-w-full break-all rounded-2xl border px-3 py-2 text-xs font-bold ${providerTone}`}>{providerName}</div>
                        {providerName === config.provider.activeProvider && (
                          <div className="rounded-2xl border border-primary/20 bg-primary/5 px-3 py-2 text-xs font-bold text-primary">ACTIVE</div>
                        )}
                        {providerName === config.provider.fallbackProvider && (
                          <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 px-3 py-2 text-xs font-bold text-amber-600 dark:text-amber-400">FALLBACK</div>
                        )}
                      </div>
                      <div className="text-sm text-slate-500 dark:text-white/45">{providerRole}</div>
                    </div>
                    <div className="flex shrink-0 items-center gap-3 text-sm text-slate-500 dark:text-white/45">
                      <span className="hidden sm:inline">展开查看 Chat / Embedding / Rerank 详细配置</span>
                      <span className="sm:hidden">展开详情</span>
                      <ChevronDown size={18} className="shrink-0 transition-transform group-open:rotate-180" />
                    </div>
                  </div>
                </summary>

                <div className="mt-5 space-y-5">
                  <div className="flex flex-wrap gap-2">
                    {editing && renamingProviderName !== providerName && (
                      <>
                        <button
                          type="button"
                          onClick={() => startRenameProvider(providerName)}
                          className="rounded-2xl border border-slate-200/70 bg-white/80 px-3 py-2 text-xs font-bold text-slate-600 dark:border-white/10 dark:bg-slate-950/30 dark:text-white/60 inline-flex items-center gap-2"
                        >
                          <Pencil size={14} />
                          重命名
                        </button>
                        <button
                          type="button"
                          onClick={() => deleteProvider(providerName)}
                          className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-3 py-2 text-xs font-bold text-rose-500 inline-flex items-center gap-2"
                        >
                          <Trash2 size={14} />
                          删除
                        </button>
                      </>
                    )}
                  </div>

                  {editing && renamingProviderName === providerName && (
                    <div className="flex flex-col gap-3 md:flex-row">
                      <TextInput
                        value={renameProviderDraft}
                        onChange={setRenameProviderDraft}
                        disabled={!editing}
                        placeholder="provider_key"
                      />
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => renameProvider(providerName)}
                          className="rounded-2xl border border-slate-200/70 bg-white/80 px-4 py-3 text-sm font-bold text-slate-700 dark:border-white/10 dark:bg-slate-950/30 dark:text-white/75"
                        >
                          确认重命名
                        </button>
                        <button
                          type="button"
                          onClick={cancelRenameProvider}
                          className="rounded-2xl border border-slate-200/70 bg-white/80 px-4 py-3 text-sm font-bold text-slate-500 dark:border-white/10 dark:bg-slate-950/30 dark:text-white/45 inline-flex items-center gap-2"
                        >
                          <X size={16} />
                          取消
                        </button>
                      </div>
                    </div>
                  )}

                  <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3">
                  <div className="min-w-0 rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-3 py-3 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-slate-950/25">
                    <div className="text-[11px] uppercase tracking-[0.22em] text-slate-400 dark:text-white/30">Chat 摘要</div>
                    <div className="mt-2 break-all text-sm font-bold text-slate-900 dark:text-white">{summarizeProviderValue(definition.chat.model)}</div>
                    <div className="mt-2 break-all text-xs leading-5 text-slate-500 dark:text-white/45">{summarizeProviderValue(definition.chat.baseUrl)}</div>
                  </div>
                  <div className="min-w-0 rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-3 py-3 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-slate-950/25">
                    <div className="text-[11px] uppercase tracking-[0.22em] text-slate-400 dark:text-white/30">Embedding 摘要</div>
                    <div className="mt-2 break-all text-sm font-bold text-slate-900 dark:text-white">{summarizeProviderValue(definition.embedding.model)}</div>
                    <div className="mt-2 break-all text-xs leading-5 text-slate-500 dark:text-white/45">
                      {summarizeProviderValue(definition.embedding.baseUrl)} · 维度 {summarizeProviderValue(definition.embedding.dimension)}
                    </div>
                  </div>
                  <div className="min-w-0 rounded-[1.4rem] border border-slate-200/70 bg-white/70 px-3 py-3 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-slate-950/25">
                    <div className="text-[11px] uppercase tracking-[0.22em] text-slate-400 dark:text-white/30">Rerank 摘要</div>
                    <div className="mt-2 break-all text-sm font-bold text-slate-900 dark:text-white">{summarizeProviderValue(definition.rerank.model)}</div>
                    <div className="mt-2 break-all text-xs leading-5 text-slate-500 dark:text-white/45">
                      {summarizeProviderValue(definition.rerank.protocol)} · {summarizeProviderValue(definition.rerank.baseUrl)}
                    </div>
                  </div>
                  </div>

                  <FieldGrid>
                  <FieldCard label="Chat 协议" hint="支持 Chat Completions 兼容协议和 Responses 协议；两者都使用当前 provider 自己的 URL 与 Key。">
                    <SelectInput
                      value={definition.chat.protocol}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({
                        ...current,
                        chat: { ...current.chat, protocol: value as AiOpsProtocol },
                      }))}
                      disabled={!editing}
                      options={providerProtocolOptions.chat}
                    />
                  </FieldCard>
                  <FieldCard label="Chat 接口地址" hint="填写当前服务商的 API base URL，例如 https://xxxx.com/v1。Responses 会在该地址后追加 /responses，不绑定 OpenAI 官方域名。">
                    <TextInput
                      value={definition.chat.baseUrl}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, baseUrl: value } }))}
                      disabled={!editing}
                      placeholder="https://provider.example.com/v1"
                    />
                  </FieldCard>
                  <FieldCard label={providerSecretMeta.chatApiKey.label} hint={providerSecretMeta.chatApiKey.hint}>
                    <div className="space-y-3">
                      <div className="text-xs text-slate-500 dark:text-white/35">
                        当前状态: {formatSecretStatus(getProviderSecretField(view, providerName, 'chatApiKey'))}
                      </div>
                      <div className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={providerSecrets.chatApiKey.retainExisting}
                          disabled={!editing}
                          onChange={(event) => updateProviderSecret(providerName, 'chatApiKey', {
                            retainExisting: event.target.checked,
                            value: event.target.checked ? '' : providerSecrets.chatApiKey.value || '',
                          })}
                        />
                        <span>保留原值</span>
                      </div>
                      <TextInput
                        type="password"
                        value={providerSecrets.chatApiKey.value || ''}
                        onChange={(value) => updateProviderSecret(providerName, 'chatApiKey', { value, retainExisting: false })}
                        disabled={!editing || Boolean(providerSecrets.chatApiKey.retainExisting)}
                        placeholder="仅在需要覆盖时填写新值"
                      />
                    </div>
                  </FieldCard>
                  <FieldCard label="Chat 模型名" hint="例如通义千问的具体模型标识。模型名错误会直接导致运行时调用失败。">
                    <TextInput
                      value={definition.chat.model}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, model: value } }))}
                      disabled={!editing}
                      placeholder="chat-model-id"
                    />
                  </FieldCard>
                  <FieldCard label="Chat 连接超时" hint="建立到 Chat 上游连接的超时时间。建议明显短于读取超时，避免把网络建连阻塞误判成模型慢响应。">
                    <TextInput
                      value={definition.chat.connectTimeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, connectTimeout: value } }))}
                      disabled={!editing}
                      placeholder="PT3S"
                    />
                  </FieldCard>
                  <FieldCard label="Chat 读取超时" hint="等待 Chat 模型完整返回的超时时间。支持 30s、500ms 或 PT30S。">
                    <TextInput
                      value={definition.chat.readTimeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, chat: { ...current.chat, readTimeout: value } }))}
                      disabled={!editing}
                      placeholder="PT30S"
                    />
                  </FieldCard>
                  <FieldCard label="生成温度" hint="值越高越发散。对诊断/教学类输出通常建议保持低温。">
                    <TextInput
                      type="number"
                      step="0.1"
                      value={definition.chat.temperature}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({
                        ...current,
                        chat: { ...current.chat, temperature: parseNullableFloat(value) },
                      }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  <FieldCard label="最大输出 Tokens" hint="限制单次回答长度。值过低会造成回答截断，值过高会增加耗时和成本。">
                    <TextInput
                      type="number"
                      value={definition.chat.maxTokens}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({
                        ...current,
                        chat: { ...current.chat, maxTokens: parseNullableInteger(value) },
                      }))}
                      disabled={!editing}
                    />
                  </FieldCard>
                  </FieldGrid>

                  <FieldGrid>
                  <FieldCard label="Embedding 协议" hint="当前后端仅支持 OpenAI 兼容协议的 Embedding 接口。">
                    <SelectInput
                      value={definition.embedding.protocol}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({
                        ...current,
                        embedding: { ...current.embedding, protocol: value as AiOpsProtocol },
                      }))}
                      disabled={!editing}
                      options={providerProtocolOptions.embedding}
                    />
                  </FieldCard>
                  <FieldCard label="Embedding 接口地址" hint="用于向量化。这里变更后会影响 RAG 导入和检索的一致性，地址与 provider key 同样完全可自定义。">
                    <TextInput
                      value={definition.embedding.baseUrl}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, embedding: { ...current.embedding, baseUrl: value } }))}
                      disabled={!editing}
                      placeholder="https://provider.example.com/v1"
                    />
                  </FieldCard>
                  <FieldCard label={providerSecretMeta.embeddingApiKey.label} hint={providerSecretMeta.embeddingApiKey.hint}>
                    <div className="space-y-3">
                      <div className="text-xs text-slate-500 dark:text-white/35">
                        当前状态: {formatSecretStatus(getProviderSecretField(view, providerName, 'embeddingApiKey'))}
                      </div>
                      <div className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={providerSecrets.embeddingApiKey.retainExisting}
                          disabled={!editing}
                          onChange={(event) => updateProviderSecret(providerName, 'embeddingApiKey', {
                            retainExisting: event.target.checked,
                            value: event.target.checked ? '' : providerSecrets.embeddingApiKey.value || '',
                          })}
                        />
                        <span>保留原值</span>
                      </div>
                      <TextInput
                        type="password"
                        value={providerSecrets.embeddingApiKey.value || ''}
                        onChange={(value) => updateProviderSecret(providerName, 'embeddingApiKey', { value, retainExisting: false })}
                        disabled={!editing || Boolean(providerSecrets.embeddingApiKey.retainExisting)}
                        placeholder="仅在需要覆盖时填写新值"
                      />
                    </div>
                  </FieldCard>
                  <FieldCard label="Embedding 模型名" hint="产品向量空间固定为 Qwen/Qwen3-Embedding-8B；如需迁移模型，应通过数据库重建与全量 reindex 发布。">
                    <TextInput
                      value={definition.embedding.model}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, embedding: { ...current.embedding, model: value } }))}
                      disabled
                      placeholder="embedding-model-id"
                    />
                  </FieldCard>
                  <FieldCard label="Embedding 连接超时" hint="建立到 Embedding 服务连接的超时时间。通常应比读取超时更短。">
                    <TextInput
                      value={definition.embedding.connectTimeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, embedding: { ...current.embedding, connectTimeout: value } }))}
                      disabled={!editing}
                      placeholder="PT3S"
                    />
                  </FieldCard>
                  <FieldCard label="Embedding 读取超时" hint="等待向量化返回的超时时间。批量导入时建议适度放宽。">
                    <TextInput
                      value={definition.embedding.readTimeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, embedding: { ...current.embedding, readTimeout: value } }))}
                      disabled={!editing}
                      placeholder="PT30S"
                    />
                  </FieldCard>
                  <FieldCard label="向量维度" hint="产品向量空间固定为 1024 维，管理端不允许在线修改。">
                    <TextInput
                      type="number"
                      value={definition.embedding.dimension}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({
                        ...current,
                        embedding: { ...current.embedding, dimension: parseNullableInteger(value) },
                      }))}
                      disabled
                    />
                  </FieldCard>
                  </FieldGrid>

                  <FieldGrid>
                  <FieldCard label="Rerank 协议" hint="当前后端支持 openai-rerank 与 openai-chat-rerank。新增其他协议前需要先扩展 ai-gateway runtime factory。">
                    <SelectInput
                      value={definition.rerank.protocol}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({
                        ...current,
                        rerank: { ...current.rerank, protocol: value as AiOpsProtocol },
                      }))}
                      disabled={!editing}
                      options={providerProtocolOptions.rerank}
                    />
                  </FieldCard>
                  <FieldCard label="Rerank 接口地址" hint="用于召回后的重排序。若关闭或异常，会明显影响最终检索质量；这里必须填写完整 rerank endpoint URL，而不是 provider 根路径。">
                    <TextInput
                      value={definition.rerank.baseUrl}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, rerank: { ...current.rerank, baseUrl: value } }))}
                      disabled={!editing}
                      placeholder="https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank"
                    />
                  </FieldCard>
                  <FieldCard label={providerSecretMeta.rerankApiKey.label} hint={providerSecretMeta.rerankApiKey.hint}>
                    <div className="space-y-3">
                      <div className="text-xs text-slate-500 dark:text-white/35">
                        当前状态: {formatSecretStatus(getProviderSecretField(view, providerName, 'rerankApiKey'))}
                      </div>
                      <div className="flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={providerSecrets.rerankApiKey.retainExisting}
                          disabled={!editing}
                          onChange={(event) => updateProviderSecret(providerName, 'rerankApiKey', {
                            retainExisting: event.target.checked,
                            value: event.target.checked ? '' : providerSecrets.rerankApiKey.value || '',
                          })}
                        />
                        <span>保留原值</span>
                      </div>
                      <TextInput
                        type="password"
                        value={providerSecrets.rerankApiKey.value || ''}
                        onChange={(value) => updateProviderSecret(providerName, 'rerankApiKey', { value, retainExisting: false })}
                        disabled={!editing || Boolean(providerSecrets.rerankApiKey.retainExisting)}
                        placeholder="仅在需要覆盖时填写新值"
                      />
                    </div>
                  </FieldCard>
                  <FieldCard label="Rerank 模型名" hint="建议与当前服务可用模型保持一致，否则健康检查会提示降级。">
                    <TextInput
                      value={definition.rerank.model}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, rerank: { ...current.rerank, model: value } }))}
                      disabled={!editing}
                      placeholder="rerank-model-id"
                    />
                  </FieldCard>
                  <FieldCard label="Rerank 连接超时" hint="建立到 Rerank 上游连接的超时时间。通常应比读取超时更短。">
                    <TextInput
                      value={definition.rerank.connectTimeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, rerank: { ...current.rerank, connectTimeout: value } }))}
                      disabled={!editing}
                      placeholder="PT3S"
                    />
                  </FieldCard>
                  <FieldCard label="Rerank 读取超时" hint="等待重排序结果返回的超时时间。阈值过低会影响召回后精排稳定性。">
                    <TextInput
                      value={definition.rerank.readTimeout}
                      onChange={(value) => updateProviderDefinition(providerName, (current) => ({ ...current, rerank: { ...current.rerank, readTimeout: value } }))}
                      disabled={!editing}
                      placeholder="PT30S"
                    />
                  </FieldCard>
                  </FieldGrid>
                </div>
              </details>
            );
          })}
        </SectionCard>
      )}

      {activeTab === 'resilience' && (
        <SectionCard title="稳定性配置" description="这些参数会在保存后直接刷新 ai-gateway 内部 retry / circuit breaker 注册表，并参与 active 到 fallback 的自动切换判断。">
          <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/60 px-3 py-3 text-sm text-slate-600 sm:rounded-[1.6rem] sm:px-4 sm:py-4 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
              <div className="font-bold text-slate-900 dark:text-white">Circuit Breaker 是什么</div>
              <div className="mt-2">当某个模型服务持续失败时，熔断器会暂时停止继续打流量，并把请求切到 fallback provider。</div>
            </div>
            <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/60 px-3 py-3 text-sm text-slate-600 sm:rounded-[1.6rem] sm:px-4 sm:py-4 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
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
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  resilience: { ...current.resilience, maxAttempts: parseNullableInteger(value) },
                }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="重试等待时长" hint="两次重试之间的等待时间，支持 2s、500ms 或 PT0.5S 这类格式。">
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
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  resilience: { ...current.resilience, failureRateThreshold: parseNullableFloat(value) },
                }))}
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
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  resilience: { ...current.resilience, slidingWindowSize: parseNullableInteger(value) },
                }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="熔断打开时长" hint="熔断后保持 OPEN 状态的时间，支持 30s、500ms 或 PT30S 这类格式。">
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
        <SectionCard title="RAG 运行参数" description="词条变更会走知识同步链路；这里仍保留 ai-gateway 回源、检索参数和手动 reindex 配置，草稿同样以数据库权威快照为准。">
          <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/60 px-3 py-3 text-sm text-slate-600 sm:rounded-[1.6rem] sm:px-4 sm:py-4 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
              <div className="font-bold text-slate-900 dark:text-white">Top K 是什么</div>
              <div className="mt-2">可以理解成“先保留前 K 个候选”。K 越大，召回越全，但后续成本也越高。</div>
            </div>
            <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/60 px-3 py-3 text-sm text-slate-600 sm:rounded-[1.6rem] sm:px-4 sm:py-4 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/55">
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
                placeholder="https://app-server.example.com"
              />
            </FieldCard>
            <FieldCard label={appServerSecretMeta.label} hint={appServerSecretMeta.hint}>
              <div className="space-y-3">
                <div className="text-xs text-slate-500 dark:text-white/35">
                  当前状态: {formatSecretStatus(view.secrets.appServerInternalToken)}
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
            <FieldCard label="连接超时" hint="ai-gateway 连接 app-server 的超时时间，支持 3s、500ms 或 PT3S 这类格式。">
              <TextInput
                value={config.rag.appServer.connectTimeout}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, appServer: { ...current.rag.appServer, connectTimeout: value } } }))}
                disabled={!editing}
                placeholder="PT3S"
              />
            </FieldCard>
            <FieldCard label="读取超时" hint="等待 app-server 返回分页数据的超时时间，支持 5s、500ms 或 PT5S 这类格式；导出大批量语料时会影响成败。">
              <TextInput
                value={config.rag.appServer.readTimeout}
                onChange={(value) => updateConfig((current) => ({ ...current, rag: { ...current.rag, appServer: { ...current.rag.appServer, readTimeout: value } } }))}
                disabled={!editing}
                placeholder="PT5S"
              />
            </FieldCard>
          </FieldGrid>

          <FieldGrid>
            <FieldCard label="导出分页大小" hint="单次从 app-server 拉取多少条词条。值越大吞吐越高，但单次失败成本也越高。">
              <TextInput
                type="number"
                value={config.rag.ingestion.exportPageSize}
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  rag: { ...current.rag, ingestion: { ...current.rag.ingestion, exportPageSize: parseNullableInteger(value) } },
                }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard label="向量批大小" hint="一次提交给 embedding 服务的批量大小。过大容易超时，过小会拖慢重建。">
              <TextInput
                type="number"
                value={config.rag.ingestion.embeddingBatchSize}
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  rag: { ...current.rag, ingestion: { ...current.rag.ingestion, embeddingBatchSize: parseNullableInteger(value) } },
                }))}
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
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  rag: { ...current.rag, retrieval: { ...current.rag.retrieval, recallTopK: parseNullableInteger(value) } },
                }))}
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
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  rag: { ...current.rag, retrieval: { ...current.rag.retrieval, recallThreshold: parseNullableFloat(value) } },
                }))}
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
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  rag: { ...current.rag, retrieval: { ...current.rag.retrieval, rerankTopN: parseNullableInteger(value) } },
                }))}
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
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  rag: { ...current.rag, retrieval: { ...current.rag.retrieval, rerankThreshold: parseNullableFloat(value) } },
                }))}
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
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  rag: { ...current.rag, retrieval: { ...current.rag.retrieval, finalTopK: parseNullableInteger(value) } },
                }))}
                disabled={!editing}
              />
            </FieldCard>
            <FieldCard
              label="HNSW ef_search"
              hint="单次 ANN 查询访问的候选规模。值越高通常召回越稳，但延迟也会升高。"
              detail="它直接影响 pgvector HNSW 查询阶段的搜索宽度。适合在召回质量和延迟之间做运行时折中。"
            >
              <TextInput
                type="number"
                value={config.rag.retrieval.hnswEfSearch}
                onChange={(value) => updateConfig((current) => ({
                  ...current,
                  rag: { ...current.rag, retrieval: { ...current.rag.retrieval, hnswEfSearch: parseNullableInteger(value) } },
                }))}
                disabled={!editing}
              />
            </FieldCard>
          </FieldGrid>
        </SectionCard>
      )}

      {activeTab === 'operations' && (
        <div className="page-stack">
          <div className="grid min-w-0 grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)] xl:gap-6">
            <SectionCard title="健康检查" description="刷新运行态健康只做 readiness/probe，不触发计费型模型调用。下面两个测试连接会对当前草稿发起真实请求，各消耗 1 次额度，且只测试 active provider。">
              <div className="page-actions">
                <button
                  type="button"
                  onClick={() => healthMutation.mutate()}
                  disabled={healthMutation.isPending}
                  className="btn-liquid px-5 py-3 text-white inline-flex items-center gap-2"
                >
                  <ShieldCheck size={16} />
                  {healthMutation.isPending ? '刷新中...' : '刷新运行态健康'}
                </button>
                <button
                  type="button"
                  onClick={() => void queryClient.invalidateQueries({ queryKey: ['admin-ai-config'] })}
                  className="rounded-2xl border border-slate-200 dark:border-white/10 px-5 py-3 text-sm font-bold text-slate-600 dark:text-white/70 bg-white/70 dark:bg-white/[0.04] inline-flex items-center gap-2"
                >
                  <RefreshCw size={16} />
                  刷新配置
                </button>
                <button
                  type="button"
                  onClick={() => void driftQuery.refetch()}
                  disabled={driftQuery.isFetching}
                  className="rounded-2xl border border-slate-200 dark:border-white/10 px-5 py-3 text-sm font-bold text-slate-600 dark:text-white/70 bg-white/70 dark:bg-white/[0.04] inline-flex items-center gap-2"
                >
                  <AlertTriangle size={16} />
                  {driftQuery.isFetching ? '诊断中...' : '刷新 Drift 诊断'}
                </button>
                <button
                  type="button"
                  onClick={() => syncRuntimeMutation.mutate({ expectedVersion: currentConfigVersion(view) })}
                  disabled={!view.stored.present || syncRuntimeMutation.isPending || saveMutation.isPending}
                  className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 px-5 py-3 text-sm font-bold text-emerald-700 dark:text-emerald-300 inline-flex items-center gap-2 disabled:opacity-60"
                >
                  <RefreshCw size={16} />
                  {syncRuntimeMutation.isPending ? '同步中...' : '同步数据库快照到运行态'}
                </button>
                <button
                  type="button"
                  onClick={triggerEmbeddingProbe}
                  disabled={embeddingProbeMutation.isPending || rerankProbeMutation.isPending || saveMutation.isPending}
                  className="rounded-2xl border border-sky-500/20 bg-sky-500/10 px-5 py-3 text-sm font-bold text-sky-700 dark:text-sky-300 inline-flex items-center gap-2 disabled:opacity-60"
                >
                  <Play size={16} />
                  {embeddingProbeMutation.isPending ? '测试中...' : '测试 Embedding'}
                </button>
                <button
                  type="button"
                  onClick={triggerRerankProbe}
                  disabled={embeddingProbeMutation.isPending || rerankProbeMutation.isPending || saveMutation.isPending}
                  className="rounded-2xl border border-amber-500/20 bg-amber-500/10 px-5 py-3 text-sm font-bold text-amber-700 dark:text-amber-300 inline-flex items-center gap-2 disabled:opacity-60"
                >
                  <Play size={16} />
                  {rerankProbeMutation.isPending ? '测试中...' : '测试 Rerank'}
                </button>
              </div>

              <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/[0.06] p-4 text-sm text-amber-700 dark:text-amber-300">
                测试连接会基于当前草稿直接调用 active provider，不经过 failover；如果你刚改了 baseUrl、model 或密钥，旧测试结果会在下一次编辑时自动清空。
              </div>

              {!view.stored.present && (
                <div className="rounded-[1.6rem] border border-sky-500/20 bg-sky-500/5 p-4 text-sm text-sky-700 dark:text-sky-300">
                  当前还没有数据库权威快照。请先完成一次保存，再执行运行态同步。
                </div>
              )}

              {healthMutation.error && (
                <div className="rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500">
                  {healthMutation.error.message}
                </div>
              )}

              <div className="grid min-w-0 grid-cols-1 gap-3 sm:gap-4 xl:grid-cols-2">
                <ProbeResultCard
                  title="Embedding 测试结果"
                  result={embeddingProbeResult}
                  emptyHint="尚未执行 Embedding 测试。"
                  rows={embeddingProbeRows}
                />
                <ProbeResultCard
                  title="Rerank 测试结果"
                  result={rerankProbeResult}
                  emptyHint="尚未执行 Rerank 测试。"
                  rows={rerankProbeRows}
                />
              </div>

              {healthState && (
                <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4">
                  <HealthBadge healthy={healthState.status === 'UP'} label={`整体状态: ${healthState.status}`} />
                  <HealthBadge healthy={isStoredSyncHealthy(healthState.storedSyncStatus)} label={`Stored Sync: ${healthState.storedSyncStatus || '--'}`} />
                  <HealthBadge healthy={healthState.databaseReady} label={`Database: ${healthState.databaseReady ? 'READY' : 'DOWN'}`} />
                  <HealthBadge healthy={healthState.vectorStoreReady} label={`Vector Store: ${healthState.vectorStoreReady ? 'READY' : 'DOWN'}`} />
                  <HealthBadge healthy={healthState.providerReady} label={`Provider: ${healthState.providerReady ? 'READY' : 'DEGRADED'}`} />
                  <HealthBadge healthy={healthState.rerankReady} label={`Rerank: ${healthState.rerankReady ? 'READY' : 'DEGRADED'}`} />
                  <HealthBadge healthy={healthState.appServerReady} label={`App Server: ${healthState.appServerReady ? 'READY' : 'DOWN'}`} />
                  <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/55 px-3 py-3 text-sm text-slate-600 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                    <div className="mb-2 text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Runtime Models</div>
                    <div className="break-all">Chat: {healthState.chatModel}</div>
                    <div className="break-all">Embedding: {healthState.embeddingModel}</div>
                    <div className="break-all">Rerank: {healthState.rerankModel}</div>
                  </div>
                  <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/55 px-3 py-3 text-sm text-slate-600 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                    <div className="mb-2 text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Environment</div>
                    <div className="break-all">Provider: {healthState.provider}</div>
                    <div className="break-all">Fallback: {healthState.fallbackProvider}</div>
                    <div className="break-words">Stored Sync: {describeStoredSyncStatus(healthState.storedSyncStatus)}</div>
                    <div className="break-all">Vector Extension: {healthState.vectorExtensionVersion || '--'}</div>
                    <div className="break-words">Profiles: {(Array.isArray(healthState.activeProfiles) ? healthState.activeProfiles : []).join(', ') || '--'}</div>
                    <div className="break-words">Checked At: {formatDateTime(healthState.timestamp)}</div>
                  </div>
                  {healthState.appServerError && (
                    <div className="min-w-0 break-all rounded-[1.6rem] border border-rose-500/20 bg-rose-500/5 p-4 text-sm text-rose-500 sm:col-span-2">
                      app-server 探测失败: {translateConfigMessage(healthState.appServerError)}
                    </div>
                  )}
                </div>
              )}

              {driftQuery.data && (
                <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4">
                  <HealthBadge healthy={!driftQuery.data.driftDetected} label={`Runtime Drift: ${driftQuery.data.driftDetected ? 'DETECTED' : 'CLEAR'}`} />
                  <HealthBadge healthy={driftQuery.data.syncJobStatus !== 'DLQ'} label={`Sync Job: ${describeSyncJobStatus(driftQuery.data.syncJobStatus)}`} />
                  <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/55 px-3 py-3 text-sm text-slate-600 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                    <div className="mb-2 text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Drift State</div>
                    <div>Stored Version: {driftQuery.data.stored.version ?? '--'}</div>
                    <div>Runtime Version: {driftQuery.data.runtime.version ?? '--'}</div>
                    <div>Attempts: {driftQuery.data.attemptCount ?? 0}</div>
                    <div className="break-words">Next Attempt: {formatDateTime(driftQuery.data.nextAttemptAt)}</div>
                  </div>
                  <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/55 px-3 py-3 text-sm text-slate-600 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                    <div className="mb-2 text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Drift Notices</div>
                    {driftQuery.data.notices.length ? (
                      driftQuery.data.notices.map((notice) => <div key={notice.code} className="break-words">{translateConfigNotice(notice)}</div>)
                    ) : (
                      <div>暂无 drift notice。</div>
                    )}
                  </div>
                </div>
              )}
            </SectionCard>

            <SectionCard title="RAG Reindex" description="正常情况下词条变更会发布知识同步事件；如果本地联调、RabbitMQ 或回源链路异常导致新词条没有进入检索，可在这里手动 reindex。默认建议覆盖词汇知识三类 source type。">
              <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3">
                <button
                  type="button"
                  onClick={() =>
                    setReindexForm({
                      mode: 'INCREMENTAL',
                      sourceTypes: defaultLexicalSourceTypes,
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
                      sourceTypes: defaultLexicalSourceTypes,
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
                      sourceTypes: current.sourceTypes?.length ? current.sourceTypes : defaultLexicalSourceTypes,
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
                <FieldCard
                  label="数据源类型"
                  hint="默认建议勾选词汇知识三类。Seed 类 source type 仅在需要补建内置知识时再启用；全部取消会让后端回退为“全部类型”。"
                >
                  <div className="space-y-4">
                    <div>
                      <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30 mb-2">App Server 知识源</div>
                      <div className="grid gap-2">
                        {appServerReindexSourceTypes.map((sourceType) => (
                          <label key={sourceType} className="flex items-center gap-2 text-sm text-slate-600 dark:text-white/60">
                            <input
                              type="checkbox"
                              checked={(reindexForm.sourceTypes || []).includes(sourceType)}
                              onChange={(event) => toggleReindexSourceType(sourceType, event.target.checked)}
                            />
                            <span>{reindexSourceTypeLabels[sourceType] || sourceType}</span>
                          </label>
                        ))}
                      </div>
                    </div>
                    <div>
                      <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30 mb-2">高级 Seed Source Type</div>
                      <div className="grid gap-2">
                        {seedReindexSourceTypes.map((sourceType) => (
                          <label key={sourceType} className="flex items-center gap-2 text-sm text-slate-600 dark:text-white/60">
                            <input
                              type="checkbox"
                              checked={(reindexForm.sourceTypes || []).includes(sourceType)}
                              onChange={(event) => toggleReindexSourceType(sourceType, event.target.checked)}
                            />
                            <span>{reindexSourceTypeLabels[sourceType] || sourceType}</span>
                          </label>
                        ))}
                      </div>
                    </div>
                  </div>
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

              {!!reindexRiskHints.length && (
                <div className="rounded-[1.6rem] border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-700 dark:text-amber-400 space-y-2">
                  <div className="font-bold">当前 reindex 配置存在较高影响面</div>
                  {reindexRiskHints.map((hint) => (
                    <div key={hint}>{hint}</div>
                  ))}
                </div>
              )}

              <button
                type="button"
                onClick={() => {
                  if (reindexRiskHints.length > 0) {
                    setReindexConfirmOpen(true);
                    return;
                  }
                  reindexMutation.mutate(reindexForm);
                }}
                disabled={reindexMutation.isPending}
                className="btn-liquid px-5 py-3 text-white inline-flex items-center gap-2"
              >
                <Play size={16} />
                {reindexMutation.isPending ? '提交中...' : '触发 RAG Reindex'}
              </button>

              {reindexJobQuery.data && (
                <div className="min-w-0 space-y-5 rounded-2xl border border-slate-200/70 bg-white/55 p-3 text-sm text-slate-600 sm:rounded-[1.6rem] sm:p-5 dark:border-white/10 dark:bg-white/[0.03] dark:text-white/60">
                  <div className="flex min-w-0 flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                    <div className="min-w-0 break-all font-black text-slate-900 dark:text-white">任务 #{reindexJobQuery.data.jobId}</div>
                    <div className="shrink-0 text-xs uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{reindexJobQuery.data.status}</div>
                  </div>

                  <div className="rounded-[1.4rem] border border-slate-200/70 bg-white/75 p-3 sm:p-4 dark:border-white/10 dark:bg-slate-950/25">
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0 font-bold text-slate-900 dark:text-white">{reindexStatusMeta.label}</div>
                      <div className="shrink-0 text-xs text-slate-400 dark:text-white/30">{reindexStatusMeta.progress}%</div>
                    </div>
                    <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200/70 dark:bg-white/10">
                      <div
                        className={`h-full rounded-full transition-all motion-layout ${
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

                  <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-3">
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

                  <div className="break-words">Mode: {reindexJobQuery.data.mode}</div>
                  <div className="break-words">Source Types: {(reindexJobQuery.data.sourceTypes || []).join(', ') || '--'}</div>
                  <div className="break-all">Source IDs: {(reindexJobQuery.data.sourceIds || []).join(', ') || '--'}</div>
                  <div className="break-all">Cursor: {reindexJobQuery.data.lastCursor || '--'}</div>
                  <div className="break-words">Last Source Update: {formatDateTime(reindexJobQuery.data.lastSourceUpdatedAt)}</div>
                  <div className="break-words">Finished At: {formatDateTime(reindexJobQuery.data.finishedAt)}</div>

                  {!!reindexStats.length && (
                    <div className="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2">
                      {reindexStats.map((item) => (
                        <div key={item.key} className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/75 px-3 py-3 sm:px-4 sm:py-4 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{item.label}</div>
                          <div className="mt-2 break-all font-bold text-slate-900 dark:text-white">{item.value}</div>
                        </div>
                      ))}
                    </div>
                  )}

                  {reindexJobQuery.data.errorMessage && (
                    <div className="text-rose-500">Error: {translateConfigMessage(reindexJobQuery.data.errorMessage)}</div>
                  )}
                </div>
              )}
              <ConfirmationDialog
                open={reindexConfirmOpen}
                title="确认执行高影响 RAG Reindex？"
                description="本次任务会按当前范围重新计算并写入检索索引。"
                safety={`风险提示：${reindexRiskHints.join('；')}。现有业务数据不会被删除，但索引更新期间检索结果可能暂时不稳定。`}
                nextStep="先核对范围和风险提示；确认窗口、provider 与模型无误后再执行。"
                confirmLabel="确认执行 Reindex"
                cancelLabel="取消，返回编辑"
                pending={reindexMutation.isPending}
                pendingTitle="正在提交 Reindex"
                pendingDescription="任务请求已提交，请等待服务端返回任务状态。"
                onCancel={() => setReindexConfirmOpen(false)}
                onConfirm={() => {
                  reindexMutation.mutate(reindexForm, { onSettled: () => setReindexConfirmOpen(false) });
                }}
              />
            </SectionCard>
          </div>

          <SectionCard title="Producer Outbox" description="这里展示 AI 知识同步 outbox 的待发送 / 失败事件，并提供人工立即重放入口，避免同步静默丢失。">
            <div className="page-toolbar">
              <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:flex-wrap">
                <div className="filter-field">
                  <SelectInput
                    value={outboxStatus}
                    onChange={setOutboxStatus}
                    options={[
                      { value: '', label: '全部状态' },
                      { value: 'DLQ', label: 'DLQ' },
                      { value: 'FAILED', label: 'FAILED' },
                      { value: 'PENDING', label: 'PENDING' },
                      { value: 'IN_PROGRESS', label: 'IN_PROGRESS' },
                      { value: 'PUBLISHED', label: 'PUBLISHED' },
                    ]}
                  />
                </div>
                <div className="w-full min-w-0 sm:w-28">
                  <TextInput value={outboxLimit} onChange={setOutboxLimit} type="number" />
                </div>
              </div>
              <button
                type="button"
                onClick={() => void queryClient.invalidateQueries({ queryKey: ['admin-ai-outbox'] })}
                className="inline-flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white/70 px-5 py-3 text-sm font-bold text-slate-600 sm:w-auto dark:border-white/10 dark:bg-white/[0.04] dark:text-white/70"
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
                    <div key={record.id} className="min-w-0 space-y-4 rounded-2xl border border-slate-200/70 bg-white/60 p-3 sm:rounded-[1.8rem] sm:p-5 dark:border-white/10 dark:bg-white/[0.03]">
                      <div className="flex min-w-0 flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                        <div className="min-w-0 space-y-2">
                          <div className="break-all text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">{record.eventType}</div>
                          <div className="break-all text-lg font-black text-slate-900 dark:text-white">{record.eventId}</div>
                          <div className="break-all text-sm text-slate-500 dark:text-white/45">routingKey: {record.routingKey}</div>
                        </div>
                        <div className="flex flex-wrap items-center gap-2">
                          <div className={`rounded-2xl border px-3 py-2 text-xs font-bold ${statusClasses(tone)}`}>{record.status}</div>
                          <div className="rounded-2xl border border-slate-200/70 bg-white/70 px-3 py-2 text-xs font-bold text-slate-500 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/45">
                            attempts: {record.attemptCount}
                          </div>
                          {(record.status === 'FAILED' || record.status === 'PENDING' || record.status === 'DLQ') && (
                            <button
                              type="button"
                              onClick={() => replayOutboxMutation.mutate(record.id)}
                              disabled={replayOutboxMutation.isPending && replayingOutboxId === record.id}
                              className="btn-liquid px-4 py-2 text-sm text-white"
                            >
                              {replayOutboxMutation.isPending && replayingOutboxId === record.id ? '重放中...' : '立即重放'}
                            </button>
                          )}
                        </div>
                      </div>

                      <div className="grid min-w-0 grid-cols-1 gap-3 text-sm sm:grid-cols-2 xl:grid-cols-4">
                        <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/75 px-3 py-3 sm:px-4 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">Created</div>
                          <div className="mt-2 break-words text-slate-700 dark:text-white/70">{formatDateTime(record.createdAt)}</div>
                        </div>
                        <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/75 px-3 py-3 sm:px-4 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">Next Attempt</div>
                          <div className="mt-2 break-words text-slate-700 dark:text-white/70">{formatDateTime(record.nextAttemptAt)}</div>
                        </div>
                        <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/75 px-3 py-3 sm:px-4 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">Published</div>
                          <div className="mt-2 break-words text-slate-700 dark:text-white/70">{formatDateTime(record.publishedAt)}</div>
                        </div>
                        <div className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/75 px-3 py-3 sm:px-4 dark:border-white/10 dark:bg-slate-950/20">
                          <div className="text-[11px] uppercase tracking-[0.24em] text-slate-400 dark:text-white/30">Trace Id</div>
                          <div className="mt-2 break-all text-slate-700 dark:text-white/70">{record.traceId || '--'}</div>
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

      {saveReviewOpen && (
        <div className="fixed inset-0 z-[90] flex items-end justify-center bg-slate-950/55 px-3 py-4 sm:items-center sm:px-4 sm:py-8" role="presentation">
          <div
            role="dialog"
            aria-modal="true"
            aria-label="保存前确认本次改动"
            className="safe-area-dialog max-h-[min(90vh,100dvh-2rem)] w-full max-w-4xl min-w-0 overflow-y-auto rounded-2xl border border-slate-200/70 bg-surface p-4 shadow-2xl sm:rounded-[2rem] sm:p-6 dark:border-white/10"
          >
            <div className="flex min-w-0 flex-wrap items-start justify-between gap-4">
              <div className="min-w-0">
                <div className="text-[11px] uppercase tracking-[0.28em] text-slate-400 dark:text-white/30">Change Review</div>
                <div className="mt-2 text-xl font-black text-slate-900 sm:text-2xl dark:text-white">保存前确认本次改动</div>
                <div className="mt-3 text-sm text-slate-500 dark:text-white/45">
                  先确认字段 diff、密钥处理和风险提示，再决定是否保存并立即生效。
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSaveReviewOpen(false)}
                className="rounded-2xl border border-slate-200/70 px-4 py-2 text-sm text-slate-600 dark:border-white/10 dark:text-white/70"
              >
                关闭
              </button>
            </div>

            <div className="mt-6 grid min-w-0 gap-4 sm:gap-6 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
              <div className="min-w-0 space-y-4">
                <div className="min-w-0 rounded-[1.6rem] border border-slate-200/70 bg-white/70 p-3 sm:p-4 dark:border-white/10 dark:bg-slate-950/20">
                  <div className="font-bold text-slate-900 dark:text-white">配置字段差异</div>
                  <div className="mt-3 space-y-3 text-sm">
                    {visibleDiffs.map((entry) => (
                      <div key={`${entry.field}-${entry.after}`} className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/80 px-3 py-3 sm:px-4 dark:border-white/10 dark:bg-white/[0.03]">
                        <div className="break-words font-semibold text-slate-900 dark:text-white">{humanizeFieldName(entry.field)}</div>
                        <div className="mt-2 break-all text-slate-500 dark:text-white/45">旧值: {entry.before}</div>
                        <div className="mt-1 break-all text-slate-700 dark:text-white/70">新值: {entry.after}</div>
                      </div>
                    ))}
                    {configDiffs.length > visibleDiffs.length && (
                      <div className="text-xs text-slate-400 dark:text-white/30">还有 {configDiffs.length - visibleDiffs.length} 项差异未展开显示。</div>
                    )}
                    {!configDiffs.length && (
                      <div className="text-slate-500 dark:text-white/45">当前没有普通字段差异。</div>
                    )}
                  </div>
                </div>

                <div className="min-w-0 rounded-[1.6rem] border border-slate-200/70 bg-white/70 p-3 sm:p-4 dark:border-white/10 dark:bg-slate-950/20">
                  <div className="font-bold text-slate-900 dark:text-white">密钥处理</div>
                  <div className="mt-3 space-y-3 text-sm">
                    {visibleSecretChanges.map((change) => (
                      <div key={change.field} className="min-w-0 rounded-2xl border border-slate-200/70 bg-white/80 px-3 py-3 sm:px-4 dark:border-white/10 dark:bg-white/[0.03]">
                        <div className="break-words font-semibold text-slate-900 dark:text-white">{humanizeFieldName(change.field)}</div>
                        <div className="mt-2 break-words text-slate-600 dark:text-white/60">{change.action}</div>
                      </div>
                    ))}
                    {secretChanges.length > visibleSecretChanges.length && (
                      <div className="text-xs text-slate-400 dark:text-white/30">还有 {secretChanges.length - visibleSecretChanges.length} 项密钥变更未展开显示。</div>
                    )}
                    {!secretChanges.length && (
                      <div className="text-slate-500 dark:text-white/45">当前没有密钥写入、覆盖或清空操作。</div>
                    )}
                  </div>
                </div>
              </div>

              <div className="min-w-0 space-y-4">
                <div className="space-y-2 rounded-[1.6rem] border border-amber-500/20 bg-amber-500/5 p-4 text-sm text-amber-700 dark:text-amber-400">
                  <div className="font-bold">风险提示</div>
                  {draftRiskHints.length > 0 ? (
                    draftRiskHints.map((hint) => <div key={hint} className="break-words">{hint}</div>)
                  ) : (
                    <div>当前草稿没有识别到高风险改动。</div>
                  )}
                </div>

                <div className="rounded-[1.6rem] border border-slate-200/70 bg-white/70 p-4 text-sm text-slate-600 dark:border-white/10 dark:bg-slate-950/20 dark:text-white/55">
                  <div className="font-bold text-slate-900 dark:text-white">保存后的建议动作</div>
                  <div className="mt-3 space-y-2">
                    <div>1. 先刷新运行态健康检查，确认 active/fallback provider 与模型信息一致。</div>
                    <div>2. 如果动了 RAG 参数，至少做一轮抽样检索验证。</div>
                    <div>3. 如果更换了 embedding 维度或 provider 超时，必要时再执行 reindex。</div>
                  </div>
                </div>
              </div>
            </div>

            <div className="page-actions mt-6 sm:justify-end">
              <button
                type="button"
                onClick={() => setSaveReviewOpen(false)}
                className="rounded-2xl border border-slate-200/70 px-5 py-3 text-sm text-slate-600 dark:border-white/10 dark:text-white/70"
              >
                返回继续编辑
              </button>
              <button
                type="button"
                onClick={confirmSave}
                disabled={saveMutation.isPending}
                className="btn-liquid px-5 py-3 text-white disabled:opacity-60"
              >
                {saveMutation.isPending ? '保存中...' : '确认保存并生效'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminConfigCenterPage;

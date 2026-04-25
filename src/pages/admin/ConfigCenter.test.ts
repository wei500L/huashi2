import { describe, expect, it } from 'vitest';
import { AdminAiConfigContractError, buildSavePayload, normalizeAdminAiConfigView } from './ConfigCenter';

function createConfigViewEnvelope() {
  return {
    config: {
      provider: {
        activeProvider: null,
        fallbackProvider: null,
        providers: {
          qwen: {
            chat: {
              protocol: 'openai-compat',
              baseUrl: null,
              apiKey: null,
              model: null,
              connectTimeout: null,
              readTimeout: null,
              temperature: null,
              maxTokens: null,
            },
            embedding: {
              protocol: 'openai-compat',
              baseUrl: null,
              apiKey: null,
              model: null,
              connectTimeout: null,
              readTimeout: null,
              dimension: null,
            },
            rerank: {
              protocol: 'openai-rerank',
              baseUrl: null,
              apiKey: null,
              model: null,
              connectTimeout: null,
              readTimeout: null,
            },
          },
        },
      },
      resilience: {
        maxAttempts: null,
        waitDuration: null,
        failureRateThreshold: null,
        slidingWindowSize: null,
        openStateDuration: null,
      },
      rag: {
        appServer: {
          baseUrl: null,
          internalToken: null,
          connectTimeout: null,
          readTimeout: null,
        },
        ingestion: {
          exportPageSize: null,
          embeddingBatchSize: null,
        },
        retrieval: {
          recallTopK: null,
          recallThreshold: null,
          rerankTopN: null,
          rerankThreshold: null,
          finalTopK: null,
          hnswEfSearch: null,
        },
      },
    },
    secrets: {
      providers: {
        qwen: {
          chatApiKey: {
            configured: false,
            maskedValue: '',
            valueLength: null,
          },
          embeddingApiKey: {
            configured: true,
            maskedValue: 'emb******001',
            valueLength: 16,
          },
          rerankApiKey: {
            configured: false,
            maskedValue: '',
            valueLength: null,
          },
        },
      },
      appServerInternalToken: {
        configured: false,
        maskedValue: '',
        valueLength: null,
      },
    },
    source: null,
    version: null,
    updatedAt: null,
    notices: [],
    runtime: {
      available: true,
      source: null,
      version: '9',
      appliedAt: null,
      inSync: true,
    },
    stored: {
      present: true,
      version: '9',
      updatedAt: null,
    },
  };
}

describe('ConfigCenter contract guards', () => {
  it('rejects responses that omit runtime', () => {
    const invalid = createConfigViewEnvelope() as Record<string, unknown>;
    delete invalid.runtime;

    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(AdminAiConfigContractError);
    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(/runtime/);
  });

  it('rejects responses that omit stored state', () => {
    const invalid = createConfigViewEnvelope() as Record<string, unknown>;
    delete invalid.stored;

    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(AdminAiConfigContractError);
    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(/stored/);
  });

  it('rejects responses that omit provider secret groups', () => {
    const invalid = createConfigViewEnvelope() as unknown as Record<string, unknown>;
    const secrets = invalid.secrets as Record<string, unknown>;
    const providers = secrets.providers as Record<string, unknown>;
    delete providers.qwen;

    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(AdminAiConfigContractError);
    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(/secrets\.providers\.qwen/);
  });

  it('rejects responses with malformed runtime flags', () => {
    const invalid = createConfigViewEnvelope() as Record<string, unknown>;
    invalid.runtime = {
      ...createConfigViewEnvelope().runtime,
      inSync: 'yes',
    };

    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(AdminAiConfigContractError);
    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(/runtime\.inSync/);
  });

  it('rejects responses with malformed notices', () => {
    const invalid = createConfigViewEnvelope() as Record<string, unknown>;
    invalid.notices = [{ code: 'ok', severity: 'info', defaultMessage: 'ok' }, 42];

    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(AdminAiConfigContractError);
    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(/notices\[1\]/);
  });

  it('rejects responses with malformed provider config scalars', () => {
    const invalid = createConfigViewEnvelope() as unknown as Record<string, unknown>;
    const config = invalid.config as Record<string, unknown>;
    const provider = config.provider as Record<string, unknown>;
    const providers = provider.providers as Record<string, unknown>;
    const qwen = providers.qwen as Record<string, unknown>;
    const chat = qwen.chat as Record<string, unknown>;
    chat.temperature = 'low';

    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(AdminAiConfigContractError);
    expect(() => normalizeAdminAiConfigView(invalid)).toThrow(/chat\.temperature/);
  });

  it('preserves nullable leaves from a valid envelope', () => {
    const normalized = normalizeAdminAiConfigView(createConfigViewEnvelope());

    expect(normalized.config.provider.activeProvider).toBeNull();
    expect(normalized.config.provider.providers.qwen.chat.baseUrl).toBeNull();
    expect(normalized.config.provider.providers.qwen.embedding.dimension).toBeNull();
    expect(normalized.config.rag.retrieval.finalTopK).toBeNull();
    expect(normalized.runtime.available).toBe(true);
    expect(normalized.stored.version).toBe('9');
  });

  it('accepts blank draft envelopes when runtime and stored snapshots are both absent', () => {
    const envelope = createConfigViewEnvelope() as unknown as Record<string, unknown>;
    const config = envelope.config as Record<string, unknown>;
    const provider = config.provider as Record<string, unknown>;
    const secrets = envelope.secrets as Record<string, unknown>;
    provider.providers = {};
    secrets.providers = {};
    envelope.source = null;
    envelope.version = null;
    envelope.updatedAt = null;
    envelope.notices = [
      {
        code: 'no_stored_snapshot_yet',
        severity: 'warning',
        defaultMessage: 'No stored AI ops config exists yet. The page is showing an unsynced draft that can be saved as the first snapshot.',
      },
    ];
    envelope.runtime = {
      available: false,
      source: null,
      version: null,
      appliedAt: null,
      inSync: false,
    };
    envelope.stored = {
      present: false,
      version: null,
      updatedAt: null,
    };

    const normalized = normalizeAdminAiConfigView(envelope);

    expect(normalized.config.provider.providers).toEqual({});
    expect(normalized.config.resilience.openStateDuration).toBeNull();
    expect(normalized.config.rag.ingestion.embeddingBatchSize).toBeNull();
    expect(normalized.runtime.available).toBe(false);
    expect(normalized.stored.present).toBe(false);
  });

  it('accepts arbitrary technical provider keys', () => {
    const envelope = createConfigViewEnvelope() as unknown as Record<string, unknown>;
    const config = envelope.config as Record<string, unknown>;
    const provider = config.provider as Record<string, unknown>;
    const providerDefinitions = provider.providers as Record<string, unknown>;
    const secrets = envelope.secrets as Record<string, unknown>;
    const secretProviders = secrets.providers as Record<string, unknown>;
    provider.providers = {
      openai_main: providerDefinitions.qwen,
      backup_1: providerDefinitions.qwen,
    };
    provider.activeProvider = 'openai_main';
    provider.fallbackProvider = 'backup_1';
    secrets.providers = {
      openai_main: secretProviders.qwen,
      backup_1: secretProviders.qwen,
    };

    const normalized = normalizeAdminAiConfigView(envelope);

    expect(normalized.config.provider.activeProvider).toBe('openai_main');
    expect(normalized.config.provider.fallbackProvider).toBe('backup_1');
    expect(Object.keys(normalized.config.provider.providers)).toEqual(['openai_main', 'backup_1']);
  });

  it('canonicalizes provider order with active and fallback first', () => {
    const envelope = createConfigViewEnvelope() as unknown as Record<string, unknown>;
    const config = envelope.config as Record<string, unknown>;
    const provider = config.provider as Record<string, unknown>;
    const providerDefinitions = provider.providers as Record<string, unknown>;
    const secrets = envelope.secrets as Record<string, unknown>;
    const secretProviders = secrets.providers as Record<string, unknown>;
    provider.providers = {
      archive: providerDefinitions.qwen,
      qwen: providerDefinitions.qwen,
      deepseek: providerDefinitions.qwen,
    };
    provider.activeProvider = 'deepseek';
    provider.fallbackProvider = 'qwen';
    secrets.providers = {
      archive: secretProviders.qwen,
      qwen: secretProviders.qwen,
      deepseek: secretProviders.qwen,
    };

    const normalized = normalizeAdminAiConfigView(envelope);

    expect(Object.keys(normalized.config.provider.providers)).toEqual(['deepseek', 'qwen', 'archive']);
    expect(Object.keys(normalized.secrets.providers)).toEqual(['deepseek', 'qwen', 'archive']);
  });

  it('includes providerOrigins for renamed providers when building save payload', () => {
    const normalized = normalizeAdminAiConfigView(createConfigViewEnvelope());
    normalized.config.provider.providers = {
      primary_openai: normalized.config.provider.providers.qwen,
    };
    normalized.config.provider.activeProvider = 'primary_openai';
    normalized.config.provider.fallbackProvider = 'primary_openai';
    const payload = buildSavePayload(
      normalized.config,
      {
        providers: {
          primary_openai: {
            chatApiKey: { retainExisting: true, value: '' },
            embeddingApiKey: { retainExisting: true, value: '' },
            rerankApiKey: { retainExisting: true, value: '' },
          },
      },
      appServerInternalToken: { retainExisting: true, value: '' },
    },
      '9',
      { primary_openai: 'qwen' }
    );

    expect(payload.providerOrigins).toEqual({ primary_openai: 'qwen' });
  });

  it('canonicalizes provider order when building save payload', () => {
    const normalized = normalizeAdminAiConfigView(createConfigViewEnvelope());
    normalized.config.provider.providers = {
      archive: normalized.config.provider.providers.qwen,
      qwen: normalized.config.provider.providers.qwen,
      deepseek: normalized.config.provider.providers.qwen,
    };
    normalized.config.provider.activeProvider = 'deepseek';
    normalized.config.provider.fallbackProvider = 'qwen';
    const payload = buildSavePayload(
      normalized.config,
      {
        providers: {
          archive: {
            chatApiKey: { retainExisting: true, value: '' },
            embeddingApiKey: { retainExisting: true, value: '' },
            rerankApiKey: { retainExisting: true, value: '' },
          },
          qwen: {
            chatApiKey: { retainExisting: true, value: '' },
            embeddingApiKey: { retainExisting: true, value: '' },
            rerankApiKey: { retainExisting: true, value: '' },
          },
          deepseek: {
            chatApiKey: { retainExisting: true, value: '' },
            embeddingApiKey: { retainExisting: true, value: '' },
            rerankApiKey: { retainExisting: true, value: '' },
          },
      },
      appServerInternalToken: { retainExisting: true, value: '' },
    },
      '9'
    );

    expect(Object.keys(payload.config.provider.providers)).toEqual(['deepseek', 'qwen', 'archive']);
    expect(Object.keys(payload.secrets.providers || {})).toEqual(['deepseek', 'qwen', 'archive']);
  });

  it('materializes strict config leaves when building save payload', () => {
    const normalized = normalizeAdminAiConfigView(createConfigViewEnvelope());
    const payload = buildSavePayload(
      normalized.config,
      {
        providers: {
          qwen: {
            chatApiKey: { retainExisting: true, value: '' },
            embeddingApiKey: { retainExisting: true, value: '' },
            rerankApiKey: { retainExisting: true, value: '' },
          },
        },
        appServerInternalToken: { retainExisting: true, value: '' },
      },
      '9'
    );

    expect(payload.expectedVersion).toBe('9');
    expect(payload.config.provider.activeProvider).toBe('');
    expect(payload.config.provider.fallbackProvider).toBe('');
    expect(payload.config.provider.providers.qwen.chat.baseUrl).toBe('');
    expect(payload.config.provider.providers.qwen.chat.temperature).toBe(0);
    expect(payload.config.rag.ingestion.exportPageSize).toBe(0);
  });
});

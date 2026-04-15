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
              baseUrl: null,
              apiKey: null,
              model: null,
              timeout: null,
              temperature: null,
              maxTokens: null,
            },
            embedding: {
              baseUrl: null,
              apiKey: null,
              model: null,
              timeout: null,
              dimension: null,
            },
            rerank: {
              baseUrl: null,
              apiKey: null,
              model: null,
              timeout: null,
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
        },
      },
    },
    secrets: {
      providers: {
        qwen: {
          chatApiKey: {
            configured: false,
            maskedValue: '',
          },
          embeddingApiKey: {
            configured: true,
            maskedValue: 'emb******001',
          },
          rerankApiKey: {
            configured: false,
            maskedValue: '',
          },
        },
      },
      appServerInternalToken: {
        configured: false,
        maskedValue: '',
      },
    },
    source: null,
    version: null,
    updatedAt: null,
    notices: [],
    runtime: {
      available: true,
      source: null,
      version: 9,
      appliedAt: null,
      inSync: true,
    },
    stored: {
      present: true,
      version: 9,
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

  it('rejects responses with non-string notices', () => {
    const invalid = createConfigViewEnvelope() as Record<string, unknown>;
    invalid.notices = ['ok', 42];

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
    expect(normalized.stored.version).toBe(9);
  });

  it('accepts arbitrary technical provider keys', () => {
    const envelope = createConfigViewEnvelope() as any;
    envelope.config.provider.providers = {
      openai_main: envelope.config.provider.providers.qwen,
      backup_1: envelope.config.provider.providers.qwen,
    };
    envelope.config.provider.activeProvider = 'openai_main';
    envelope.config.provider.fallbackProvider = 'backup_1';
    envelope.secrets.providers = {
      openai_main: envelope.secrets.providers.qwen,
      backup_1: envelope.secrets.providers.qwen,
    };

    const normalized = normalizeAdminAiConfigView(envelope);

    expect(normalized.config.provider.activeProvider).toBe('openai_main');
    expect(normalized.config.provider.fallbackProvider).toBe('backup_1');
    expect(Object.keys(normalized.config.provider.providers)).toEqual(['openai_main', 'backup_1']);
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
      9,
      { primary_openai: 'qwen' }
    );

    expect(payload.providerOrigins).toEqual({ primary_openai: 'qwen' });
  });

  it('keeps nullable config leaves when building save payload', () => {
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
      9
    );

    expect(payload.config.provider.activeProvider).toBeNull();
    expect(payload.config.provider.providers.qwen.chat.temperature).toBeNull();
    expect(payload.config.rag.ingestion.exportPageSize).toBeNull();
  });
});

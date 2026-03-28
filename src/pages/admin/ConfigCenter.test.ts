import { describe, expect, it } from 'vitest';
import { AdminAiConfigContractError, normalizeAdminAiConfigView } from './ConfigCenter';

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

  it('converts a valid envelope with null leaves into an editable draft', () => {
    const normalized = normalizeAdminAiConfigView(createConfigViewEnvelope());

    expect(normalized.config.provider.activeProvider).toBe('');
    expect(normalized.config.provider.providers.qwen.chat.baseUrl).toBe('');
    expect(normalized.config.provider.providers.qwen.embedding.dimension).toBe(0);
    expect(normalized.config.rag.retrieval.finalTopK).toBe(0);
    expect(normalized.runtime.available).toBe(true);
    expect(normalized.stored.version).toBe(9);
  });
});

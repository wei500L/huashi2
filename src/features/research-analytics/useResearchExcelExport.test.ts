import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api';
import { researchAnalyticsService } from '@/lib/services';
import { toResearchExportErrorMessage, waitForResearchExportJob } from './useResearchExcelExport';
import type { ResearchExportJobVO } from '@/lib/contracts';

vi.mock('@/lib/services', () => ({
  researchAnalyticsService: {
    getExport: vi.fn(),
  },
}));

function job(overrides: Partial<ResearchExportJobVO> = {}): ResearchExportJobVO {
  return {
    jobId: 12,
    jobKey: 'abc',
    publishId: 5,
    status: 'PENDING',
    format: 'XLSX',
    scope: 'ATTEMPTS',
    includeSensitiveFields: false,
    includeAttachmentManifest: true,
    fileName: '研究数据.xlsx',
    ...overrides,
  };
}

describe('toResearchExportErrorMessage', () => {
  it('translates axios network failures', () => {
    expect(toResearchExportErrorMessage(new ApiError('Network Error'))).toBe('导出服务中断，请稍后重试。');
    expect(toResearchExportErrorMessage(new Error('timeout of 60000ms exceeded'))).toBe('导出超时，请稍后重试。');
  });

  it('keeps backend failure messages', () => {
    expect(toResearchExportErrorMessage(new Error('磁盘不可写'))).toBe('磁盘不可写');
  });
});

describe('waitForResearchExportJob', () => {
  afterEach(() => {
    vi.mocked(researchAnalyticsService.getExport).mockReset();
  });

  it('returns immediately when the job is already complete', async () => {
    const completed = job({ status: 'COMPLETED' });
    await expect(waitForResearchExportJob(completed, { poll: vi.fn() })).resolves.toEqual(completed);
  });

  it('polls until the job completes', async () => {
    const poll = vi.fn()
      .mockResolvedValueOnce(job({ status: 'PROCESSING' }))
      .mockResolvedValueOnce(job({ status: 'COMPLETED' }));
    const wait = vi.fn().mockResolvedValue(undefined);

    const result = await waitForResearchExportJob(job({ status: 'PENDING' }), { poll, wait });

    expect(result.status).toBe('COMPLETED');
    expect(poll).toHaveBeenCalledTimes(2);
    expect(wait).toHaveBeenCalled();
  });

  it('times out if the job stays pending', async () => {
    let now = 0;
    const poll = vi.fn().mockResolvedValue(job({ status: 'PROCESSING' }));
    const wait = vi.fn().mockImplementation(async () => {
      now += 1_000;
    });

    await expect(waitForResearchExportJob(job({ status: 'PENDING' }), {
      now: () => now,
      poll,
      wait,
      timeoutMs: 2_000,
    })).rejects.toThrow('导出超时，请稍后重试。');
  });
});

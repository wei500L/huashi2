import React from 'react';
import { ApiError, getApiErrorMessage, saveBlob } from '@/lib/api';
import type { ResearchAnalyticsFilter, ResearchExportJobVO } from '@/lib/contracts';
import { researchAnalyticsService } from '@/lib/services';

const EXPORT_TIMEOUT_MS = 360_000;
const POLL_INTERVAL_MS = 1_000;
const CREATE_TIMEOUT_MS = 20_000;
const DOWNLOAD_TIMEOUT_MS = 180_000;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}

function isPendingExport(status?: string | null): boolean {
  return status === 'PENDING' || status === 'PROCESSING';
}

export function toResearchExportErrorMessage(error: unknown): string {
  const normalized = error instanceof ApiError ? error : null;
  const raw = getApiErrorMessage(error, '导出失败。');
  if (/network error/i.test(raw) || normalized?.message === 'Network Error') {
    return '导出服务中断，请稍后重试。';
  }
  if (/timeout/i.test(raw) || /econnaborted/i.test(raw)) {
    return '导出超时，请稍后重试。';
  }
  return raw || '导出失败。';
}

export async function waitForResearchExportJob(
  initial: ResearchExportJobVO,
  options?: {
    now?: () => number;
    poll?: (jobId: number) => Promise<ResearchExportJobVO>;
    wait?: (ms: number) => Promise<void>;
    timeoutMs?: number;
  }
): Promise<ResearchExportJobVO> {
  const now = options?.now ?? Date.now;
  const poll = options?.poll ?? ((jobId) => researchAnalyticsService.getExport(jobId));
  const wait = options?.wait ?? sleep;
  const timeoutMs = options?.timeoutMs ?? EXPORT_TIMEOUT_MS;
  const deadline = now() + timeoutMs;
  let current = initial;
  while (isPendingExport(current.status)) {
    if (now() >= deadline) {
      throw new Error('导出超时，请稍后重试。');
    }
    await wait(POLL_INTERVAL_MS);
    current = await poll(current.jobId);
  }
  return current;
}

export function useResearchExcelExport() {
  const [exporting, setExporting] = React.useState(false);
  const [exportError, setExportError] = React.useState<string | null>(null);
  const [lastFileName, setLastFileName] = React.useState<string | null>(null);

  const exportExcel = React.useCallback(async (publishId: number, filters?: ResearchAnalyticsFilter) => {
    setExporting(true);
    setExportError(null);
    try {
      const created = await researchAnalyticsService.createExport(publishId, {
        ...filters,
        format: 'XLSX',
        includeAttachmentManifest: true,
      }, { timeout: CREATE_TIMEOUT_MS });
      const job = await waitForResearchExportJob(created);
      if (job.status === 'FAILED') {
        throw new Error(job.errorMessage || '导出失败。');
      }
      if (job.status !== 'COMPLETED') {
        throw new Error(job.errorMessage || '导出失败。');
      }
      const fileName = job.fileName || '研究数据.xlsx';
      saveBlob(await researchAnalyticsService.downloadExport(job.jobId, { timeout: DOWNLOAD_TIMEOUT_MS }), fileName);
      setLastFileName(fileName);
    } catch (error) {
      setExportError(toResearchExportErrorMessage(error));
    } finally {
      setExporting(false);
    }
  }, []);

  return { exporting, exportError, lastFileName, exportExcel };
}

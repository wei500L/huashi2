import React from 'react';
import { getApiErrorMessage, saveBlob } from '@/lib/api';
import type { ResearchAnalyticsFilter } from '@/lib/contracts';
import { researchAnalyticsService } from '@/lib/services';

const EXPORT_TIMEOUT_MS = 60_000;

export function useResearchExcelExport() {
  const [exporting, setExporting] = React.useState(false);
  const [exportError, setExportError] = React.useState<string | null>(null);
  const [lastFileName, setLastFileName] = React.useState<string | null>(null);

  const exportExcel = React.useCallback(async (publishId: number, filters?: ResearchAnalyticsFilter) => {
    setExporting(true);
    setExportError(null);
    try {
      const job = await researchAnalyticsService.createExport(publishId, {
        ...filters,
        format: 'XLSX',
        includeAttachmentManifest: true,
      }, { timeout: EXPORT_TIMEOUT_MS });
      if (job.status === 'FAILED') {
        throw new Error(job.errorMessage || '导出失败。');
      }
      const fileName = job.fileName || '研究数据.xlsx';
      saveBlob(await researchAnalyticsService.downloadExport(job.jobId, { timeout: EXPORT_TIMEOUT_MS }), fileName);
      setLastFileName(fileName);
    } catch (error) {
      setExportError(getApiErrorMessage(error, '导出失败。'));
    } finally {
      setExporting(false);
    }
  }, []);

  return { exporting, exportError, lastFileName, exportExcel };
}

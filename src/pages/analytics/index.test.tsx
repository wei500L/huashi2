import type { ReactNode, RefObject } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { exportReportPagesToPdf } from '@/lib/pdf-report';
import { studentService } from '@/lib/services';
import AnalyticsPage from './index';

vi.mock('@/components/common', () => ({
  PageHeader: ({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {subtitle ? <p>{subtitle}</p> : null}
      <div>{actions}</div>
    </div>
  ),
  SectionEyebrow: ({ children, className }: { children: ReactNode; className?: string }) => <div className={className}>{children}</div>,
}));

vi.mock('@/components/common/ChartCard', () => ({
  ChartCard: ({ title }: { title: string }) => <div>{title}</div>,
}));

vi.mock('@/components/analytics/AnalyticsPdfReport', () => ({
  StudentAnalyticsPdfReport: ({ reportRef }: { reportRef: RefObject<HTMLDivElement | null> }) => (
    <div ref={reportRef}>student-pdf-report</div>
  ),
}));

vi.mock('@/lib/pdf-report', () => ({
  exportReportPagesToPdf: vi.fn(),
}));

vi.mock('@/lib/services', () => ({
  studentService: {
    getOverview: vi.fn(),
    getTrends: vi.fn(),
    getHeatmap: vi.fn(),
    getScatter: vi.fn(),
    getHighRiskPairs: vi.fn(),
    getErrorDistribution: vi.fn(),
    exportCsv: vi.fn(),
  },
}));

const queryClients: QueryClient[] = [];

function createDeferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolver) => {
    resolve = resolver;
  });
  return { promise, resolve };
}

function createOverview() {
  return {
    studentName: '李同学',
    gradeName: '高一',
    frenchLevel: 'A1',
    primaryRiskLevel: 'MEDIUM',
    recommendedTrainingMode: 'LEXICAL_DISTINCTION',
    cards: [],
    radar: [],
    contextPerformance: [],
    latestSnapshot: {
      recentAccuracy: 0.78,
      recentNegativeTransferRisk: 0.34,
      recentAvgReactionTimeMs: 820,
      pendingReviewCount: 3,
      focusTags: [],
    },
  };
}

function createTrend() {
  return {
    labels: ['2026-04-01'],
    series: [],
  };
}

function createHeatmap() {
  return {
    xLabels: [],
    yLabels: [],
    cells: [],
  };
}

function createScatter() {
  return {
    points: [],
  };
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });
  queryClients.push(client);

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AnalyticsPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('AnalyticsPage PDF export', () => {
  beforeEach(() => {
    vi.mocked(studentService.getOverview).mockResolvedValue(createOverview() as never);
    vi.mocked(studentService.getTrends).mockResolvedValue(createTrend() as never);
    vi.mocked(studentService.getHeatmap).mockResolvedValue(createHeatmap() as never);
    vi.mocked(studentService.getScatter).mockResolvedValue(createScatter() as never);
    vi.mocked(studentService.getHighRiskPairs).mockResolvedValue([]);
    vi.mocked(studentService.getErrorDistribution).mockResolvedValue([]);
    vi.mocked(studentService.exportCsv).mockResolvedValue(new Blob());
    vi.mocked(exportReportPagesToPdf).mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
    queryClients.splice(0).forEach((client) => client.clear());
    vi.clearAllMocks();
  });

  it('waits for the hidden report to mount before exporting PDF', async () => {
    renderPage();
    const user = userEvent.setup();

    const exportButton = await screen.findByRole('button', { name: /导出 PDF|Export PDF/ });
    await waitFor(() => expect(exportButton).toBeEnabled());

    await user.click(exportButton);

    await waitFor(() => {
      expect(exportReportPagesToPdf).toHaveBeenCalledTimes(1);
    });
    expect(vi.mocked(exportReportPagesToPdf).mock.calls[0][0]).toBeInstanceOf(HTMLDivElement);
    expect(vi.mocked(exportReportPagesToPdf).mock.calls[0][1]).toBe('student-analytics-30d-report.pdf');
  });

  it('keeps PDF export disabled until high-risk-pair data is loaded', async () => {
    const highRiskPairsDeferred = createDeferred<[]>();
    vi.mocked(studentService.getHighRiskPairs).mockReturnValueOnce(highRiskPairsDeferred.promise);

    renderPage();

    const exportButton = await screen.findByRole('button', { name: /导出 PDF|Export PDF/ });
    await waitFor(() => {
      expect(studentService.getOverview).toHaveBeenCalled();
      expect(studentService.getScatter).toHaveBeenCalled();
    });
    expect(exportButton).toBeDisabled();

    highRiskPairsDeferred.resolve([]);

    await waitFor(() => expect(exportButton).toBeEnabled());
  });
});

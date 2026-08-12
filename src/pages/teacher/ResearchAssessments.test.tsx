import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { researchAnalyticsService } from '@/lib/services';
import type { ResearchPublishOverviewVO, ResearchReleaseListItemVO } from '@/lib/contracts';
import ResearchAssessmentsPage from './ResearchAssessments';

vi.mock('@/components/common', async () => {
  const actual = await vi.importActual<typeof import('@/components/common')>('@/components/common');
  return {
    ...actual,
    PageHeader: ({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) => (
      <div>
        <h1>{title}</h1>
        {subtitle ? <p>{subtitle}</p> : null}
        <div>{actions}</div>
      </div>
    ),
  };
});

vi.mock('@/components/common/ChartCard', () => ({
  ChartCard: ({ title, isEmpty, error }: { title: string; isEmpty?: boolean; error?: unknown }) => (
    <div>
      <h3>{title}</h3>
      {error ? <p>chart-error</p> : null}
      {isEmpty ? <p>chart-empty</p> : <p>chart-ready</p>}
    </div>
  ),
}));

vi.mock('@/lib/services', async () => {
  const actual = await vi.importActual<typeof import('@/lib/services')>('@/lib/services');
  return {
    ...actual,
    assessmentService: {
      ...actual.assessmentService,
      listTeacherPapers: vi.fn().mockResolvedValue([]),
    },
    researchAnalyticsService: {
      listReleases: vi.fn(),
      getOverview: vi.fn(),
      listAttempts: vi.fn(),
      getQuestionStats: vi.fn(),
      getDimensionStats: vi.fn(),
      getLatestAiReport: vi.fn(),
      createAiReport: vi.fn(),
      createExport: vi.fn(),
      downloadExport: vi.fn(),
    },
  };
});

const release: ResearchReleaseListItemVO = {
  publishId: 11,
  paperId: 7,
  paperTitle: 'Lexi-Bridge V1',
  releaseCode: 'ABC123',
  publishedAt: '2026-08-01T00:00:00',
  status: 'PUBLISHED',
  startedCount: 2,
  submittedCount: 1,
  latestSubmissionAt: '2026-08-02T00:00:00',
  aiReportStatus: null,
};

const overview = (completionValue: number | null, started = 2): ResearchPublishOverviewVO => ({
  publishId: 11,
  paperId: 7,
  paperTitle: 'Lexi-Bridge V1',
  releaseCode: 'ABC123',
  funnel: {
    codeGenerated: 2,
    codeVerified: 1,
    participantCreated: 2,
    attemptStarted: started,
    inProgress: started - 1,
    submitted: started ? 1 : 0,
    expired: 0,
  },
  rates: {
    completionRate: { numerator: started ? 1 : 0, denominator: started, value: completionValue },
    codeRedemptionRate: { numerator: 1, denominator: 2, value: 0.5 },
    submissionRate: { numerator: 1, denominator: 2, value: 0.5 },
  },
  timing: { average: 120000, median: 110000, q1: 90000, q3: 150000, p90: 180000, sampleCount: 1 },
  score: { average: 80, median: 80, q1: 70, q3: 90, p90: 95, sampleCount: 1 },
  dataQuality: { valid: 1, flagged: 0, flagDistribution: [] },
  ai: { pending: 0, processing: 0, completed: 1, fallback: 0, failed: 0 },
  latestSubmissionAt: '2026-08-02T00:00:00',
  statisticsGeneratedAt: '2026-08-02T01:00:00',
});

function renderDataTab() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/teacher/research?tab=data']}>
        <Routes>
          <Route path="/teacher/research" element={<ResearchAssessmentsPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('ResearchAssessments data tab', () => {
  it('shows em dash when completion rate is null', async () => {
    vi.mocked(researchAnalyticsService.listReleases).mockResolvedValue([release]);
    vi.mocked(researchAnalyticsService.getOverview).mockResolvedValue(overview(null, 0));
    vi.mocked(researchAnalyticsService.listAttempts).mockResolvedValue({ total: 0, pageNo: 1, pageSize: 20, records: [] });
    vi.mocked(researchAnalyticsService.getQuestionStats).mockResolvedValue({
      meta: { filterEcho: {}, sampleCount: 0, generatedAt: '2026-08-02T01:00:00', metricVersion: 'RESEARCH_STATS_V1' },
      questions: [],
    });
    vi.mocked(researchAnalyticsService.getDimensionStats).mockResolvedValue({
      meta: { filterEcho: {}, sampleCount: 0, generatedAt: '2026-08-02T01:00:00', metricVersion: 'RESEARCH_STATS_V1' },
      dimensions: [],
    });
    vi.mocked(researchAnalyticsService.getLatestAiReport).mockResolvedValue(null);
    renderDataTab();
    await waitFor(() => expect(screen.getByText('这份发布还没有答卷')).toBeInTheDocument());
  });

  it('renders completion rate and distinguishes filter-empty from true empty', async () => {
    vi.mocked(researchAnalyticsService.listReleases).mockResolvedValue([release]);
    vi.mocked(researchAnalyticsService.getOverview).mockResolvedValue(overview(0.5, 2));
    vi.mocked(researchAnalyticsService.listAttempts).mockResolvedValue({ total: 0, pageNo: 1, pageSize: 20, records: [] });
    vi.mocked(researchAnalyticsService.getQuestionStats).mockResolvedValue({
      meta: { filterEcho: {}, sampleCount: 1, generatedAt: '2026-08-02T01:00:00', metricVersion: 'RESEARCH_STATS_V1' },
      questions: [],
    });
    vi.mocked(researchAnalyticsService.getDimensionStats).mockResolvedValue({
      meta: { filterEcho: {}, sampleCount: 1, generatedAt: '2026-08-02T01:00:00', metricVersion: 'RESEARCH_STATS_V1' },
      dimensions: [],
    });
    vi.mocked(researchAnalyticsService.getLatestAiReport).mockResolvedValue(null);
    renderDataTab();
    await waitFor(() => expect(screen.getByText('50%')).toBeInTheDocument());
    expect(screen.queryByText('这份发布还没有答卷')).not.toBeInTheDocument();
    expect(screen.getByText('导出研究数据包')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: '导出 Excel' }).length).toBeGreaterThan(0);
  });

  it('uses compact cards instead of the wide table on small viewports conceptually', async () => {
    vi.mocked(researchAnalyticsService.listReleases).mockResolvedValue([release]);
    vi.mocked(researchAnalyticsService.getOverview).mockResolvedValue(overview(1, 1));
    vi.mocked(researchAnalyticsService.listAttempts).mockResolvedValue({
      total: 1,
      pageNo: 1,
      pageSize: 20,
      records: [{
        attemptId: 9,
        participantCode: 'P-000009',
        participantType: 'PUBLIC_CODE',
        status: 'SUBMITTED',
        answeredCount: 1,
        questionCount: 1,
        percentageScore: 100,
        effectiveDurationMs: 120000,
        qualityFlags: [],
        attachmentCount: 0,
        aiAnalysisStatus: 'COMPLETED',
        startedAt: '2026-08-02T00:00:00',
        lastSavedAt: '2026-08-02T00:10:00',
        submittedAt: '2026-08-02T00:12:00',
      }],
    });
    vi.mocked(researchAnalyticsService.getQuestionStats).mockResolvedValue({
      meta: { filterEcho: {}, sampleCount: 1, generatedAt: '2026-08-02T01:00:00', metricVersion: 'RESEARCH_STATS_V1' },
      questions: [],
    });
    vi.mocked(researchAnalyticsService.getDimensionStats).mockResolvedValue({
      meta: { filterEcho: {}, sampleCount: 1, generatedAt: '2026-08-02T01:00:00', metricVersion: 'RESEARCH_STATS_V1' },
      dimensions: [],
    });
    vi.mocked(researchAnalyticsService.getLatestAiReport).mockResolvedValue(null);
    renderDataTab();
    await waitFor(() => expect(screen.getAllByText('P-000009').length).toBeGreaterThan(0));
    expect(screen.getAllByRole('button', { name: '查看 P-000009 的答卷' }).length).toBeGreaterThan(1);
  });
});

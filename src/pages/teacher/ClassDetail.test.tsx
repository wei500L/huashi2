import type { ReactNode, RefObject } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { exportReportPagesToPdf } from '@/lib/pdf-report';
import { teacherAnalyticsService, teacherClassService } from '@/lib/services';
import TeacherClassDetailPage from './ClassDetail';

vi.mock('@/components/common', () => ({
  PageHeader: ({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {subtitle ? <p>{subtitle}</p> : null}
      <div>{actions}</div>
    </div>
  ),
  SectionEyebrow: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  StatCard: ({ title, value }: { title: string; value: string }) => (
    <div>
      <span>{title}</span>
      <span>{value}</span>
    </div>
  ),
  StatusBadge: ({ label }: { label: string }) => <span>{label}</span>,
}));

vi.mock('@/components/common/ChartCard', () => ({
  ChartCard: ({ title }: { title: string }) => <div>{title}</div>,
}));

vi.mock('@/components/analytics/AnalyticsPdfReport', () => ({
  ClassAnalyticsPdfReport: ({ reportRef }: { reportRef: RefObject<HTMLDivElement | null> }) => (
    <div ref={reportRef}>class-pdf-report</div>
  ),
}));

vi.mock('@/lib/pdf-report', () => ({
  exportReportPagesToPdf: vi.fn(),
}));

vi.mock('@/lib/services', () => ({
  teacherAnalyticsService: {
    getClassOverview: vi.fn(),
    getRiskDistribution: vi.fn(),
    getHeatmap: vi.fn(),
    getErrorDistribution: vi.fn(),
    getCompletionRate: vi.fn(),
    listStudents: vi.fn(),
    exportClassCsv: vi.fn(),
  },
  teacherClassService: {
    getDetail: vi.fn(),
    listStudentCandidates: vi.fn(),
    addStudents: vi.fn(),
    removeStudents: vi.fn(),
    archiveClass: vi.fn(),
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

function createDetail() {
  return {
    classId: 42,
    className: '高一 1 班',
    classCode: 'INVITE42',
    gradeName: '高一',
    studentCount: 0,
    createdAt: '2026-04-15T10:00:00',
    updatedAt: '2026-04-15T12:00:00',
    students: [],
  };
}

function createOverview() {
  return {
    className: '高一 1 班',
    classCode: 'INVITE42',
    studentCount: 0,
    activeStudentCount: 0,
    highRiskStudentCount: 0,
    primaryRiskLevel: 'LOW',
    cards: [],
    radar: [],
    latestSnapshot: {
      recentAccuracy: 0.91,
      recentAvgReactionTimeMs: 760,
      activeStudentCount: 0,
      recommendedFocusModes: [],
    },
  };
}

function createHeatmap() {
  return {
    xLabels: [],
    yLabels: [],
    cells: [],
  };
}

function createCompletionRate() {
  return {
    overallRate: 0.8,
    completedStudentCount: 0,
    studentCount: 0,
    trend: {
      labels: ['2026-04-01'],
      series: [],
    },
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
      <MemoryRouter initialEntries={['/teacher/classes/42']}>
        <Routes>
          <Route path="/teacher/classes/:classId" element={<TeacherClassDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('TeacherClassDetailPage PDF export', () => {
  beforeEach(() => {
    vi.mocked(teacherClassService.getDetail).mockResolvedValue(createDetail() as never);
    vi.mocked(teacherClassService.listStudentCandidates).mockResolvedValue([]);
    vi.mocked(teacherClassService.addStudents).mockResolvedValue(createDetail() as never);
    vi.mocked(teacherClassService.removeStudents).mockResolvedValue(createDetail() as never);
    vi.mocked(teacherClassService.archiveClass).mockResolvedValue(undefined);
    vi.mocked(teacherAnalyticsService.getClassOverview).mockResolvedValue(createOverview() as never);
    vi.mocked(teacherAnalyticsService.getRiskDistribution).mockResolvedValue([]);
    vi.mocked(teacherAnalyticsService.getHeatmap).mockResolvedValue(createHeatmap() as never);
    vi.mocked(teacherAnalyticsService.getErrorDistribution).mockResolvedValue([]);
    vi.mocked(teacherAnalyticsService.getCompletionRate).mockResolvedValue(createCompletionRate() as never);
    vi.mocked(teacherAnalyticsService.listStudents).mockResolvedValue([]);
    vi.mocked(teacherAnalyticsService.exportClassCsv).mockResolvedValue(new Blob());
    vi.mocked(exportReportPagesToPdf).mockResolvedValue(undefined);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    cleanup();
    queryClients.splice(0).forEach((client) => client.clear());
    vi.clearAllMocks();
  });

  it('waits for the hidden class report to mount before exporting PDF', async () => {
    renderPage();
    const user = userEvent.setup();

    const exportButton = await screen.findByRole('button', { name: /导出 PDF|Export PDF/ });
    await waitFor(() => expect(exportButton).toBeEnabled());

    await user.click(exportButton);

    await waitFor(() => {
      expect(exportReportPagesToPdf).toHaveBeenCalledTimes(1);
    });
    expect(vi.mocked(exportReportPagesToPdf).mock.calls[0][0]).toBeInstanceOf(HTMLDivElement);
    expect(vi.mocked(exportReportPagesToPdf).mock.calls[0][1]).toBe('class-42-analytics-report.pdf');
  });

  it('keeps PDF export disabled until analytics student data is loaded', async () => {
    const analyticsStudentsDeferred = createDeferred<[]>();
    vi.mocked(teacherAnalyticsService.listStudents).mockReturnValueOnce(analyticsStudentsDeferred.promise);

    renderPage();

    const exportButton = await screen.findByRole('button', { name: /导出 PDF|Export PDF/ });
    await waitFor(() => {
      expect(teacherClassService.getDetail).toHaveBeenCalled();
      expect(teacherAnalyticsService.getCompletionRate).toHaveBeenCalled();
    });
    expect(exportButton).toBeDisabled();

    analyticsStudentsDeferred.resolve([]);

    await waitFor(() => expect(exportButton).toBeEnabled());
  });
});

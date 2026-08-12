import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { researchAnalyticsService } from '@/lib/services';
import type { TeacherResearchAttemptDetailVO } from '@/lib/contracts';
import ResearchAttemptDetailPage from './ResearchAttemptDetail';

vi.mock('@/lib/services', async () => {
  const actual = await vi.importActual<typeof import('@/lib/services')>('@/lib/services');
  return {
    ...actual,
    researchAnalyticsService: {
      ...actual.researchAnalyticsService,
      getAttemptDetail: vi.fn(),
    },
  };
});

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

const detail: TeacherResearchAttemptDetailVO = {
  participant: { participantCode: 'P-000018', participantType: 'PUBLIC_CODE', consentedAt: '2026-08-12T05:17:30' },
  attempt: {
    attemptId: 21,
    publishId: 8,
    paperId: 3,
    paperTitle: 'Lexi-Bridge V1',
    status: 'SUBMITTED',
    answeredCount: 1,
    questionCount: 2,
    startedAt: '2026-08-12T05:17:30',
    lastSavedAt: '2026-08-12T05:40:08',
    submittedAt: '2026-08-12T05:40:08',
    submitReason: 'MANUAL',
  },
  result: {
    percentageScore: 80,
    qualityFlags: ['FAST_ITEM'],
  },
  ai: { status: 'FALLBACK' },
  questions: [
    {
      questionId: 1,
      questionOrder: 2,
      questionType: 'SHORT_TEXT',
      sectionTitle: 'BASIC_INFO',
      formalSection: false,
      stemText: '您的姓名：',
      options: [],
      responses: ['张三'],
      correctAnswers: [],
      attachments: [],
    },
    {
      questionId: 2,
      questionOrder: 10,
      questionType: 'SINGLE_CHOICE',
      sectionTitle: 'P1',
      formalSection: true,
      stemText: 'actual 的意思是？',
      options: [{ key: 'A', label: '实际的' }, { key: 'B', label: '当前的' }],
      responses: ['B'],
      correctAnswers: ['A'],
      correct: false,
      scoreAwarded: 0,
      questionScore: 1,
      attachments: [],
    },
  ],
};

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/teacher/research/attempts/21']}>
        <Routes>
          <Route path="/teacher/research/attempts/:attemptId" element={<ResearchAttemptDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('ResearchAttemptDetail', () => {
  it('splits profile and formal questions and uses Chinese quality labels', async () => {
    vi.mocked(researchAnalyticsService.getAttemptDetail).mockResolvedValue(detail);
    const user = userEvent.setup();
    renderPage();
    expect(await screen.findByText('P-000018')).toBeInTheDocument();
    expect(screen.getByText('过快作答')).toBeInTheDocument();
    expect(screen.getByText(/参与码进入/)).toBeInTheDocument();
    expect(screen.getAllByText('资料').length).toBeGreaterThan(0);
    expect(screen.getAllByText('正式题').length).toBeGreaterThan(0);
    expect(screen.getByText('您的姓名：')).toBeInTheDocument();
    expect(screen.getByText('actual 的意思是？')).toBeInTheDocument();
    expect(screen.getByText('学生选择')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '未答' }));
    expect(screen.getByText('当前筛选没有题目。')).toBeInTheDocument();
  });
});

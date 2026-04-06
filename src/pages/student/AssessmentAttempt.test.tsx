import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import type { AssessmentAttemptDetailVO } from '@/lib/contracts';
import { assessmentService } from '@/lib/services';
import AssessmentAttempt from './AssessmentAttempt';

let blockerState: 'unblocked' | 'blocked' = 'unblocked';
const blockerProceed = vi.fn();
const blockerReset = vi.fn();
const mockedUseBlocker = vi.fn(() => ({
  state: blockerState,
  proceed: blockerProceed,
  reset: blockerReset,
}));
const mockedUseBeforeUnload = vi.fn();

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return {
    ...actual,
    useBlocker: () => mockedUseBlocker(),
    useBeforeUnload: (...args: unknown[]) => mockedUseBeforeUnload(...args),
  };
});

vi.mock('@/components/common', () => ({
  PageHeader: ({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {subtitle ? <p>{subtitle}</p> : null}
      <div>{actions}</div>
    </div>
  ),
}));

vi.mock('@/lib/services', () => ({
  assessmentService: {
    getStudentAttempt: vi.fn(),
    saveStudentResponses: vi.fn(),
    saveStudentResponsesKeepalive: vi.fn(),
    submitStudentAttempt: vi.fn(),
  },
}));

const queryClients: QueryClient[] = [];

function createAttemptDetail(overrides: Partial<AssessmentAttemptDetailVO> = {}): AssessmentAttemptDetailVO {
  return {
    attemptId: 42,
    publishId: 9,
    paperId: 11,
    paperTitle: '测评作答',
    paperDescription: '测试用测评',
    className: '2024级英法迁移试点1班',
    status: 'IN_PROGRESS',
    instructionsText: '请认真作答',
    durationMinutes: 30,
    questionCount: 1,
    answeredCount: 0,
    totalScore: 10,
    startedAt: '2099-04-06T10:00:00',
    expiresAt: '2099-04-06T11:00:00',
    submittedAt: null,
    lastSavedAt: null,
    serverTime: '2099-04-06T10:10:00',
    questions: [
      {
        answerId: 501,
        questionId: 601,
        questionOrder: 1,
        questionType: 'FILL_BLANK',
        stemText: '请输入答案',
        promptText: '任写一个即可',
        options: [],
        score: 10,
        responses: [],
        answered: false,
      },
    ],
    ...overrides,
  };
}

function renderAttemptPage() {
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
      <MemoryRouter initialEntries={['/assessments/attempts/42']}>
        <Routes>
          <Route path="/assessments/attempts/:attemptId" element={<AssessmentAttempt />} />
          <Route path="/assessments" element={<div>assessment list</div>} />
          <Route path="/assessments/attempts/:attemptId/result" element={<div>assessment result</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('AssessmentAttempt', () => {
  beforeEach(() => {
    blockerState = 'unblocked';
    blockerProceed.mockReset();
    blockerReset.mockReset();
    blockerProceed.mockImplementation(() => {
      blockerState = 'unblocked';
    });
    blockerReset.mockImplementation(() => {
      blockerState = 'unblocked';
    });
    mockedUseBlocker.mockClear();
    mockedUseBeforeUnload.mockClear();
    vi.mocked(assessmentService.getStudentAttempt).mockResolvedValue(createAttemptDetail());
    vi.mocked(assessmentService.saveStudentResponses).mockResolvedValue({
      attemptId: 42,
      status: 'IN_PROGRESS',
      answeredCount: 1,
      lastSavedAt: '2026-04-06T10:12:00',
    });
    vi.mocked(assessmentService.saveStudentResponsesKeepalive).mockResolvedValue({
      attemptId: 42,
      status: 'IN_PROGRESS',
      answeredCount: 1,
      lastSavedAt: '2026-04-06T10:12:00',
    });
    vi.mocked(assessmentService.submitStudentAttempt).mockResolvedValue({
      attemptId: 42,
      status: 'SUBMITTED',
      submittedAt: '2026-04-06T10:13:00',
    });
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    cleanup();
    queryClients.splice(0).forEach((client) => client.clear());
    vi.clearAllMocks();
    vi.useRealTimers();
  });

  it('keeps the student on the attempt page when route-save fails', async () => {
    vi.mocked(assessmentService.saveStudentResponses).mockRejectedValue(new Error('network down'));

    const view = renderAttemptPage();
    expect(await screen.findByPlaceholderText('请输入答案')).toBeInTheDocument();

    blockerState = 'blocked';
    view.rerender(
      <QueryClientProvider client={queryClients[0]}>
        <MemoryRouter initialEntries={['/assessments/attempts/42']}>
          <Routes>
            <Route path="/assessments/attempts/:attemptId" element={<AssessmentAttempt />} />
            <Route path="/assessments" element={<div>assessment list</div>} />
            <Route path="/assessments/attempts/:attemptId/result" element={<div>assessment result</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(await screen.findByText('network down')).toBeInTheDocument();
    expect(blockerProceed).not.toHaveBeenCalled();
    expect(blockerReset).toHaveBeenCalled();
  });

  it('clears the submit lock when the final save fails', async () => {
    vi.mocked(assessmentService.submitStudentAttempt).mockRejectedValueOnce(new Error('submit failed'));

    renderAttemptPage();
    expect(await screen.findByPlaceholderText('请输入答案')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '交卷' }));

    expect(await screen.findByText('submit failed')).toBeInTheDocument();
    await waitFor(() => {
      expect(assessmentService.saveStudentResponses).toHaveBeenCalled();
      expect(assessmentService.submitStudentAttempt).toHaveBeenCalled();
      expect(screen.getByPlaceholderText('请输入答案')).not.toBeDisabled();
      expect(screen.getByRole('button', { name: '交卷' })).not.toBeDisabled();
    });
  });
});

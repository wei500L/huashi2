import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import type {
  PublicAssessmentAttemptVO,
  PublicAssessmentMetadataVO,
  PublicAssessmentResultVO,
} from '@/lib/contracts';
import { publicAssessmentService } from '@/lib/services';
import { rememberPublicSession } from './research-session';
import ResearchParticipantPage from './index';

vi.mock('@/lib/services', () => ({
  publicAssessmentService: {
    getMetadata: vi.fn(),
    getAttempt: vi.fn(),
    saveResponses: vi.fn(),
    submit: vi.fn(),
    getResult: vi.fn(),
    recordTiming: vi.fn(),
  },
}));

const metadata: PublicAssessmentMetadataVO = {
  releaseCode: 'RES-TEST',
  title: '研究问卷',
  description: null,
  instructionsText: null,
  durationMinutes: 60,
  questionCount: 1,
  formalQuestionCount: 1,
  profileFieldCount: 0,
  status: 'PUBLISHED',
  startsAt: null,
  dueAt: null,
  qrEntryEnabled: false,
  maxAttempts: 1,
};

const attempt: PublicAssessmentAttemptVO = {
  attemptId: 42,
  releaseCode: 'RES-TEST',
  paperTitle: '研究问卷',
  paperDescription: null,
  instructionsText: null,
  status: 'IN_PROGRESS',
  durationMinutes: 60,
  questionCount: 1,
  answeredCount: 0,
  startedAt: '2030-01-01T00:00:00Z',
  answeringStartedAt: '2030-01-01T00:00:01Z',
  expiresAt: '2099-01-01T01:00:00Z',
  lastSavedAt: null,
  version: 1,
  serverTime: '2030-01-01T00:00:02Z',
  questions: [{
    questionId: 1,
    questionOrder: 1,
    questionType: 'SINGLE_CHOICE',
    sectionCode: 'FORMAL',
    sectionTitle: '正式题',
    formalSection: true,
    stemText: '链路是否正常？',
    promptText: null,
    options: [
      { key: 'A', label: '正常' },
      { key: 'B', label: '异常' },
    ],
    required: true,
    justificationRequired: false,
    responses: [],
    justificationText: null,
  }],
};

const result: PublicAssessmentResultVO = {
  attemptId: 42,
  releaseCode: 'RES-TEST',
  paperTitle: '研究问卷',
  status: 'SUBMITTED',
  questionCount: 1,
  answeredCount: 1,
  objectiveScore: 10,
  totalScore: 10,
  submittedAt: '2030-01-01T00:01:00Z',
  scoreVisible: true,
  qualityFlags: [],
  aiAnalysisStatus: 'FAILED',
  aiAnalysis: null,
  questions: [],
};

describe('public assessment submission autosave boundary', () => {
  beforeEach(() => {
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: vi.fn(),
    });
  });

  afterEach(() => {
    window.localStorage.clear();
    delete (HTMLElement.prototype as Partial<HTMLElement>).scrollIntoView;
    vi.clearAllMocks();
  });

  it('does not dispatch a queued autosave after submission starts', async () => {
    let resolveSubmit!: () => void;
    const pendingSubmit = new Promise<void>((resolve) => {
      resolveSubmit = resolve;
    });
    vi.mocked(publicAssessmentService.getMetadata).mockResolvedValue(metadata);
    vi.mocked(publicAssessmentService.getAttempt).mockResolvedValue(attempt);
    vi.mocked(publicAssessmentService.saveResponses).mockResolvedValue({
      attemptId: 42,
      status: 'IN_PROGRESS',
      answeredCount: 1,
      lastSavedAt: '2030-01-01T00:00:03Z',
      version: 2,
    });
    vi.mocked(publicAssessmentService.submit).mockImplementation(async () => {
      await pendingSubmit;
      return {
        attemptId: 42,
        status: 'SUBMITTED',
        submittedAt: '2030-01-01T00:01:00Z',
        version: 2,
      };
    });
    vi.mocked(publicAssessmentService.getResult).mockResolvedValue(result);
    vi.mocked(publicAssessmentService.recordTiming).mockResolvedValue(undefined);
    rememberPublicSession('RES-TEST');

    render(
      <MemoryRouter initialEntries={['/research/RES-TEST']}>
        <Routes>
          <Route path="/research/:releaseCode" element={<ResearchParticipantPage />} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole('radio', { name: '正常' }));
    fireEvent.click(screen.getByRole('button', { name: /提交问卷/ }));
    await waitFor(() => expect(publicAssessmentService.submit).toHaveBeenCalledTimes(1));

    await act(async () => {
      await new Promise((resolve) => window.setTimeout(resolve, 1_050));
    });
    expect(publicAssessmentService.saveResponses).not.toHaveBeenCalled();

    await act(async () => resolveSubmit());
    expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('你的迁移路径，已经被记录。');
  });
});

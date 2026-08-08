import type { ReactNode, RefObject } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { exportReportPagesToPdf } from '@/lib/pdf-report';
import { aiService, diagnosisSessionService, diagnosisTemplateService, trainingService } from '@/lib/services';
import DiagnosisPage from './index';

vi.mock('./flow', () => ({
  initialDiagnosisFlowState: {
    phase: 'result',
    sessionId: 42,
  },
  diagnosisFlowReducer: (state: unknown) => state,
}));

vi.mock('@/components/common', () => ({
  PageHeader: ({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {subtitle ? <p>{subtitle}</p> : null}
      <div>{actions}</div>
    </div>
  ),
  PanelSkeleton: ({ className }: { className?: string }) => <div className={className}>loading</div>,
  SectionEyebrow: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  StatusBadge: ({ label }: { label: string }) => <div>{label}</div>,
}));

vi.mock('@/components/common/EChart', () => ({
  EChart: () => <div>chart</div>,
}));

vi.mock('@/components/diagnosis/DiagnosisPdfReport', () => ({
  DiagnosisPdfReport: ({
    reportRef,
    explanationErrorMessage,
  }: {
    reportRef: RefObject<HTMLDivElement | null>;
    explanationErrorMessage?: string | null;
  }) => <div ref={reportRef}>{explanationErrorMessage || 'diagnosis-pdf-report'}</div>,
}));

vi.mock('@/features/session-runtime/components', () => ({
  SessionFeedbackBanners: () => null,
  SessionOptionButton: ({ label }: { label: string }) => <button type="button">{label}</button>,
  SessionProgressHeader: () => null,
  SessionSaveActions: () => null,
}));

vi.mock('@/features/session-runtime/useSessionRuntime', () => ({
  useSessionRuntime: () => ({
    isSaving: false,
    saveMessage: null,
    saveErrorMessage: null,
    resetFeedback: vi.fn(),
    saveProgressManually: vi.fn(),
  }),
}));

vi.mock('@/lib/pdf-report', () => ({
  exportReportPagesToPdf: vi.fn(),
}));

vi.mock('@/lib/services', () => ({
  aiService: {
    explainDiagnosis: vi.fn(),
    explainDiagnosisAsync: vi.fn(),
  },
  diagnosisSessionService: {
    listHistory: vi.fn(),
    getResult: vi.fn(),
  },
  diagnosisTemplateService: {
    listPublished: vi.fn(),
  },
  trainingService: {
    getRecommendedPlan: vi.fn(),
  },
}));

const queryClients: QueryClient[] = [];

function createResult() {
  return {
    summaryId: 301,
    sessionId: 42,
    status: 'COMPLETED',
    templateId: 12,
    templateName: '法语迁移诊断',
    ownerUserId: 7,
    totalItems: 2,
    answeredItems: 2,
    startedAt: '2026-04-15T08:00:00Z',
    completedAt: '2026-04-15T08:12:00Z',
    metrics: {
      positiveTransferScore: 0.64,
      negativeTransferRisk: 0.31,
      contextSensitivity: 0.55,
      semanticDiscrimination: 0.72,
      overallAccuracy: 0.5,
      averageReactionTime: 912,
    },
    errorTypeDistribution: [],
    highRiskLexicalPairs: [
      {
        lexicalPairId: 1,
        englishWord: 'actual',
        frenchWord: 'actuel',
        lexicalPairType: 'FALSE_FRIEND',
        riskScore: 0.74,
        errorCount: 1,
        averageReactionTime: 920,
        dominantErrorType: 'FALSE_FRIEND_BIAS',
      },
    ],
    chartPayload: {
      radarMetrics: [
        {
          code: 'positiveTransferScore',
          label: 'Positive Transfer',
          value: 0.64,
        },
      ],
      errorTypeDistribution: [],
      contextPerformance: [],
      lexicalTypePerformance: [],
      topRiskPairs: [],
      responseTimeline: [],
    },
    items: [
      {
        itemResultId: 11,
        templateItemId: 101,
        presentationOrder: 1,
        taskType: 'REACTION_TIME',
        lexicalPairId: 1,
        englishWord: 'actual',
        frenchWord: 'actuel',
        chineseGloss: '实际的',
        lexicalPairType: 'FALSE_FRIEND',
        contextSupportLevel: 'HIGH',
        expectedSemanticMatch: false,
        stimulus: {
          instruction: '选出正确词义',
          promptText: 'The actual problem is hidden.',
          contextSentence: 'Actual does not mean current here.',
        },
        options: [
          { key: 'A', label: '当前的' },
          { key: 'B', label: '实际的' },
        ],
        correctAnswerKey: 'B',
        selectedAnswerKey: 'A',
        reactionTimeMs: 920,
        hesitationTimeMs: 120,
        correct: false,
        semanticConsistent: false,
        detectedErrorType: 'FALSE_FRIEND_BIAS',
        transferRiskScore: 0.74,
        itemScore: 0,
      },
      {
        itemResultId: 12,
        templateItemId: 102,
        presentationOrder: 2,
        taskType: 'MULTIPLE_CHOICE',
        lexicalPairId: 2,
        englishWord: 'library',
        frenchWord: 'librairie',
        chineseGloss: '图书馆',
        lexicalPairType: 'FALSE_FRIEND',
        contextSupportLevel: 'MEDIUM',
        expectedSemanticMatch: false,
        stimulus: {
          instruction: '选择词义',
          promptText: 'She went to the library.',
          contextSentence: '',
        },
        options: [
          { key: 'A', label: '书店' },
          { key: 'B', label: '图书馆' },
        ],
        correctAnswerKey: 'B',
        selectedAnswerKey: 'B',
        reactionTimeMs: 880,
        hesitationTimeMs: 80,
        correct: true,
        semanticConsistent: true,
        detectedErrorType: 'NONE',
        transferRiskScore: 0.18,
        itemScore: 1,
      },
    ],
  };
}

function createExplanation() {
  return {
    requestId: 'req-1',
    generationSource: 'llm',
    promptVersion: 'v1',
    model: 'gpt-test',
    latencyMs: 320,
    recommendationPath: [],
    focusLexicalPairs: [],
    recommendedTrainingModes: [],
    explanation: '学生主要受假朋友词影响。',
    teacherNote: '讲评时重点区分形近词与义项差异。',
    diagnosisInsight: {
      strengths: ['能在中等语境支持下保持较高辨析度'],
      weaknesses: ['高相似假朋友词仍然容易误判'],
      suggestions: ['先进行假朋友词专项训练'],
    },
    confidence: 0.78,
    fallbackReason: null,
  };
}

function createDeferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolver, rejecter) => {
    resolve = resolver;
    reject = rejecter;
  });
  return { promise, resolve, reject };
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
        <DiagnosisPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('DiagnosisPage PDF export', () => {
  beforeEach(() => {
    vi.mocked(diagnosisSessionService.listHistory).mockResolvedValue({ records: [] } as never);
    vi.mocked(diagnosisSessionService.getResult).mockResolvedValue(createResult() as never);
    vi.mocked(aiService.explainDiagnosisAsync).mockResolvedValue(createExplanation() as never);
    vi.mocked(diagnosisTemplateService.listPublished).mockResolvedValue({ records: [] } as never);
    vi.mocked(trainingService.getRecommendedPlan).mockResolvedValue({ suggestedSessions: [], priorityMode: null } as never);
    vi.mocked(exportReportPagesToPdf).mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
    queryClients.splice(0).forEach((client) => client.clear());
    vi.clearAllMocks();
  });

  it('waits for the hidden diagnosis report to mount before exporting PDF', async () => {
    renderPage();
    const user = userEvent.setup();

    const exportButton = await screen.findByRole('button', { name: /导出 PDF|Export PDF/ });
    await waitFor(() => expect(exportButton).toBeEnabled());

    await user.click(exportButton);

    await waitFor(() => {
      expect(exportReportPagesToPdf).toHaveBeenCalledTimes(1);
    });
    expect(vi.mocked(exportReportPagesToPdf).mock.calls[0][0]).toBeInstanceOf(HTMLDivElement);
    expect(vi.mocked(exportReportPagesToPdf).mock.calls[0][1]).toBe('diagnosis-session-42-report.pdf');
  });

  it('keeps PDF export disabled until explanation data is loaded', async () => {
    const explanationDeferred = createDeferred<ReturnType<typeof createExplanation>>();
    vi.mocked(aiService.explainDiagnosisAsync).mockReturnValueOnce(explanationDeferred.promise as never);

    renderPage();

    const exportButton = await screen.findByRole('button', { name: /导出 PDF|Export PDF/ });
    await waitFor(() => expect(diagnosisSessionService.getResult).toHaveBeenCalled());
    expect(exportButton).toBeDisabled();

    explanationDeferred.resolve(createExplanation());

    await waitFor(() => expect(exportButton).toBeEnabled());
  });

  it('allows PDF export when explanation loading fails', async () => {
    vi.mocked(aiService.explainDiagnosisAsync).mockRejectedValueOnce(new Error('AI explain failed'));
    renderPage();
    const user = userEvent.setup();

    const exportButton = await screen.findByRole('button', { name: /导出 PDF|Export PDF/ });
    await waitFor(() => expect(screen.getByText('AI explain failed')).toBeInTheDocument());
    await waitFor(() => expect(exportButton).toBeEnabled());

    await user.click(exportButton);

    await waitFor(() => {
      expect(exportReportPagesToPdf).toHaveBeenCalledTimes(1);
    });
  });
});

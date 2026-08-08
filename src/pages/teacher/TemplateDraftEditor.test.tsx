import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import type {
  DiagnosisTemplateDetailVO,
  DiagnosisTemplateDraftDetailVO,
  DiagnosisTemplateDraftSaveRequest,
  DiagnosisTemplateDraftValidationResponseVO,
  LexicalPairDetailVO,
  LexicalPairSummaryVO,
  PageResult,
} from '@/lib/contracts';
import { diagnosisTemplateService, lexicalPairService, teacherAnalyticsService } from '@/lib/services';
import TemplateDraftEditor from './TemplateDraftEditor';

vi.mock('@/components/common', () => ({
  PageHeader: ({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {subtitle ? <p>{subtitle}</p> : null}
      <div>{actions}</div>
    </div>
  ),
  SectionEyebrow: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  WorkflowStepper: ({ title }: { title?: string }) => <div data-testid="workflow-stepper">{title}</div>,
}));

vi.mock('@/lib/services', () => ({
  diagnosisTemplateService: {
    getDraft: vi.fn(),
    saveDraft: vi.fn(),
    validateDraft: vi.fn(),
    publishDraft: vi.fn(),
  },
  lexicalPairService: {
    getDetail: vi.fn(),
    pageQuery: vi.fn(),
  },
  teacherAnalyticsService: {
    listClasses: vi.fn(),
  },
}));

const queryClients: QueryClient[] = [];
const emptyPairPage: PageResult<LexicalPairSummaryVO> = {
  total: 0,
  pageNo: 1,
  pageSize: 8,
  records: [],
};
const unpublishedTemplate: DiagnosisTemplateDetailVO = {
  id: 91,
  templateName: 'Published Draft',
  description: '',
  status: 'PUBLISHED',
  targetClassId: null,
  targetClassName: null,
  estimatedDurationMinutes: 10,
  scoringVersion: 'RULE_V1',
  itemCount: 0,
  shareScope: 'PRIVATE',
  ownerUserId: 7,
  ownerDisplayName: 'Teacher Zhang',
  createdAt: '2026-03-29T10:00:00',
  updatedAt: '2026-03-29T10:10:00',
  items: [],
};
const unusedPairDetail = {
  id: 0,
  englishWord: '',
  frenchWord: '',
  chineseGloss: '',
  lexicalPairType: 'FALSE_FRIEND',
  semanticOverlapScore: 0,
  falseFriendRisk: 0,
  riskLevel: 'LOW',
  defaultContextSupport: 'LOW',
  difficultyLevel: 1,
  source: null,
  active: true,
  knowledgeStatus: 'READY',
  embeddingStatus: 'EMBEDDED',
  lastEmbeddedAt: null,
  tags: [],
  senses: [],
} satisfies LexicalPairDetailVO;

function createDraftDetail(
  overrides: Partial<DiagnosisTemplateDraftDetailVO> = {}
): DiagnosisTemplateDraftDetailVO {
  return {
    draftId: 42,
    sourceTemplateId: null,
    publishedTemplateId: null,
    syncState: 'DIRTY',
    version: 1,
    schema: {
      basic: {
        templateName: 'Base Draft',
        description: '',
        publishTarget: 'SELF',
        estimatedDurationMinutes: 10,
        targetClassId: null,
        shareScope: 'PRIVATE',
        scoringVersion: 'RULE_V1',
      },
      items: [],
    },
    createdAt: '2026-03-29T10:00:00',
    updatedAt: '2026-03-29T10:00:00',
    ...overrides,
  };
}

function renderEditor() {
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
      <MemoryRouter initialEntries={['/teacher/diagnosis-template-drafts/42?step=1']}>
        <Routes>
          <Route path="/teacher/diagnosis-template-drafts/:draftId" element={<TemplateDraftEditor />} />
          <Route path="/teacher/diagnosis-templates" element={<div>templates list</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('TemplateDraftEditor', () => {
  beforeEach(() => {
    vi.mocked(diagnosisTemplateService.getDraft).mockResolvedValue(createDraftDetail());
    vi.mocked(diagnosisTemplateService.saveDraft).mockImplementation(
      async (_draftId: number, payload: DiagnosisTemplateDraftSaveRequest) =>
        createDraftDetail({
          version: payload.version + 1,
          schema: payload.schema as DiagnosisTemplateDraftDetailVO['schema'],
          updatedAt: '2026-03-29T10:05:00',
        })
    );
    vi.mocked(diagnosisTemplateService.validateDraft).mockResolvedValue({
      valid: true,
      fieldErrors: {},
      itemErrors: [],
      blockingSteps: [],
    } satisfies DiagnosisTemplateDraftValidationResponseVO);
    vi.mocked(diagnosisTemplateService.publishDraft).mockResolvedValue(unpublishedTemplate);
    vi.mocked(lexicalPairService.getDetail).mockResolvedValue(unusedPairDetail);
    vi.mocked(lexicalPairService.pageQuery).mockResolvedValue(emptyPairPage);
    vi.mocked(teacherAnalyticsService.listClasses).mockResolvedValue([
      {
        classId: 1,
        classCode: 'CLS-0001',
        className: '2024级英法迁移试点1班',
        gradeName: 'Pilot Grade',
        studentCount: 2,
      },
    ]);
  });

  afterEach(() => {
    cleanup();
    queryClients.splice(0).forEach((client) => client.clear());
    vi.clearAllMocks();
  });

  it('saves the latest in-memory schema before validating', async () => {
    renderEditor();
    const user = userEvent.setup();

    const templateNameInput = await screen.findByLabelText('模板名称');
    await user.clear(templateNameInput);
    await user.type(templateNameInput, 'Edited Draft');
    await user.click(screen.getByRole('button', { name: '校验' }));

    await waitFor(() => {
      expect(diagnosisTemplateService.saveDraft).toHaveBeenCalledTimes(1);
      expect(diagnosisTemplateService.validateDraft).toHaveBeenCalledTimes(1);
    });

    const savePayload = vi.mocked(diagnosisTemplateService.saveDraft).mock.calls[0][1];
    expect(savePayload.schema.basic.templateName).toBe('Edited Draft');
    expect(vi.mocked(diagnosisTemplateService.saveDraft).mock.invocationCallOrder[0]).toBeLessThan(
      vi.mocked(diagnosisTemplateService.validateDraft).mock.invocationCallOrder[0]
    );
  });

  it('saves the latest in-memory schema before publishing', async () => {
    renderEditor();
    const user = userEvent.setup();

    const templateNameInput = await screen.findByLabelText('模板名称');
    await user.clear(templateNameInput);
    await user.type(templateNameInput, 'Publish Ready Draft');
    await user.click(screen.getByRole('button', { name: '发布模板' }));

    await waitFor(() => {
      expect(diagnosisTemplateService.saveDraft).toHaveBeenCalledTimes(1);
      expect(diagnosisTemplateService.validateDraft).toHaveBeenCalledTimes(1);
      expect(diagnosisTemplateService.publishDraft).toHaveBeenCalledTimes(1);
    });

    const savePayload = vi.mocked(diagnosisTemplateService.saveDraft).mock.calls[0][1];
    expect(savePayload.schema.basic.templateName).toBe('Publish Ready Draft');
    expect(vi.mocked(diagnosisTemplateService.saveDraft).mock.invocationCallOrder[0]).toBeLessThan(
      vi.mocked(diagnosisTemplateService.validateDraft).mock.invocationCallOrder[0]
    );
    expect(vi.mocked(diagnosisTemplateService.validateDraft).mock.invocationCallOrder[0]).toBeLessThan(
      vi.mocked(diagnosisTemplateService.publishDraft).mock.invocationCallOrder[0]
    );
  });

  it('stops before validate and publish when saving fails', async () => {
    vi.mocked(diagnosisTemplateService.saveDraft).mockRejectedValueOnce(new Error('save exploded'));

    renderEditor();
    const user = userEvent.setup();

    const templateNameInput = await screen.findByLabelText('模板名称');
    await user.clear(templateNameInput);
    await user.type(templateNameInput, 'Broken Draft');
    await user.click(screen.getByRole('button', { name: '发布模板' }));

    await waitFor(() => {
      expect(diagnosisTemplateService.saveDraft).toHaveBeenCalledTimes(1);
    });

    expect(diagnosisTemplateService.validateDraft).not.toHaveBeenCalled();
    expect(diagnosisTemplateService.publishDraft).not.toHaveBeenCalled();
    expect(await screen.findByText('save exploded')).toBeInTheDocument();
  });
});

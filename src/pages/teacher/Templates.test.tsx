import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import type { DiagnosisTemplateDetailVO, DiagnosisTemplateDraftDetailVO, DiagnosisTemplateSummaryVO, PageResult } from '@/lib/contracts';
import { diagnosisTemplateService } from '@/lib/services';
import TeacherTemplatesPage from './Templates';

vi.mock('@/components/common', () => ({
  PageHeader: ({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) => (
    <div>
      <h1>{title}</h1>
      {subtitle ? <p>{subtitle}</p> : null}
      <div>{actions}</div>
    </div>
  ),
  SectionEyebrow: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  StatusBadge: ({ label }: { label: string }) => <span>{label}</span>,
}));

vi.mock('@/lib/services', () => ({
  diagnosisTemplateService: {
    listDrafts: vi.fn(),
    listTeacherTemplates: vi.fn(),
    listMarketTemplates: vi.fn(),
    createDraft: vi.fn(),
    createDraftFromTemplate: vi.fn(),
    deleteDraft: vi.fn(),
    updateTeacherTemplateSharing: vi.fn(),
  },
}));

const queryClients: QueryClient[] = [];

const draftPage: PageResult<{ draftId: number; templateName: string; description?: string | null; syncState: string; version: number; updatedAt: string; publishedTemplateId?: number | null }> = {
  total: 0,
  pageNo: 1,
  pageSize: 50,
  records: [],
};

const templatePage: PageResult<DiagnosisTemplateSummaryVO> = {
  total: 1,
  pageNo: 1,
  pageSize: 50,
  records: [
    {
      id: 18,
      templateName: '风险模板 A',
      description: '自建模板',
      status: 'PUBLISHED',
      targetClassId: null,
      targetClassName: null,
      itemCount: 6,
      estimatedDurationMinutes: 12,
      scoringVersion: 'RULE_V1',
      shareScope: 'PRIVATE',
      ownerUserId: 7,
      ownerDisplayName: '张老师',
      updatedAt: '2026-04-15T10:00:00',
    },
  ],
};

const marketPage: PageResult<DiagnosisTemplateSummaryVO> = {
  total: 1,
  pageNo: 1,
  pageSize: 50,
  records: [
    {
      id: 19,
      templateName: '公开模板 B',
      description: '市场模板',
      status: 'PUBLISHED',
      targetClassId: null,
      targetClassName: null,
      itemCount: 8,
      estimatedDurationMinutes: 15,
      scoringVersion: 'RULE_V1',
      shareScope: 'PUBLIC',
      ownerUserId: 9,
      ownerDisplayName: '李老师',
      updatedAt: '2026-04-15T09:30:00',
    },
  ],
};

const updatedTemplate: DiagnosisTemplateDetailVO = {
  id: 18,
  templateName: '风险模板 A',
  description: '自建模板',
  status: 'PUBLISHED',
  targetClassId: null,
  targetClassName: null,
  estimatedDurationMinutes: 12,
  scoringVersion: 'RULE_V1',
  itemCount: 6,
  shareScope: 'PUBLIC',
  ownerUserId: 7,
  ownerDisplayName: '张老师',
  createdAt: '2026-04-15T09:00:00',
  updatedAt: '2026-04-15T10:10:00',
  items: [],
};

const createdDraft: DiagnosisTemplateDraftDetailVO = {
  draftId: 91,
  sourceTemplateId: 19,
  publishedTemplateId: null,
  syncState: 'DIRTY',
  version: 1,
  schema: {
    basic: {
      templateName: '公开模板 B',
      description: '市场模板',
      publishTarget: 'SELF',
      estimatedDurationMinutes: 15,
      targetClassId: null,
      shareScope: 'PRIVATE',
      scoringVersion: 'RULE_V1',
    },
    items: [],
  },
  createdAt: '2026-04-15T09:40:00',
  updatedAt: '2026-04-15T09:40:00',
};

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
        <TeacherTemplatesPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('TeacherTemplatesPage', () => {
  beforeEach(() => {
    vi.mocked(diagnosisTemplateService.listDrafts).mockResolvedValue(draftPage);
    vi.mocked(diagnosisTemplateService.listTeacherTemplates).mockResolvedValue(templatePage);
    vi.mocked(diagnosisTemplateService.listMarketTemplates).mockResolvedValue(marketPage);
    vi.mocked(diagnosisTemplateService.createDraft).mockResolvedValue(createdDraft);
    vi.mocked(diagnosisTemplateService.createDraftFromTemplate).mockResolvedValue(createdDraft);
    vi.mocked(diagnosisTemplateService.deleteDraft).mockResolvedValue();
    vi.mocked(diagnosisTemplateService.updateTeacherTemplateSharing).mockResolvedValue(updatedTemplate);
  });

  afterEach(() => {
    cleanup();
    queryClients.splice(0).forEach((client) => client.clear());
    vi.clearAllMocks();
  });

  it('renders market templates and updates sharing state', async () => {
    renderPage();
    const user = userEvent.setup();

    expect((await screen.findAllByText('模板市场')).length).toBeGreaterThan(0);
    expect(await screen.findByText('公开模板 B')).toBeInTheDocument();
    expect(await screen.findByText('作者：李老师')).toBeInTheDocument();

    await user.click(await screen.findByRole('button', { name: '共享给其他教师' }));

    await waitFor(() => {
      expect(diagnosisTemplateService.updateTeacherTemplateSharing).toHaveBeenCalledWith(18, { shareScope: 'PUBLIC' });
    });
  });
});

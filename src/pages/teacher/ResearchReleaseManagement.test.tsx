import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { PublicAssessmentReleaseSummaryVO } from '@/lib/contracts';
import { ResearchReleaseManagement } from './ResearchReleaseManagement';

const mocks = vi.hoisted(() => ({
  listPublicReleases: vi.fn(),
  listParticipationCodes: vi.fn(),
  createParticipationCodeBatch: vi.fn(),
  revokeParticipationCode: vi.fn(),
  revokeParticipationCodeBatch: vi.fn(),
  updatePublicRelease: vi.fn(),
  toDataURL: vi.fn(),
}));

vi.mock('@/lib/services', () => ({
  assessmentService: {
    listPublicReleases: mocks.listPublicReleases,
    listParticipationCodes: mocks.listParticipationCodes,
    createParticipationCodeBatch: mocks.createParticipationCodeBatch,
    revokeParticipationCode: mocks.revokeParticipationCode,
    revokeParticipationCodeBatch: mocks.revokeParticipationCodeBatch,
    updatePublicRelease: mocks.updatePublicRelease,
  },
}));

vi.mock('qrcode', () => ({
  default: { toDataURL: mocks.toDataURL },
}));

const release: PublicAssessmentReleaseSummaryVO = {
  publishId: 5,
  paperId: 1,
  paperCode: 'LEXIBRIDGE_RESEARCH_V1',
  paperTitle: '语言迁移研究问卷',
  releaseCode: 'RES-TEST123',
  status: 'OPEN',
  publishedAt: '2026-08-09T00:00:00',
  qrEntryEnabled: false,
  codeCount: 1,
  unusedCount: 1,
  inProgressCount: 0,
  submittedCount: 0,
  revokedCount: 0,
  qrParticipantCount: 0,
  batches: [],
};

function renderManagement() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <ResearchReleaseManagement />
      </QueryClientProvider>
    </MemoryRouter>
  );
}

afterEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});

describe('research release participation management', () => {
  it('protects one-time codes and confirms QR and revoke actions', async () => {
    mocks.listPublicReleases.mockResolvedValue([release]);
    mocks.listParticipationCodes.mockResolvedValue({
      total: 1,
      pageNo: 1,
      pageSize: 20,
      records: [{ codeId: 11, codeHint: 'PJ57', status: 'UNUSED' }],
    });
    mocks.createParticipationCodeBatch.mockResolvedValue({
      batchId: 'batch-1',
      generatedAt: '2026-08-09T00:00:00',
      participationCodes: ['AAAA-BBBB-CCCC', 'DDDD-EEEE-FFFF'],
    });
    mocks.updatePublicRelease.mockResolvedValue({ ...release, qrEntryEnabled: true });
    mocks.revokeParticipationCode.mockResolvedValue({ revokedCount: 1 });
    mocks.toDataURL.mockResolvedValue('data:image/png;base64,qr');
    const anchorClick = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    const user = userEvent.setup();

    renderManagement();

    await screen.findByText('语言迁移研究问卷');
    await waitFor(() => expect(mocks.toDataURL).toHaveBeenCalledWith(
      expect.stringContaining('/research/RES-TEST123?entry=qr'),
      expect.objectContaining({ width: 360 })
    ));

    await user.click(screen.getByRole('button', { name: '下载 PNG' }));
    expect(anchorClick).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: '生成新批次' }));
    await waitFor(() => expect(mocks.createParticipationCodeBatch).toHaveBeenCalledWith(5, 20));
    await screen.findByText('已生成 2 个参与码。明文仅在本页本次显示，请立即复制或下载。');
    expect(screen.getByRole('textbox')).toHaveValue('AAAA-BBBB-CCCC\nDDDD-EEEE-FFFF');
    expect(screen.getByRole('button', { name: '请先保存当前批次' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: '已保存，清除明文' }));
    await user.click(screen.getByRole('button', { name: '已保存，确认清除' }));
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '生成新批次' })).toBeEnabled();

    await user.click(screen.getByRole('button', { name: '开启免码' }));
    await user.click(screen.getByRole('button', { name: '确认开启' }));
    await waitFor(() => expect(mocks.updatePublicRelease).toHaveBeenCalledWith(5, true));

    await user.click(await screen.findByRole('button', { name: '停用' }));
    await user.click(screen.getByRole('button', { name: '确认停用' }));
    await waitFor(() => expect(mocks.revokeParticipationCode).toHaveBeenCalledWith(5, 11));
  });
});

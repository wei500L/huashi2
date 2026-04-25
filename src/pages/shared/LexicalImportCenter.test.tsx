import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import type {
  LexicalImportBatchCreatedVO,
  LexicalImportBatchDetailVO,
  LexicalImportBatchSummaryVO,
  LexicalImportRowVO,
  PageResult,
} from '@/lib/contracts';
import { adminService, lexicalPairService } from '@/lib/services';
import LexicalImportCenter from './LexicalImportCenter';

vi.mock('@/lib/services', () => ({
  adminService: {
    listUsers: vi.fn(),
    getRagReindexJob: vi.fn(),
  },
  lexicalPairService: {
    createImportBatch: vi.fn(),
    listImportBatches: vi.fn(),
    getImportBatch: vi.fn(),
    listImportRows: vi.fn(),
    updateImportRow: vi.fn(),
    commitImportBatch: vi.fn(),
    reindexImportBatch: vi.fn(),
    downloadImportFile: vi.fn(),
  },
}));

const batchSummary: LexicalImportBatchSummaryVO = {
  id: 41,
  status: 'DRAFT',
  sourceFormat: 'CSV',
  originalFilename: 'batch-draft.csv',
  contentType: 'text/csv',
  fileSizeBytes: 2048,
  totalRows: 1,
  readyRows: 0,
  invalidRows: 1,
  skippedRows: 0,
  importedRows: 0,
  pendingEmbeddingCount: 0,
  embeddedCount: 0,
  failedEmbeddingCount: 0,
  errorMessage: null,
  ownerUserId: 7,
  ownerDisplayName: 'Teacher Zhang',
  createdAt: '2026-03-22T10:00:00',
  updatedAt: '2026-03-22T10:00:00',
  parserJobFinishedAt: '2026-03-22T10:00:05',
  importJobFinishedAt: null,
};

const batchDetail: LexicalImportBatchDetailVO = {
  id: 41,
  status: 'DRAFT',
  sourceFormat: 'CSV',
  originalFilename: 'batch-draft.csv',
  contentType: 'text/csv',
  fileSizeBytes: 2048,
  totalRows: 1,
  readyRows: 0,
  invalidRows: 1,
  skippedRows: 0,
  importedRows: 0,
  pendingEmbeddingCount: 0,
  embeddedCount: 0,
  failedEmbeddingCount: 0,
  errorMessage: null,
  ownerUserId: 7,
  ownerDisplayName: 'Teacher Zhang',
  fileSha256: 'abc123',
  parserJobStartedAt: '2026-03-22T10:00:01',
  parserJobFinishedAt: '2026-03-22T10:00:05',
  importJobStartedAt: null,
  importJobFinishedAt: null,
  latestEmbeddedAt: null,
  createdAt: '2026-03-22T10:00:00',
  updatedAt: '2026-03-22T10:00:05',
};

const invalidRow: LexicalImportRowVO = {
  id: 501,
  rowNumber: 2,
  status: 'INVALID',
  draft: {
    englishWord: 'lexicalalpha',
    frenchWord: 'lexicalalpha',
    chineseGloss: '原始中文释义',
    lexicalPairType: 'FALSE_FRIEND',
    semanticOverlapScore: '0.42',
    falseFriendRisk: '0.81',
    defaultContextSupport: 'HIGH',
    difficultyLevel: '4',
    notes: 'needs review',
    source: 'teacher',
    active: 'true',
    tags: 'alpha|batch',
    knowledgeStatus: 'READY',
    embeddingStatus: 'PENDING',
    senseEnglishDefinition: 'alpha definition',
    senseFrenchDefinition: 'definition alpha',
    senseChineseDefinition: '词义 alpha',
    exampleEnglish: 'alpha example',
    exampleFrench: 'exemple alpha',
    exampleChinese: '例句 alpha',
    exampleContextSupport: 'MEDIUM',
  },
  validationErrors: ['Lexical pair already exists'],
  importedLexicalPairId: null,
  importMessage: null,
};

function pageResult<T>(records: T[]): PageResult<T> {
  return {
    total: records.length,
    pageNo: 1,
    pageSize: Math.max(records.length, 1),
    records,
  };
}

const queryClients: QueryClient[] = [];

function renderImportCenter(
  initialEntries: string[] = ['/teacher/lexical-pairs/imports'],
  mode: 'teacher' | 'admin' = 'teacher'
) {
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
      <MemoryRouter initialEntries={initialEntries}>
        <LexicalImportCenter mode={mode} />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('LexicalImportCenter', () => {
  beforeEach(() => {
    vi.mocked(adminService.listUsers).mockResolvedValue(pageResult([]));
    vi.mocked(adminService.getRagReindexJob).mockResolvedValue({
      jobId: 'job-41',
      jobType: 'KNOWLEDGE_REINDEX',
      mode: 'MANUAL',
      status: 'SUCCEEDED',
      sourceTypes: ['LEXICAL_PAIR', 'LEXICAL_SENSE', 'LEXICAL_EXAMPLE'],
      sourceIds: ['501'],
      lastCursor: null,
      lastSourceUpdatedAt: null,
      finishedAt: '2026-03-22T10:06:00',
      stats: {
        documentsProcessed: 1,
        chunksProcessed: 3,
        embeddedChunks: 3,
      },
      errorMessage: null,
    });
    vi.mocked(lexicalPairService.listImportBatches).mockResolvedValue(pageResult([batchSummary]));
    vi.mocked(lexicalPairService.getImportBatch).mockResolvedValue(batchDetail);
    vi.mocked(lexicalPairService.listImportRows).mockResolvedValue(pageResult([invalidRow]));
    vi.mocked(lexicalPairService.updateImportRow).mockResolvedValue({
      ...invalidRow,
      status: 'READY',
      validationErrors: [],
      draft: {
        ...invalidRow.draft,
        chineseGloss: '修正后的中文释义',
      },
    });
    vi.mocked(lexicalPairService.createImportBatch).mockResolvedValue({
      batchId: 77,
      status: 'PARSING',
    } satisfies LexicalImportBatchCreatedVO);
    vi.mocked(lexicalPairService.commitImportBatch).mockResolvedValue({
      batchId: 41,
      status: 'IMPORTING',
    } satisfies LexicalImportBatchCreatedVO);
    vi.mocked(lexicalPairService.reindexImportBatch).mockResolvedValue({
      jobId: 'job-41',
      status: 'PENDING',
    });
    vi.mocked(lexicalPairService.downloadImportFile).mockResolvedValue(new Blob(['csv']));
  });

  afterEach(() => {
    cleanup();
    queryClients.splice(0).forEach((client) => client.clear());
    vi.clearAllMocks();
  });

  it('creates an import batch from a selected file', async () => {
    vi.mocked(lexicalPairService.listImportBatches).mockResolvedValue(pageResult([]));
    vi.mocked(lexicalPairService.getImportBatch).mockResolvedValue({
      ...batchDetail,
      id: 77,
      status: 'PARSING',
      originalFilename: 'new-import.xlsx',
      sourceFormat: 'XLSX',
    });

    const { container } = renderImportCenter();
    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement | null;
    expect(fileInput).not.toBeNull();

    const uploadFile = new File(['xlsx-content'], 'new-import.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });

    fireEvent.change(fileInput!, {
      target: {
        files: [uploadFile],
      },
    });

    await userEvent.click(await screen.findByRole('button', { name: '创建导入批次' }));

    await waitFor(() => {
      expect(lexicalPairService.createImportBatch).toHaveBeenCalledTimes(1);
    });

    const formData = vi.mocked(lexicalPairService.createImportBatch).mock.calls[0][0] as FormData;
    expect(formData.get('file')).toBe(uploadFile);
    expect(await screen.findByText('已创建导入批次 #77，后台正在解析文件。')).toBeInTheDocument();
  });

  it('saves an edited draft row', async () => {
    renderImportCenter();
    const user = userEvent.setup();

    expect(await screen.findByText('编辑第 2 行')).toBeInTheDocument();

    const glossInput = screen.getByLabelText('中文释义');
    await user.clear(glossInput);
    await user.type(glossInput, '修正后的中文释义');

    await user.click(screen.getByRole('button', { name: '保存草稿行' }));

    await waitFor(() => {
      expect(lexicalPairService.updateImportRow).toHaveBeenCalledWith(
        41,
        501,
        expect.objectContaining({
          chineseGloss: '修正后的中文释义',
          englishWord: 'lexicalalpha',
          frenchWord: 'lexicalalpha',
        })
      );
    });

    expect(await screen.findByText('已保存第 2 行草稿。')).toBeInTheDocument();
  });

  it('uses semantic view filters from the URL when listing batches', async () => {
    renderImportCenter(['/teacher/lexical-pairs/imports?view=pending&batchId=41&source=workspace']);

    await waitFor(() => {
      expect(lexicalPairService.listImportBatches).toHaveBeenCalled();
    });

    const [params] = vi.mocked(lexicalPairService.listImportBatches).mock.calls[0];
    expect(params).toMatchObject({
      view: 'pending',
      status: undefined,
      pageNo: 1,
      pageSize: 12,
    });
    expect(await screen.findByText(/当前从教师工作台进入/)).toBeInTheDocument();
  });

  it('shows embedding sync summary for the selected batch', async () => {
    vi.mocked(lexicalPairService.getImportBatch).mockResolvedValue({
      ...batchDetail,
      importedRows: 4,
      pendingEmbeddingCount: 1,
      embeddedCount: 3,
      failedEmbeddingCount: 0,
      latestEmbeddedAt: '2026-03-22T10:06:00',
    });

    renderImportCenter();

    expect(await screen.findByText('知识同步概览')).toBeInTheDocument();
    expect(await screen.findByText('仍有 1 条词对等待嵌入，后台知识同步还在继续。')).toBeInTheDocument();
    expect(await screen.findByText(/最近成功嵌入/)).toBeInTheDocument();
    expect(await screen.findByText('待嵌入')).toBeInTheDocument();
    expect(await screen.findByText('已嵌入')).toBeInTheDocument();
  });

  it('allows admin to trigger batch reindex and shows the latest job summary', async () => {
    vi.mocked(lexicalPairService.getImportBatch).mockResolvedValue({
      ...batchDetail,
      status: 'COMPLETED',
      importedRows: 4,
      pendingEmbeddingCount: 2,
      embeddedCount: 2,
      failedEmbeddingCount: 0,
      latestEmbeddedAt: '2026-03-22T10:06:00',
    });
    vi.mocked(lexicalPairService.listImportBatches).mockResolvedValue(
      pageResult([
        {
          ...batchSummary,
          status: 'COMPLETED',
          importedRows: 4,
        },
      ])
    );
    renderImportCenter(['/admin/lexical-pairs/imports'], 'admin');

    await userEvent.click(await screen.findByRole('button', { name: '重建本批索引' }));

    await waitFor(() => {
      expect(lexicalPairService.reindexImportBatch).toHaveBeenCalledWith(41);
    });
    await waitFor(() => {
      expect(adminService.getRagReindexJob).toHaveBeenCalledWith('job-41', expect.anything());
    });

    expect(await screen.findByText(/最近一次定向重建任务 #job-41/)).toBeInTheDocument();
    expect(await screen.findByText(/任务已完成。文档 1 · 分块 3 · 新嵌入 3/)).toBeInTheDocument();
  });
});

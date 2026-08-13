import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import HistoryPage from './History';

vi.mock('@/lib/services', () => ({
  diagnosisSessionService: {
    listHistory: vi.fn().mockResolvedValue({ total: 0, pageNo: 1, pageSize: 10, records: [] }),
    getResult: vi.fn(),
  },
  trainingService: {
    listHistory: vi.fn().mockResolvedValue({ total: 0, pageNo: 1, pageSize: 10, records: [] }),
    getSummary: vi.fn(),
  },
  assessmentService: {
    listStudentHistory: vi.fn().mockResolvedValue({ total: 0, pageNo: 1, pageSize: 10, records: [] }),
    getStudentAttemptResult: vi.fn(),
  },
}));

function renderHistoryPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchOnWindowFocus: false },
    },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <HistoryPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('HistoryPage tabs', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it('renders readable Chinese tab labels instead of mojibake', async () => {
    renderHistoryPage();
    expect(await screen.findByRole('button', { name: '诊断历史' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '训练历史' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '测评历史' })).toBeInTheDocument();
    expect(screen.queryByText('璇婃柇鍘嗗彶')).not.toBeInTheDocument();
  });
});

import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@/lib/i18n';
import { practiceService } from '@/lib/services';
import PracticePage from './index';

vi.mock('@/lib/services', () => ({
  practiceService: {
    listBanks: vi.fn(),
    listHistory: vi.fn(),
    startSession: vi.fn(),
    getSession: vi.fn(),
    saveDraft: vi.fn(),
    checkSpelling: vi.fn(),
    complete: vi.fn(),
    abandon: vi.fn(),
    getResult: vi.fn(),
  },
  aiService: {
    practiceTutoringAsync: vi.fn(),
    explainPracticeQuestion: vi.fn(),
  },
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchOnWindowFocus: false },
    },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <PracticePage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('PracticePage accessibility', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(practiceService.listHistory).mockResolvedValue({
      total: 0,
      pageNo: 1,
      pageSize: 6,
      records: [],
    });
    vi.mocked(practiceService.listBanks).mockResolvedValue([
      {
        bankCode: 'LEXIBRIDGE_FF4_V2',
        name: '假朋友练习',
        description: '自测题库',
        totalQuestionCount: 2,
        sections: [
          {
            sectionCode: 'FF4_WORD_MEANING',
            title: '词义',
            description: '词义选择',
            questionCount: 1,
            constructCodes: ['FF4_WORD_MEANING'],
          },
        ],
      },
    ]);
    vi.mocked(practiceService.startSession).mockResolvedValue({
      sessionId: 11,
      bankCode: 'LEXIBRIDGE_FF4_V2',
      sectionCode: 'FF4_WORD_MEANING',
      totalCount: 2,
    });
    vi.mocked(practiceService.getSession).mockResolvedValue({
      sessionId: 11,
      bankCode: 'LEXIBRIDGE_FF4_V2',
      sectionCode: 'FF4_WORD_MEANING',
      status: 'IN_PROGRESS',
      totalCount: 2,
      answeredCount: 0,
      correctCount: null,
      startedAt: '2026-08-13T10:00:00',
      completedAt: null,
      questions: [
        {
          questionOrder: 1,
          questionCode: 'Q1',
          questionType: 'SPELLING',
          stemText: '请写出法语单词',
          promptText: null,
          options: [],
          sectionCode: 'FF4_SPELLING',
          constructCode: 'FF4_SPELLING',
          transferCategory: null,
          targetWord: 'actuel',
          response: [],
          spellingHintShown: null,
          spellingHintFirstLetter: null,
          spellingWrongAttemptCount: null,
          answered: false,
        },
        {
          questionOrder: 2,
          questionCode: 'Q2',
          questionType: 'SINGLE_CHOICE',
          stemText: 'actuel 的意思是？',
          promptText: null,
          options: [
            { key: 'A', label: '当前的' },
            { key: 'B', label: '实际的' },
          ],
          sectionCode: 'FF4_WORD_MEANING',
          constructCode: 'FF4_WORD_MEANING',
          transferCategory: null,
          targetWord: 'actuel',
          response: [],
          spellingHintShown: null,
          spellingHintFirstLetter: null,
          spellingWrongAttemptCount: null,
          answered: false,
        },
      ],
    });
    vi.mocked(practiceService.saveDraft).mockResolvedValue({
      sessionId: 11,
      status: 'IN_PROGRESS',
      totalCount: 2,
      answeredCount: 0,
      correctCount: null,
    });
  });

  afterEach(() => {
    cleanup();
  });

  it('gives the spelling input an accessible name and marks choices as radios', async () => {
    const user = userEvent.setup();
    renderPage();

    const startButton = await screen.findByRole('button', { name: /全库混合/ });
    await user.click(startButton);

    const spellingInput = await screen.findByRole('textbox', { name: '请输入法语单词' });
    expect(spellingInput).toBeInTheDocument();

    const choiceGroup = screen.getByRole('radiogroup', { name: 'actuel 的意思是？' });
    expect(choiceGroup).toBeInTheDocument();
    const optionA = screen.getByRole('radio', { name: /当前的/ });
    expect(optionA).toHaveAttribute('aria-checked', 'false');

    await user.click(optionA);
    await waitFor(() => {
      expect(screen.getByRole('radio', { name: /当前的/ })).toHaveAttribute('aria-checked', 'true');
    });
  });
});

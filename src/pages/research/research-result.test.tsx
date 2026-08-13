import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { PublicAssessmentResultVO } from '@/lib/contracts';
import { PublicResult } from './index';

const result = (overrides: Partial<PublicAssessmentResultVO> = {}): PublicAssessmentResultVO => ({
  attemptId: 42001,
  releaseCode: 'RES-TEST',
  paperTitle: 'Lexi-bridge 英法词汇认知迁移研究问卷',
  status: 'SUBMITTED',
  questionCount: 60,
  answeredCount: 60,
  objectiveScore: 0,
  totalScore: 0,
  submittedAt: '2026-08-13T12:00:00Z',
  scoreVisible: true,
  qualityFlags: ['FAST_ITEM', 'TIMING_GAP'],
  aiAnalysisStatus: 'FAILED',
  aiAnalysis: null,
  questions: [],
  ...overrides,
});

describe('public assessment result', () => {
  it('keeps zero scores and quality notices readable', () => {
    render(<PublicResult result={result()} />);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('你的迁移路径，已经被记录。');
    expect(screen.getByRole('region', { name: '提交结果摘要' })).toBeInTheDocument();
    expect(screen.getByLabelText('规则评分 0，总分 0')).toHaveTextContent('0 / 0');
    expect(screen.getByRole('note')).toHaveTextContent('数据质量提醒：过快作答、计时缺失');
  });

  it('offers a retake when the release still allows another attempt', async () => {
    const onStartNewAttempt = vi.fn();
    const onUseAnotherCode = vi.fn();
    const user = userEvent.setup();
    render(
      <PublicResult
        result={result({ canStartNewAttempt: true, maxAttempts: 10, attemptNo: 1 })}
        onStartNewAttempt={onStartNewAttempt}
        onUseAnotherCode={onUseAnotherCode}
      />
    );

    expect(screen.getByText(/你可以再答一次或换一个参与码进入/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '再答一次' }));
    expect(onStartNewAttempt).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: '用其他参与码进入' }));
    expect(onUseAnotherCode).toHaveBeenCalledTimes(1);
  });

  it('still lets a submitted participant switch participation codes', async () => {
    const onUseAnotherCode = vi.fn();
    const user = userEvent.setup();
    render(<PublicResult result={result()} onUseAnotherCode={onUseAnotherCode} />);

    expect(screen.queryByRole('button', { name: '再答一次' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '用其他参与码进入' }));
    expect(onUseAnotherCode).toHaveBeenCalledTimes(1);
  });

  it('renders the waiting state without a score summary when scores are hidden', () => {
    render(<PublicResult result={result({
      scoreVisible: false,
      qualityFlags: [],
      aiAnalysisStatus: 'PROCESSING',
    })} />);

    expect(screen.queryByRole('region', { name: '提交结果摘要' })).not.toBeInTheDocument();
    expect(screen.getByText('模型生成中 · 可稍后返回')).toBeInTheDocument();
    expect(screen.getByText(/分析通常需要 1–3 分钟/)).toBeInTheDocument();
  });
});

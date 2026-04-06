import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { useSessionRuntime } from './useSessionRuntime';

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

function RuntimeHarness({
  saveProgress,
}: {
  saveProgress: (sessionId: number, snapshot: Record<string, unknown>) => Promise<unknown>;
}) {
  const runtime = useSessionRuntime({
    active: true,
    sessionId: 7,
    nextItem: { id: 1 },
    refetchCurrent: vi.fn().mockResolvedValue({ data: { id: 1 } }),
    buildSnapshot: () => ({ currentItemId: 1 }),
    saveProgress,
    saveProgressKeepalive: vi.fn().mockResolvedValue({}),
    isCompleted: () => false,
    onCompleted: vi.fn(),
  });

  return <div>{runtime.saveErrorMessage}</div>;
}

describe('useSessionRuntime route leave protection', () => {
  beforeEach(() => {
    blockerState = 'unblocked';
    blockerProceed.mockReset();
    blockerReset.mockReset();
    mockedUseBlocker.mockClear();
    mockedUseBeforeUnload.mockClear();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('does not proceed when the route-save fails', async () => {
    const saveProgress = vi.fn().mockRejectedValue(new Error('offline'));
    const view = render(<RuntimeHarness saveProgress={saveProgress} />);

    blockerState = 'blocked';
    view.rerender(<RuntimeHarness saveProgress={saveProgress} />);

    expect(await screen.findByText('offline')).toBeInTheDocument();
    await waitFor(() => {
      expect(saveProgress).toHaveBeenCalled();
      expect(blockerProceed).not.toHaveBeenCalled();
      expect(blockerReset).toHaveBeenCalled();
    });
  });
});

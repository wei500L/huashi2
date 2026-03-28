import { describe, expect, it, vi } from 'vitest';
import { buildProgressPercent, buildSessionSnapshot } from './helpers';

describe('session runtime helpers', () => {
  it('builds a resumable session snapshot from the current item payload', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-03-28T08:00:00.000Z'));

    expect(buildSessionSnapshot(42, { currentItemOrder: 3, answeredItems: 2 })).toEqual({
      sessionId: 42,
      currentItemOrder: 3,
      answeredItems: 2,
      timestamp: '2026-03-28T08:00:00.000Z',
    });

    vi.useRealTimers();
  });

  it('caps the progress percentage between 0 and 100', () => {
    expect(buildProgressPercent(4, 5)).toBe(80);
    expect(buildProgressPercent(7, 5)).toBe(100);
    expect(buildProgressPercent(undefined, 0)).toBe(0);
  });
});

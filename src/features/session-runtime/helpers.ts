export type SessionProgressLike = {
  currentItemOrder?: number | null;
  answeredItems?: number | null;
};

export function buildSessionSnapshot<TProgress extends SessionProgressLike>(
  sessionId: number,
  nextItem?: TProgress | null
): Record<string, unknown> {
  if (!nextItem) {
    return {
      sessionId,
      timestamp: new Date().toISOString(),
    };
  }
  return {
    sessionId,
    currentItemOrder: nextItem.currentItemOrder ?? null,
    answeredItems: nextItem.answeredItems ?? null,
    timestamp: new Date().toISOString(),
  };
}

export function buildProgressPercent(answered?: number | null, total?: number | null): number {
  const safeAnswered = Math.max(0, answered ?? 0);
  const safeTotal = Math.max(1, total ?? 1);
  return Math.max(0, Math.min(100, (safeAnswered / safeTotal) * 100));
}

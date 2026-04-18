export type DiagnosisLaunchParams = {
  source?: string | null;
  sourceSummaryId?: number | null;
};

const DIAGNOSIS_LAUNCH_KEYS = ['source', 'sourceSummaryId'] as const;

export function buildDiagnosisHref(params: DiagnosisLaunchParams = {}) {
  const search = new URLSearchParams();
  if (params.source) {
    search.set('source', params.source);
  }
  if (params.sourceSummaryId) {
    search.set('sourceSummaryId', String(params.sourceSummaryId));
  }
  const query = search.toString();
  return query ? `/diagnosis?${query}` : '/diagnosis';
}

export function clearDiagnosisLaunchParams(searchParams: URLSearchParams) {
  const next = new URLSearchParams(searchParams);
  DIAGNOSIS_LAUNCH_KEYS.forEach((key) => next.delete(key));
  return next;
}

export function parseDiagnosisLaunchNumber(value?: string | null) {
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

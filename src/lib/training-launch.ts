export type TrainingLaunchParams = {
  mode?: string | null;
  source?: string | null;
  diagnosisSummaryId?: number | null;
  lexicalPairId?: number | null;
  wrongBookId?: number | null;
  reviewScheduleId?: number | null;
};

const TRAINING_LAUNCH_KEYS = [
  'mode',
  'source',
  'diagnosisSummaryId',
  'lexicalPairId',
  'wrongBookId',
  'reviewScheduleId',
] as const;

export function buildTrainingHref(params: TrainingLaunchParams) {
  const search = new URLSearchParams();
  if (params.mode) {
    search.set('mode', params.mode);
  }
  if (params.source) {
    search.set('source', params.source);
  }
  if (params.diagnosisSummaryId) {
    search.set('diagnosisSummaryId', String(params.diagnosisSummaryId));
  }
  if (params.lexicalPairId) {
    search.set('lexicalPairId', String(params.lexicalPairId));
  }
  if (params.wrongBookId) {
    search.set('wrongBookId', String(params.wrongBookId));
  }
  if (params.reviewScheduleId) {
    search.set('reviewScheduleId', String(params.reviewScheduleId));
  }
  const query = search.toString();
  return query ? `/training?${query}` : '/training';
}

export function clearTrainingLaunchParams(searchParams: URLSearchParams) {
  const next = new URLSearchParams(searchParams);
  TRAINING_LAUNCH_KEYS.forEach((key) => next.delete(key));
  return next;
}

export function parseTrainingLaunchNumber(value?: string | null) {
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

import type { ResearchAnalyticsFilter } from '@/lib/contracts';
import { EMPTY_RESEARCH_FILTERS, type ResearchWorkspaceFilters } from './types';

export function normalizeResearchFilters(input: Partial<ResearchWorkspaceFilters> | ResearchAnalyticsFilter | URLSearchParams): ResearchWorkspaceFilters {
  const source = input instanceof URLSearchParams
    ? {
        status: input.get('status') || '',
        entryType: input.get('entryType') || '',
        qualityFlag: input.get('qualityFlag') || '',
        aiStatus: input.get('aiStatus') || '',
        submittedFrom: input.get('submittedFrom') || '',
        submittedTo: input.get('submittedTo') || '',
        keyword: input.get('keyword') || '',
      }
    : input;
  return {
    status: source.status?.trim() || '',
    entryType: source.entryType?.trim() || '',
    qualityFlag: source.qualityFlag?.trim() || '',
    aiStatus: source.aiStatus?.trim() || '',
    submittedFrom: source.submittedFrom?.trim() || '',
    submittedTo: source.submittedTo?.trim() || '',
    keyword: source.keyword?.trim() || '',
  };
}

export function toResearchApiFilters(filters: ResearchWorkspaceFilters): ResearchAnalyticsFilter {
  return {
    status: filters.status || undefined,
    entryType: filters.entryType || undefined,
    qualityFlag: filters.qualityFlag || undefined,
    aiStatus: filters.aiStatus || undefined,
    submittedFrom: filters.submittedFrom || undefined,
    submittedTo: filters.submittedTo || undefined,
    keyword: filters.keyword || undefined,
  };
}

export function hasActiveResearchFilters(filters: ResearchWorkspaceFilters): boolean {
  return Object.values(filters).some((value) => value.trim().length > 0);
}

export function writeResearchFiltersToSearch(params: URLSearchParams, filters: ResearchWorkspaceFilters) {
  (Object.keys(EMPTY_RESEARCH_FILTERS) as Array<keyof ResearchWorkspaceFilters>).forEach((key) => {
    const value = filters[key];
    if (value) params.set(key, value);
    else params.delete(key);
  });
}

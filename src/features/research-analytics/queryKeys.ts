import type { ResearchWorkspaceFilters } from './types';
import { normalizeResearchFilters } from './filters';

export const researchAnalyticsKeys = {
  all: ['research-analytics'] as const,
  releases: () => [...researchAnalyticsKeys.all, 'releases'] as const,
  overview: (publishId: number, filters: ResearchWorkspaceFilters) =>
    [...researchAnalyticsKeys.all, 'overview', publishId, normalizeResearchFilters(filters)] as const,
  attempts: (publishId: number, filters: ResearchWorkspaceFilters, page: number, sort: string) =>
    [...researchAnalyticsKeys.all, 'attempts', publishId, normalizeResearchFilters(filters), page, sort] as const,
  questionStats: (publishId: number, filters: ResearchWorkspaceFilters) =>
    [...researchAnalyticsKeys.all, 'question-stats', publishId, normalizeResearchFilters(filters)] as const,
  optionStats: (publishId: number, filters: ResearchWorkspaceFilters) =>
    [...researchAnalyticsKeys.all, 'option-stats', publishId, normalizeResearchFilters(filters)] as const,
  dimensionStats: (publishId: number, filters: ResearchWorkspaceFilters) =>
    [...researchAnalyticsKeys.all, 'dimension-stats', publishId, normalizeResearchFilters(filters)] as const,
  reactionStats: (publishId: number, filters: ResearchWorkspaceFilters) =>
    [...researchAnalyticsKeys.all, 'reaction-stats', publishId, normalizeResearchFilters(filters)] as const,
  qualityStats: (publishId: number, filters: ResearchWorkspaceFilters) =>
    [...researchAnalyticsKeys.all, 'quality-stats', publishId, normalizeResearchFilters(filters)] as const,
  aiReport: (publishId: number, reportId?: number) =>
    [...researchAnalyticsKeys.all, 'ai-report', publishId, reportId ?? 'latest'] as const,
  attemptDetail: (attemptId: number) => [...researchAnalyticsKeys.all, 'attempt', attemptId] as const,
};

export type ResearchWorkspaceFilters = {
  status: string;
  entryType: string;
  qualityFlag: string;
  aiStatus: string;
  submittedFrom: string;
  submittedTo: string;
  keyword: string;
};

export const EMPTY_RESEARCH_FILTERS: ResearchWorkspaceFilters = {
  status: '',
  entryType: '',
  qualityFlag: '',
  aiStatus: '',
  submittedFrom: '',
  submittedTo: '',
  keyword: '',
};

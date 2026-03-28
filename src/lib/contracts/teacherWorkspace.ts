export interface TeacherWorkspaceSummaryVO {
  classCount: number;
  studentCount: number;
  draftTemplateCount: number;
  pendingInterventionCount: number;
  lexicalPairCount: number;
  lexicalListCount: number;
  pendingImportBatchCount: number;
}

export interface TeacherWorkspaceClassActivityVO {
  classId: number;
  classCode: string;
  className: string;
  studentCount: number;
  highRiskStudentCount: number;
  lastActiveAt?: string | null;
}

export interface TeacherWorkspaceDraftTemplateVO {
  draftId: number;
  templateName: string;
  syncState: string;
  updatedAt?: string | null;
}

export interface TeacherWorkspaceInterventionVO {
  id: number;
  studentName?: string | null;
  priority: string;
  status: string;
  plannedAt?: string | null;
}

export interface TeacherWorkspaceLexicalListVO {
  id: number;
  listName: string;
  itemCount: number;
  updatedAt?: string | null;
}

export interface TeacherWorkspaceOverviewVO {
  teacherName: string;
  organizationLabel?: string | null;
  summary: TeacherWorkspaceSummaryVO;
  recentClasses: TeacherWorkspaceClassActivityVO[];
  draftTemplates: TeacherWorkspaceDraftTemplateVO[];
  pendingInterventions: TeacherWorkspaceInterventionVO[];
  recentLexicalLists: TeacherWorkspaceLexicalListVO[];
}

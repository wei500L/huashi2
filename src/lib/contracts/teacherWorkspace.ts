export interface TeacherWorkspaceSummaryVO {
  classCount: number;
  studentCount: number;
  draftTemplateCount: number;
  pendingInterventionCount: number;
  lexicalPairCount: number;
  lexicalListCount: number;
  pendingImportBatchCount: number;
  assessmentPaperCount: number;
  activeAssessmentPublishCount: number;
  pendingAssessmentSubmissionCount: number;
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
  classId?: number | null;
  studentUserId?: number | null;
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

export interface TeacherWorkspaceAssessmentPublishVO {
  publishId: number;
  paperId: number;
  title: string;
  classId: number;
  className: string;
  publishedAt?: string | null;
  dueAt?: string | null;
  assignedCount: number;
  submittedCount: number;
  pendingCount: number;
}

export interface TeacherWorkspaceOverviewVO {
  teacherName: string;
  organizationLabel?: string | null;
  summary: TeacherWorkspaceSummaryVO;
  recentClasses: TeacherWorkspaceClassActivityVO[];
  draftTemplates: TeacherWorkspaceDraftTemplateVO[];
  pendingInterventions: TeacherWorkspaceInterventionVO[];
  recentLexicalLists: TeacherWorkspaceLexicalListVO[];
  recentAssessmentPublishes: TeacherWorkspaceAssessmentPublishVO[];
}

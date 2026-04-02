package com.huashi.eftransfer.app.modules.analytics.vo;

public record TeacherWorkspaceSummaryVO(
        long classCount,
        long studentCount,
        long draftTemplateCount,
        long pendingInterventionCount,
        long lexicalPairCount,
        long lexicalListCount,
        long pendingImportBatchCount,
        long assessmentPaperCount,
        long activeAssessmentPublishCount,
        long pendingAssessmentSubmissionCount
) {
}

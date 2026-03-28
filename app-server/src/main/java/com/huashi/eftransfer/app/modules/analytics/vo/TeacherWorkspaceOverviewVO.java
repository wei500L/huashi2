package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.List;

public record TeacherWorkspaceOverviewVO(
        String teacherName,
        String organizationLabel,
        TeacherWorkspaceSummaryVO summary,
        List<TeacherWorkspaceClassActivityVO> recentClasses,
        List<TeacherWorkspaceDraftTemplateVO> draftTemplates,
        List<TeacherWorkspaceInterventionVO> pendingInterventions,
        List<TeacherWorkspaceLexicalListVO> recentLexicalLists
) {
}

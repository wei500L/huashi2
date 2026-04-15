package com.huashi.eftransfer.app.modules.analytics.vo;

public record TeacherInterventionEffectVO(
        Long baselineSnapshotId,
        Long completionSnapshotId,
        TeacherInterventionEffectSnapshotVO baselineSnapshot,
        TeacherInterventionEffectSnapshotVO completionSnapshot,
        TeacherInterventionEffectDiffVO metricDiff
) {
}

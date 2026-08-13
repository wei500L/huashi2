package com.huashi.eftransfer.app.modules.user.vo;

public record StudentProfileVO(
        String studentNo,
        String gradeName,
        String frenchLevel,
        String courseStage,
        int compositeScore,
        Integer dailyTrainingTarget,
        Integer weeklyAccuracyTarget
) {
}

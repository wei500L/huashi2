package com.huashi.eftransfer.app.modules.diagnosis.vo;

public record DiagnosisNextItemVO(
        Long sessionId,
        String sessionStatus,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        Boolean hasNextItem,
        Boolean readyToComplete,
        DiagnosisQuestionItemVO item
) {
}

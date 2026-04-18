package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.shared.enums.DiagnosisSessionStatus;

public record DiagnosisNextItemVO(
        Long sessionId,
        DiagnosisSessionStatus sessionStatus,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        Boolean hasNextItem,
        Boolean readyToComplete,
        DiagnosisQuestionItemVO item
) {
}

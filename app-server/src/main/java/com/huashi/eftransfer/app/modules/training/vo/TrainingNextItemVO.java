package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.app.common.session.SessionCompletionHookStatus;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.enums.TrainingSessionStatus;

public record TrainingNextItemVO(
        Long sessionId,
        TrainingSessionStatus sessionStatus,
        TrainingMode mode,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        Boolean hasNextItem,
        Boolean readyToComplete,
        SessionCompletionHookStatus completionHooksStatus,
        TrainingQuestionItemVO item
) {
}

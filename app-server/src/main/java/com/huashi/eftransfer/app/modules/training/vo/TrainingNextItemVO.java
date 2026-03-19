package com.huashi.eftransfer.app.modules.training.vo;

public record TrainingNextItemVO(
        Long sessionId,
        String sessionStatus,
        String mode,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        Boolean hasNextItem,
        TrainingQuestionItemVO item
) {
}

package com.huashi.eftransfer.app.modules.assessment.dto;

import java.util.Map;

public record PublicAssessmentVerifyRequest(
        String participationCode,
        Map<String, Object> basicInfo
) {
}

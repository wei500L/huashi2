package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigSemanticValidator;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiOpsLocalValidationService {

    private final Validator validator;

    public AiOpsLocalValidationService(Validator validator) {
        this.validator = validator;
    }

    public AiOpsConfigValidationResponse validate(AiOpsConfigPayload payload, List<String> notices) {
        List<AiOpsConfigIssue> issues = collectIssues(payload);
        return new AiOpsConfigValidationResponse(
                issues.isEmpty(),
                issues,
                notices == null ? List.of() : notices
        );
    }

    public void requireValid(AiOpsConfigPayload payload) {
        List<AiOpsConfigIssue> issues = collectIssues(payload);
        if (!issues.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, formatIssues(issues), 400);
        }
    }

    public List<AiOpsConfigIssue> collectIssues(AiOpsConfigPayload payload) {
        List<AiOpsConfigIssue> issues = new ArrayList<>();
        if (payload != null) {
            for (ConstraintViolation<AiOpsConfigPayload> violation : validator.validate(payload)) {
                issues.add(new AiOpsConfigIssue(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ));
            }
        }
        issues.addAll(AiOpsConfigSemanticValidator.validate(payload));

        Map<String, AiOpsConfigIssue> deduped = new LinkedHashMap<>();
        issues.stream()
                .sorted(Comparator.comparing(AiOpsConfigIssue::field).thenComparing(AiOpsConfigIssue::message))
                .forEach(issue -> deduped.putIfAbsent(issue.field() + "\u0000" + issue.message(), issue));
        return List.copyOf(deduped.values());
    }

    private String formatIssues(List<AiOpsConfigIssue> issues) {
        return issues.stream()
                .map(issue -> issue.field() + ": " + issue.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("AI ops config validation failed");
    }
}

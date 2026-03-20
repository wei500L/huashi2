package com.huashi.eftransfer.app.modules.ai.service;

import com.huashi.eftransfer.app.modules.ai.support.AiJsonCodec;
import com.huashi.eftransfer.app.modules.ai.support.AiStructuredGuidancePayload;
import com.huashi.eftransfer.app.modules.ai.support.LexicalStructuredAnswerPayload;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiResponseValidator {

    private final AiJsonCodec aiJsonCodec;
    private final Validator validator;

    public AiResponseValidator(AiJsonCodec aiJsonCodec, Validator validator) {
        this.aiJsonCodec = aiJsonCodec;
        this.validator = validator;
    }

    public AiStructuredGuidancePayload validateGuidance(Map<String, Object> structuredData) {
        return validatePayload(structuredData, AiStructuredGuidancePayload.class);
    }

    public LexicalStructuredAnswerPayload validateLexicalRagAnswer(
            Map<String, Object> structuredData,
            Set<String> availableCitationIds
    ) {
        LexicalStructuredAnswerPayload payload = validatePayload(structuredData, LexicalStructuredAnswerPayload.class);
        Set<String> citationIds = payload.citationIds() == null
                ? Set.of()
                : payload.citationIds().stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (citationIds.isEmpty()) {
            throw new IllegalStateException("citationIds must not be empty");
        }
        if (payload.citationIds() != null && payload.citationIds().size() != citationIds.size()) {
            throw new IllegalStateException("citationIds must not contain duplicates");
        }
        if (!availableCitationIds.containsAll(citationIds)) {
            throw new IllegalStateException("citationIds must reference retrieved citations only");
        }
        String combinedText = (payload.answer() == null ? "" : payload.answer())
                + "\n"
                + (payload.explanation() == null ? "" : payload.explanation());
        boolean allInlineReferenced = citationIds.stream()
                .allMatch(citationId -> combinedText.contains("[" + citationId + "]"));
        if (!allInlineReferenced) {
            throw new IllegalStateException("answer and explanation must include inline citations for every citationId");
        }
        return payload;
    }

    private <T> T validatePayload(Map<String, Object> structuredData, Class<T> targetType) {
        T payload = aiJsonCodec.convert(structuredData, targetType);
        Set<ConstraintViolation<T>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            throw new IllegalStateException(violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining("; ")));
        }
        return payload;
    }
}

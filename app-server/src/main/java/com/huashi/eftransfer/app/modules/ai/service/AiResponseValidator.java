package com.huashi.eftransfer.app.modules.ai.service;

import com.huashi.eftransfer.app.modules.ai.support.AiJsonCodec;
import com.huashi.eftransfer.app.modules.ai.support.AiStructuredGuidancePayload;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

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
        AiStructuredGuidancePayload payload = aiJsonCodec.convert(structuredData, AiStructuredGuidancePayload.class);
        Set<ConstraintViolation<AiStructuredGuidancePayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            throw new IllegalStateException(violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining("; ")));
        }
        return payload;
    }
}

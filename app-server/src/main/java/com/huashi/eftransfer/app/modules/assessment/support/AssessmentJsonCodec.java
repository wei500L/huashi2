package com.huashi.eftransfer.app.modules.assessment.support;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Component
public class AssessmentJsonCodec {

    private final ObjectMapper objectMapper;

    public AssessmentJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize assessment payload", exception);
        }
    }

    public List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return Arrays.asList(read(json, String[].class));
    }

    public List<AssessmentOptionPayload> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return Arrays.asList(read(json, AssessmentOptionPayload[].class));
    }

    private <T> T read(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize assessment payload", exception);
        }
    }
}

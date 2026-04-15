package com.huashi.eftransfer.app.modules.ai.support;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AiJsonCodec {

    private final ObjectMapper objectMapper;

    public AiJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize AI payload", exception);
        }
    }

    public <T> T convert(Object value, Class<T> targetType) {
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Failed to convert AI payload", exception);
        }
    }

    public <T> T read(String value, Class<T> targetType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, targetType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize AI payload", exception);
        }
    }
}

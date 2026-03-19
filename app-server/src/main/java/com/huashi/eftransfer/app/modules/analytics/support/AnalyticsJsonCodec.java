package com.huashi.eftransfer.app.modules.analytics.support;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AnalyticsJsonCodec {

    private final ObjectMapper objectMapper;

    public AnalyticsJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize analytics payload", exception);
        }
    }

    public <T> T read(String json, Class<T> targetType) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize analytics payload", exception);
        }
    }
}

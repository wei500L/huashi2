package com.huashi.eftransfer.app.modules.training.support;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Component
public class TrainingJsonCodec {

    private final ObjectMapper objectMapper;

    public TrainingJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize training payload", exception);
        }
    }

    public List<String> readStringList(String json) {
        return Arrays.asList(read(json, String[].class));
    }

    public TrainingStimulusPayload readStimulus(String json) {
        return read(json, TrainingStimulusPayload.class);
    }

    public List<TrainingOptionPayload> readOptions(String json) {
        return Arrays.asList(read(json, TrainingOptionPayload[].class));
    }

    public TrainingSessionSummarySnapshot readSummarySnapshot(String json) {
        return read(json, TrainingSessionSummarySnapshot.class);
    }

    public List<TrainingRiskWordSnapshot> readRiskWords(String json) {
        return Arrays.asList(read(json, TrainingRiskWordSnapshot[].class));
    }

    public TrainingLearningProfileSnapshot readLearningProfileSnapshot(String json) {
        return read(json, TrainingLearningProfileSnapshot.class);
    }

    private <T> T read(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize training payload", exception);
        }
    }
}

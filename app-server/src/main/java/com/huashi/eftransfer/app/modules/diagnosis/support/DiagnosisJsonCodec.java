package com.huashi.eftransfer.app.modules.diagnosis.support;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Component
public class DiagnosisJsonCodec {

    private final ObjectMapper objectMapper;

    public DiagnosisJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize diagnosis payload", exception);
        }
    }

    public DiagnosisStimulusPayload readStimulus(String json) {
        return read(json, DiagnosisStimulusPayload.class);
    }

    public List<DiagnosisOptionPayload> readOptions(String json) {
        return Arrays.asList(read(json, DiagnosisOptionPayload[].class));
    }

    public DiagnosisScoringProfilePayload readScoringProfile(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return read(json, DiagnosisScoringProfilePayload.class);
    }

    public List<DiagnosisDistributionItem> readDistributionItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return Arrays.asList(read(json, DiagnosisDistributionItem[].class));
    }

    public List<DiagnosisHighRiskLexicalPair> readHighRiskLexicalPairs(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return Arrays.asList(read(json, DiagnosisHighRiskLexicalPair[].class));
    }

    public DiagnosisChartPayload readChartPayload(String json) {
        return read(json, DiagnosisChartPayload.class);
    }

    private <T> T read(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize diagnosis payload", exception);
        }
    }
}

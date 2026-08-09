package com.huashi.eftransfer.app.modules.ai.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AiOutputSchemaFactory {

    public Map<String, Object> assessmentAnalysisSchema() {
        Map<String, Object> stringArray = Map.of(
                "type", "array",
                "minItems", 2,
                "maxItems", 4,
                "items", Map.of("type", "string")
        );
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "performanceOverview", Map.of("type", "string"),
                        "strengths", stringArray,
                        "risks", stringArray,
                        "contextInterpretation", Map.of("type", "string"),
                        "reactionTimeInterpretation", Map.of("type", "string"),
                        "recommendations", Map.of(
                                "type", "array",
                                "minItems", 3,
                                "maxItems", 3,
                                "items", Map.of("type", "string")
                        ),
                        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "qualityNotice", Map.of("type", "string")
                ),
                "required", List.of(
                        "performanceOverview", "strengths", "risks", "contextInterpretation",
                        "reactionTimeInterpretation", "recommendations", "confidence", "qualityNotice"
                )
        );
    }

    /**
     * Guidance schema optimized for DeepSeek json_object compatibility:
     * models primarily supply IDs, reasons and free-text; the server back-fills
     * approved lexical pair metadata and training mode labels.
     */
    public Map<String, Object> guidanceSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "recommendationPath", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "properties", Map.of(
                                                "title", Map.of("type", "string"),
                                                "reason", Map.of("type", "string"),
                                                "priority", Map.of("type", "string")
                                        ),
                                        "required", List.of("title", "reason", "priority")
                                )
                        ),
                        "focusLexicalPairs", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "properties", Map.of(
                                                "lexicalPairId", Map.of("type", "integer"),
                                                "englishWord", Map.of("type", "string"),
                                                "frenchWord", Map.of("type", "string"),
                                                "chineseGloss", Map.of("type", "string"),
                                                "lexicalPairType", Map.of("type", "string"),
                                                "riskScore", Map.of("type", "number"),
                                                "dominantErrorType", Map.of("type", "string"),
                                                "focusReason", Map.of("type", "string")
                                        ),
                                        "required", List.of("lexicalPairId", "focusReason")
                                )
                        ),
                        "recommendedTrainingModes", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "properties", Map.of(
                                                "mode", Map.of("type", "string"),
                                                "label", Map.of("type", "string"),
                                                "reason", Map.of("type", "string")
                                        ),
                                        "required", List.of("mode", "reason")
                                )
                        ),
                        "explanation", Map.of("type", "string"),
                        "teacherNote", Map.of("type", "string"),
                        "diagnosisInsight", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "strengths", Map.of(
                                                "type", "array",
                                                "minItems", 1,
                                                "items", Map.of("type", "string")
                                        ),
                                        "weaknesses", Map.of(
                                                "type", "array",
                                                "minItems", 1,
                                                "items", Map.of("type", "string")
                                        ),
                                        "suggestions", Map.of(
                                                "type", "array",
                                                "minItems", 1,
                                                "items", Map.of("type", "string")
                                        )
                                ),
                                "required", List.of("strengths", "weaknesses", "suggestions")
                        ),
                        "confidence", Map.of("type", "number"),
                        "citationIds", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        ),
                        "uncertaintyNote", Map.of("type", "string")
                ),
                "required", List.of(
                        "recommendationPath",
                        "focusLexicalPairs",
                        "recommendedTrainingModes",
                        "explanation",
                        "teacherNote",
                        "confidence",
                        "citationIds",
                        "uncertaintyNote"
                )
        );
    }

    public Map<String, Object> lexicalRagAnswerSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "answer", Map.of("type", "string"),
                        "explanation", Map.of("type", "string"),
                        "recommendedActions", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", Map.of("type", "string")
                        ),
                        "confidence", Map.of("type", "number"),
                        "citationIds", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", Map.of("type", "string")
                        )
                ),
                "required", List.of(
                        "answer",
                        "explanation",
                        "recommendedActions",
                        "confidence",
                        "citationIds"
                )
        );
    }

    public Map<String, Object> groundingVerificationSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "supported", Map.of("type", "boolean"),
                        "unsupportedClaims", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        ),
                        "uncertaintyNote", Map.of("type", "string")
                ),
                "required", List.of("supported", "unsupportedClaims", "uncertaintyNote")
        );
    }
}

package com.huashi.eftransfer.app.modules.lexicon.imports.dto;

public record LexicalImportRowUpdateRequest(
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        String semanticOverlapScore,
        String falseFriendRisk,
        String defaultContextSupport,
        String difficultyLevel,
        String notes,
        String source,
        String active,
        String tags,
        String knowledgeStatus,
        String embeddingStatus,
        String senseEnglishDefinition,
        String senseFrenchDefinition,
        String senseChineseDefinition,
        String exampleEnglish,
        String exampleFrench,
        String exampleChinese,
        String exampleContextSupport,
        Boolean skipped
) {
}

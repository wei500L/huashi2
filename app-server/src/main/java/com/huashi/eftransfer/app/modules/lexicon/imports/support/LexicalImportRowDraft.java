package com.huashi.eftransfer.app.modules.lexicon.imports.support;

public record LexicalImportRowDraft(
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
        String sourceCode,
        String contentVersion,
        String wordId,
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
        String exampleContextSupport
) {
}

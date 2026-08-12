package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;

/**
 * One practice-bank word exported to the ai-gateway knowledge base. The bank
 * explanations carry TEM4 / false-friend dictionary evidence, so indexing them
 * lets the practice tutoring scenes ground their answers on the same content
 * students practiced with.
 */
public record PracticeWordKnowledgeExportItem(
        String wordCode,
        String targetWord,
        String questionType,
        String chineseMeaning,
        String explanation,
        OffsetDateTime sourceUpdatedAt
) {
}

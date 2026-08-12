package com.huashi.eftransfer.app.modules.assessment.service;

import com.huashi.eftransfer.app.modules.assessment.vo.ResearchFilterEchoVO;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public record ResearchQueryFilter(
        String status,
        String entryType,
        String qualityFlag,
        String aiStatus,
        LocalDateTime submittedFrom,
        LocalDateTime submittedTo,
        String keyword
) {
    public static ResearchQueryFilter from(
            String status,
            String entryType,
            String qualityFlag,
            String aiStatus,
            String submittedFrom,
            String submittedTo,
            String keyword
    ) {
        return new ResearchQueryFilter(
                blankToNull(status),
                blankToNull(entryType),
                blankToNull(qualityFlag),
                blankToNull(aiStatus),
                parseTime(submittedFrom),
                parseTime(submittedTo),
                blankToNull(keyword)
        );
    }

    public ResearchFilterEchoVO echo() {
        return new ResearchFilterEchoVO(status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword);
    }

    public Long keywordParticipantId() {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("P-")) {
            normalized = normalized.substring(2);
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            try {
                return Long.parseLong(normalized);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static LocalDateTime parseTime(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid datetime: " + value);
        }
    }
}

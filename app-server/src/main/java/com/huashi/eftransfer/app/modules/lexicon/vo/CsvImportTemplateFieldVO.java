package com.huashi.eftransfer.app.modules.lexicon.vo;

public record CsvImportTemplateFieldVO(
        String fieldName,
        boolean required,
        String description,
        String example
) {
}

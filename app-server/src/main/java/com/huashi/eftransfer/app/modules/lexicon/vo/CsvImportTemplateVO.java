package com.huashi.eftransfer.app.modules.lexicon.vo;

import java.util.List;

public record CsvImportTemplateVO(
        List<CsvImportTemplateFieldVO> fields,
        String headerLine,
        String exampleLine
) {
}

package com.huashi.eftransfer.app.modules.practice.vo;

import java.util.List;

public record PracticeSectionVO(
        String sectionCode,
        String title,
        String description,
        Integer questionCount,
        List<String> constructCodes
) {
}

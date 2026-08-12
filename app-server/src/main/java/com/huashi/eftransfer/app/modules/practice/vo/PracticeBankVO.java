package com.huashi.eftransfer.app.modules.practice.vo;

import java.util.List;

public record PracticeBankVO(
        String bankCode,
        String name,
        String description,
        Integer totalQuestionCount,
        List<PracticeSectionVO> sections
) {
}

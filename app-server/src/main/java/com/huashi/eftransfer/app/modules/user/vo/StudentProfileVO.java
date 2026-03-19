package com.huashi.eftransfer.app.modules.user.vo;

public record StudentProfileVO(
        String studentNo,
        String gradeName,
        String englishLevel,
        String frenchLevel,
        Integer compositeScore
) {
}

package com.huashi.eftransfer.shared.ai;

public record RagErrorTypeStat(
        String code,
        String label,
        Long count,
        Double ratio
) {
}

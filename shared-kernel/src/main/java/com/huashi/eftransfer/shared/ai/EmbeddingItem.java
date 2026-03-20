package com.huashi.eftransfer.shared.ai;

import java.util.List;

public record EmbeddingItem(
        int index,
        String text,
        List<Double> embedding
) {
}

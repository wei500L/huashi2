package com.huashi.eftransfer.ai.integration.provider.dto;

import java.util.List;

public record RerankResponse(
        String provider,
        String model,
        List<RerankItem> items
) {

    public record RerankItem(
            int index,
            double score,
            String document
    ) {
    }
}

package com.huashi.eftransfer.ai.integration.provider;

import com.huashi.eftransfer.ai.integration.provider.dto.RerankRequest;
import com.huashi.eftransfer.ai.integration.provider.dto.RerankResponse;

public interface RerankClient {

    RerankResponse rerank(RerankRequest request);
}

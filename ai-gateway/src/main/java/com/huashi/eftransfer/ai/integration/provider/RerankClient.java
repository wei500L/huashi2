package com.huashi.eftransfer.ai.integration.provider;

import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;

public interface RerankClient {

    RerankResponse rerank(String providerName, RerankRequest request);
}

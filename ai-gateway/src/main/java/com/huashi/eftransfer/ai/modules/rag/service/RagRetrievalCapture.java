package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievalResult;
import org.springframework.stereotype.Component;

@Component
public class RagRetrievalCapture {

    private final ThreadLocal<RagRetrievalResult> holder = new ThreadLocal<>();

    public void store(RagRetrievalResult retrievalResult) {
        holder.set(retrievalResult);
    }

    public RagRetrievalResult consume() {
        RagRetrievalResult retrievalResult = holder.get();
        holder.remove();
        return retrievalResult;
    }
}

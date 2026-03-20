package com.huashi.eftransfer.ai.common.observability;

public class ProviderRequestContextHolder {

    private final ThreadLocal<String> requestIdHolder = new ThreadLocal<>();

    public void setRequestId(String requestId) {
        requestIdHolder.set(requestId);
    }

    public String getRequestId() {
        return requestIdHolder.get();
    }

    public void clear() {
        requestIdHolder.remove();
    }
}

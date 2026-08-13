package com.huashi.eftransfer.app.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public class RateLimitWindow {

    @Min(1)
    private long limit;

    @NotNull
    private Duration window;

    public RateLimitWindow() {
    }

    public RateLimitWindow(long limit, Duration window) {
        this.limit = limit;
        this.window = window;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}

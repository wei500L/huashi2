package com.huashi.eftransfer.app.modules.ai.support;

import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.ai.TokenUsage;

public class AiUsageSummary {

    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private int rerankTokens;

    public void addStructured(StructuredChatResponse response) {
        if (response == null) {
            return;
        }
        addUsage(response.usage());
    }

    public void addChat(ChatResponse response) {
        if (response == null) {
            return;
        }
        addUsage(response.usage());
    }

    public void addRerank(RerankResponse response) {
        if (response == null || response.totalTokens() == null) {
            return;
        }
        rerankTokens += response.totalTokens();
        totalTokens += response.totalTokens();
    }

    public int promptTokens() {
        return promptTokens;
    }

    public int completionTokens() {
        return completionTokens;
    }

    public int totalTokens() {
        return totalTokens;
    }

    public int rerankTokens() {
        return rerankTokens;
    }

    private void addUsage(TokenUsage usage) {
        if (usage == null) {
            return;
        }
        promptTokens += safe(usage.promptTokens());
        completionTokens += safe(usage.completionTokens());
        totalTokens += safe(usage.totalTokens());
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}

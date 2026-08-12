package com.huashi.eftransfer.ai.modules.rag.integration;

import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeIngestionService;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSourceTypes;
import com.huashi.eftransfer.shared.ai.RagReindexJobResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Indexes the student self-practice question bank words at startup so the
 * practice tutoring scenes can ground on the bank explanations. The sync is
 * idempotent (FULL mode + content-hash upsert) and tolerant: failures are
 * retried briefly and then only logged, because the tutoring scenes degrade
 * to rule fallback when word evidence is missing.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 50)
public class PracticeWordKnowledgeStartupSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PracticeWordKnowledgeStartupSync.class);
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(20);

    private final KnowledgeIngestionService knowledgeIngestionService;

    public PracticeWordKnowledgeStartupSync(KnowledgeIngestionService knowledgeIngestionService) {
        this.knowledgeIngestionService = knowledgeIngestionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // The app-server export may not be ready yet at ai-gateway startup
        // (compose starts ai-gateway before app-server), so the sync runs on a
        // daemon thread with retries instead of blocking startup.
        Thread syncThread = new Thread(this::sync, "practice-word-knowledge-sync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void sync() {
        RagReindexRequest request = new RagReindexRequest(
                "FULL",
                List.of(KnowledgeSourceTypes.PRACTICE_WORD),
                List.of(),
                false
        );
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                RagReindexJobResponse job = knowledgeIngestionService.submitAndAwait(request);
                log.info("event=practice_word_knowledge_sync_ready status={} documentsProcessed={} message={}",
                        job.status(), job.stats() == null ? null : job.stats().get("documentsProcessed"),
                        job.errorMessage() == null ? "" : job.errorMessage());
                return;
            } catch (Exception exception) {
                log.warn("event=practice_word_knowledge_sync_failed attempt={}/{} message={}",
                        attempt, MAX_ATTEMPTS, exception.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY.toMillis());
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}

package com.huashi.eftransfer.ai.modules.rag.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.repository.IntegrationConsumeRecordRepository;
import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeIngestionService;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSourceTypes;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.event.LexicalKnowledgeChangedEvent;
import com.huashi.eftransfer.shared.event.PlatformEventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LexicalKnowledgeChangedEventListener {

    private static final Logger log = LoggerFactory.getLogger(LexicalKnowledgeChangedEventListener.class);
    private static final String CONSUMER_NAME = "ai-gateway-lexical-knowledge-sync";

    private final ObjectMapper objectMapper;
    private final KnowledgeIngestionService knowledgeIngestionService;
    private final IntegrationConsumeRecordRepository integrationConsumeRecordRepository;
    private final RetryTemplate knowledgeSyncRetryTemplate;

    public LexicalKnowledgeChangedEventListener(
            ObjectMapper objectMapper,
            KnowledgeIngestionService knowledgeIngestionService,
            IntegrationConsumeRecordRepository integrationConsumeRecordRepository,
            RetryTemplate knowledgeSyncRetryTemplate
    ) {
        this.objectMapper = objectMapper;
        this.knowledgeIngestionService = knowledgeIngestionService;
        this.integrationConsumeRecordRepository = integrationConsumeRecordRepository;
        this.knowledgeSyncRetryTemplate = knowledgeSyncRetryTemplate;
    }

    @RabbitListener(queues = PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_QUEUE)
    public void onMessage(byte[] payload) {
        LexicalKnowledgeChangedEvent event = parse(payload);
        if (integrationConsumeRecordRepository.isSucceeded(CONSUMER_NAME, event.eventId())) {
            log.info("event=lexical_knowledge_changed_duplicate_skipped eventId={}", event.eventId());
            return;
        }

        RagReindexRequest request = new RagReindexRequest(
                "FULL",
                List.of(
                        KnowledgeSourceTypes.LEXICAL_PAIR,
                        KnowledgeSourceTypes.LEXICAL_SENSE,
                        KnowledgeSourceTypes.LEXICAL_EXAMPLE
                ),
                event.sourceIds().stream().map(String::valueOf).toList(),
                Boolean.FALSE
        );

        AtomicInteger attemptCounter = new AtomicInteger();
        try {
            knowledgeSyncRetryTemplate.execute(context -> {
                int attempt = attemptCounter.incrementAndGet();
                try {
                    knowledgeIngestionService.submitAndAwait(request);
                    return null;
                } catch (RuntimeException exception) {
                    log.warn("event=lexical_knowledge_changed_consume_retry eventId={} attempt={} message={}",
                            event.eventId(), attempt, exception.getMessage());
                    throw exception;
                }
            });
            integrationConsumeRecordRepository.markSucceeded(CONSUMER_NAME, event.eventId(), event.sourceType());
            log.info("event=lexical_knowledge_changed_consumed eventId={} sourceIds={} attempt={}",
                    event.eventId(), event.sourceIds(), attemptCounter.get());
            return;
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null ? "Unknown lexical knowledge sync failure" : exception.getMessage();
            integrationConsumeRecordRepository.markFailed(CONSUMER_NAME, event.eventId(), event.sourceType(), message);
            throw new AmqpRejectAndDontRequeueException(message, exception);
        }
    }

    private LexicalKnowledgeChangedEvent parse(byte[] payload) {
        try {
            return objectMapper.readValue(payload, LexicalKnowledgeChangedEvent.class);
        } catch (IOException exception) {
            throw new AmqpRejectAndDontRequeueException("Failed to deserialize lexical knowledge change event", exception);
        }
    }
}

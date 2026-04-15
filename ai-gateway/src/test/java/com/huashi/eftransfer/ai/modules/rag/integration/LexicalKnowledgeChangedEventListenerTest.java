package com.huashi.eftransfer.ai.modules.rag.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.repository.IntegrationConsumeRecordRepository;
import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeIngestionService;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSourceTypes;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.event.LexicalKnowledgeChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LexicalKnowledgeChangedEventListenerTest {

    @Mock
    private KnowledgeIngestionService knowledgeIngestionService;

    @Mock
    private IntegrationConsumeRecordRepository integrationConsumeRecordRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private LexicalKnowledgeChangedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new LexicalKnowledgeChangedEventListener(
                objectMapper,
                knowledgeIngestionService,
                integrationConsumeRecordRepository
        );
    }

    @Test
    void shouldSkipDuplicateSucceededEvent() throws Exception {
        byte[] payload = eventPayload("evt-duplicate");
        when(integrationConsumeRecordRepository.isSucceeded("ai-gateway-lexical-knowledge-sync", "evt-duplicate"))
                .thenReturn(true);

        listener.onMessage(payload);

        verify(integrationConsumeRecordRepository).isSucceeded("ai-gateway-lexical-knowledge-sync", "evt-duplicate");
        verifyNoInteractions(knowledgeIngestionService);
        verify(integrationConsumeRecordRepository, never()).markSucceeded(any(), any(), any());
        verify(integrationConsumeRecordRepository, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    void shouldRetryAndMarkSucceededAfterTransientFailures() throws Exception {
        byte[] payload = eventPayload("evt-retry-success");
        when(integrationConsumeRecordRepository.isSucceeded("ai-gateway-lexical-knowledge-sync", "evt-retry-success"))
                .thenReturn(false);
        doThrow(new RuntimeException("provider down"))
                .doThrow(new RuntimeException("provider still down"))
                .doReturn(null)
                .when(knowledgeIngestionService).submitAndAwait(any(RagReindexRequest.class));

        listener.onMessage(payload);

        ArgumentCaptor<RagReindexRequest> requestCaptor = ArgumentCaptor.forClass(RagReindexRequest.class);
        verify(knowledgeIngestionService, times(3)).submitAndAwait(requestCaptor.capture());
        RagReindexRequest request = requestCaptor.getValue();
        assertThat(request.mode()).isEqualTo("FULL");
        assertThat(request.sourceTypes()).containsExactly(
                KnowledgeSourceTypes.LEXICAL_PAIR,
                KnowledgeSourceTypes.LEXICAL_SENSE,
                KnowledgeSourceTypes.LEXICAL_EXAMPLE
        );
        assertThat(request.sourceIds()).containsExactly("1001", "1002");
        assertThat(request.forceReembed()).isFalse();
        verify(integrationConsumeRecordRepository).markSucceeded(
                "ai-gateway-lexical-knowledge-sync",
                "evt-retry-success",
                "LEXICAL_PAIR"
        );
        verify(integrationConsumeRecordRepository, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    void shouldMarkFailedAndRejectAfterRetryExhausted() throws Exception {
        byte[] payload = eventPayload("evt-failed");
        when(integrationConsumeRecordRepository.isSucceeded("ai-gateway-lexical-knowledge-sync", "evt-failed"))
                .thenReturn(false);
        doThrow(new RuntimeException("provider unavailable"))
                .when(knowledgeIngestionService).submitAndAwait(any(RagReindexRequest.class));

        AmqpRejectAndDontRequeueException exception = assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> listener.onMessage(payload)
        );

        assertThat(exception.getMessage()).contains("provider unavailable");
        verify(knowledgeIngestionService, times(3)).submitAndAwait(any(RagReindexRequest.class));
        verify(integrationConsumeRecordRepository).markFailed(
                "ai-gateway-lexical-knowledge-sync",
                "evt-failed",
                "LEXICAL_PAIR",
                "provider unavailable"
        );
        verify(integrationConsumeRecordRepository, never()).markSucceeded(any(), any(), any());
    }

    @Test
    void shouldRejectMalformedPayload() {
        AmqpRejectAndDontRequeueException exception = assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> listener.onMessage("not-json".getBytes())
        );

        assertThat(exception.getMessage()).contains("Failed to deserialize lexical knowledge change event");
        verifyNoInteractions(knowledgeIngestionService, integrationConsumeRecordRepository);
    }

    private byte[] eventPayload(String eventId) throws Exception {
        return objectMapper.writeValueAsBytes(new LexicalKnowledgeChangedEvent(
                eventId,
                1,
                "LEXICAL_PAIR",
                List.of(1001L, 1002L),
                OffsetDateTime.parse("2026-03-20T00:00:00Z"),
                "trace-knowledge-sync"
        ));
    }
}

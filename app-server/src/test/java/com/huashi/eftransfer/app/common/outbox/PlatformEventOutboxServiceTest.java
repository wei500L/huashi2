package com.huashi.eftransfer.app.common.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformEventOutboxServiceTest {

    @Mock
    private PlatformEventOutboxRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private PlatformEventOutboxProperties properties;
    private PlatformEventOutboxService service;

    @BeforeEach
    void setUp() {
        properties = new PlatformEventOutboxProperties();
        properties.setConfirmTimeout(Duration.ofSeconds(5));
        service = new PlatformEventOutboxService(
                repository,
                properties,
                rabbitTemplate,
                new ObjectMapper()
        );
    }

    @Test
    void shouldMarkMessageFailedWhenBrokerReturnsMessage() {
        PlatformEventOutboxRecord record = new PlatformEventOutboxRecord(
                7L,
                "evt-returned",
                "LexicalKnowledgeChangedEvent",
                "ef.transfer.platform.events",
                "knowledge.lexical.changed.v1",
                "{\"eventId\":\"evt-returned\"}",
                null,
                "trace-returned",
                PlatformEventOutboxStatus.PENDING,
                0,
                OffsetDateTime.parse("2026-04-18T00:00:00Z"),
                null,
                null,
                null,
                OffsetDateTime.parse("2026-04-18T00:00:00Z"),
                OffsetDateTime.parse("2026-04-18T00:00:00Z")
        );
        when(repository.claimBatch(eq(properties.getBatchSize()), eq(properties.getStuckThreshold())))
                .thenReturn(List.of(record));
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3, CorrelationData.class);
            correlationData.setReturned(new ReturnedMessage(
                    invocation.getArgument(2, Message.class),
                    312,
                    "NO_ROUTE",
                    record.exchangeName(),
                    record.routingKey()
            ));
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(eq(record.exchangeName()), eq(record.routingKey()), any(Message.class), any(CorrelationData.class));

        OffsetDateTime lowerBound = OffsetDateTime.now(ZoneOffset.UTC).plus(properties.getInitialBackoff()).minusSeconds(1);
        int relayed = service.relayDueMessages();
        OffsetDateTime upperBound = OffsetDateTime.now(ZoneOffset.UTC).plus(properties.getInitialBackoff()).plusSeconds(1);

        assertThat(relayed).isEqualTo(1);
        ArgumentCaptor<OffsetDateTime> nextAttemptCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).markFailed(eq(record.id()), nextAttemptCaptor.capture(), eq("Rabbit message was returned: NO_ROUTE"));
        assertThat(nextAttemptCaptor.getValue()).isBetween(lowerBound, upperBound);
    }
}

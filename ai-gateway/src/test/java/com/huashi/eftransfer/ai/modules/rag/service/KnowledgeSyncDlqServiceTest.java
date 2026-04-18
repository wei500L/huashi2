package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.shared.ai.RagKnowledgeSyncDlqReplayResponse;
import com.huashi.eftransfer.shared.event.PlatformEventTopics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSyncDlqServiceTest {

    @Test
    void shouldReplayDlqMessagesAndStripBrokerDeathHeaders() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setMessageId("evt-1001");
        properties.setHeader("eventType", "LEXICAL_PAIR");
        properties.setHeader("traceId", "trace-knowledge-sync");
        properties.setHeader("x-death", "broker-managed");
        Message firstMessage = new Message("{\"eventId\":\"evt-1001\"}".getBytes(StandardCharsets.UTF_8), properties);

        when(rabbitTemplate.receive(PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ))
                .thenReturn(firstMessage)
                .thenReturn(null);

        KnowledgeSyncDlqService service = new KnowledgeSyncDlqService(rabbitTemplate);
        RagKnowledgeSyncDlqReplayResponse response = service.replay(5);

        assertThat(response.requestedLimit()).isEqualTo(5);
        assertThat(response.replayedCount()).isEqualTo(1);
        assertThat(response.drained()).isTrue();

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                org.mockito.ArgumentMatchers.eq(PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(PlatformEventTopics.LEXICAL_KNOWLEDGE_CHANGED_ROUTING_KEY),
                messageCaptor.capture()
        );
        verify(rabbitTemplate, times(2)).receive(PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ);

        Message replayed = messageCaptor.getValue();
        assertThat(replayed.getMessageProperties().getMessageId()).isEqualTo("evt-1001");
        assertThat(replayed.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(replayed.getMessageProperties().getHeaders()).containsEntry("eventType", "LEXICAL_PAIR");
        assertThat(replayed.getMessageProperties().getHeaders()).containsEntry("traceId", "trace-knowledge-sync");
        assertThat(replayed.getMessageProperties().getHeaders()).doesNotContainKey("x-death");
    }
}

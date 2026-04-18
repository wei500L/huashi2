package com.huashi.eftransfer.ai.modules.rag.service;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.huashi.eftransfer.shared.ai.RagKnowledgeSyncDlqReplayResponse;
import com.huashi.eftransfer.shared.event.PlatformEventTopics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSyncDlqServiceTest {

    @Test
    void shouldReplayDlqMessagesAndStripBrokerDeathHeaders() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        Channel channel = mock(Channel.class);
        stubExecute(rabbitTemplate, channel);

        byte[] body = "{\"eventId\":\"evt-1001\"}".getBytes(StandardCharsets.UTF_8);
        GetResponse firstMessage = new GetResponse(
                new Envelope(7L, false, "", PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ),
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .messageId("evt-1001")
                        .headers(Map.of(
                                "eventType", "LEXICAL_PAIR",
                                "traceId", "trace-knowledge-sync",
                                "x-death", "broker-managed"
                        ))
                        .build(),
                body,
                0
        );

        when(channel.basicGet(PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ, false))
                .thenReturn(firstMessage)
                .thenReturn(null);

        KnowledgeSyncDlqService service = new KnowledgeSyncDlqService(rabbitTemplate);
        RagKnowledgeSyncDlqReplayResponse response = service.replay(5);

        assertThat(response.requestedLimit()).isEqualTo(5);
        assertThat(response.replayedCount()).isEqualTo(1);
        assertThat(response.drained()).isTrue();

        ArgumentCaptor<AMQP.BasicProperties> propertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(channel).basicPublish(
                eq(PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE),
                eq(PlatformEventTopics.LEXICAL_KNOWLEDGE_CHANGED_ROUTING_KEY),
                propertiesCaptor.capture(),
                bodyCaptor.capture()
        );
        verify(channel).basicAck(7L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());

        AMQP.BasicProperties replayed = propertiesCaptor.getValue();
        assertThat(replayed.getMessageId()).isEqualTo("evt-1001");
        assertThat(replayed.getContentType()).isEqualTo("application/json");
        assertThat(replayed.getHeaders()).containsEntry("eventType", "LEXICAL_PAIR");
        assertThat(replayed.getHeaders()).containsEntry("traceId", "trace-knowledge-sync");
        assertThat(replayed.getHeaders()).doesNotContainKey("x-death");
        assertThat(bodyCaptor.getValue()).isEqualTo(body);
    }

    @Test
    void shouldNackAndPreserveDlqMessageWhenReplayPublishFails() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        Channel channel = mock(Channel.class);
        stubExecute(rabbitTemplate, channel);

        byte[] body = "{\"eventId\":\"evt-1002\"}".getBytes(StandardCharsets.UTF_8);
        GetResponse message = new GetResponse(
                new Envelope(9L, false, "", PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ),
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .messageId("evt-1002")
                        .headers(Map.of("eventType", "LEXICAL_PAIR"))
                        .build(),
                body,
                0
        );

        when(channel.basicGet(PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ, false))
                .thenReturn(message);
        doThrow(new IllegalStateException("publish failed"))
                .when(channel)
                .basicPublish(any(String.class), any(String.class), any(AMQP.BasicProperties.class), any(byte[].class));

        KnowledgeSyncDlqService service = new KnowledgeSyncDlqService(rabbitTemplate);

        assertThatThrownBy(() -> service.replay(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("publish failed");

        verify(channel).basicNack(9L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private void stubExecute(RabbitTemplate rabbitTemplate, Channel channel) {
        when(rabbitTemplate.execute(any(ChannelCallback.class)))
                .thenAnswer(invocation -> ((ChannelCallback<?>) invocation.getArgument(0)).doInRabbit(channel));
    }
}

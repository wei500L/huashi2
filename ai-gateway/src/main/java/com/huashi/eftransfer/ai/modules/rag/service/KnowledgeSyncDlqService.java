package com.huashi.eftransfer.ai.modules.rag.service;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;
import com.huashi.eftransfer.shared.ai.RagKnowledgeSyncDlqReplayResponse;
import com.huashi.eftransfer.shared.event.PlatformEventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class KnowledgeSyncDlqService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSyncDlqService.class);
    private static final int DEFAULT_REPLAY_LIMIT = 50;

    private final RabbitTemplate rabbitTemplate;
    private final DefaultMessagePropertiesConverter messagePropertiesConverter = new DefaultMessagePropertiesConverter();

    public KnowledgeSyncDlqService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public RagKnowledgeSyncDlqReplayResponse replay(Integer limit) {
        int requestedLimit = limit == null ? DEFAULT_REPLAY_LIMIT : limit;
        int replayedCount = 0;
        boolean drained = false;

        for (int index = 0; index < requestedLimit; index++) {
            Boolean replayed = rabbitTemplate.execute(channel -> {
                GetResponse response = channel.basicGet(PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ, false);
                if (response == null) {
                    return null;
                }
                Message message = new Message(
                        response.getBody(),
                        messagePropertiesConverter.toMessageProperties(
                                response.getProps(),
                                response.getEnvelope(),
                                StandardCharsets.UTF_8.name()
                        )
                );
                Message replayMessage = toReplayMessage(message);
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                try {
                    AMQP.BasicProperties basicProperties = messagePropertiesConverter.fromMessageProperties(
                            replayMessage.getMessageProperties(),
                            StandardCharsets.UTF_8.name()
                    );
                    channel.basicPublish(
                            PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE,
                            PlatformEventTopics.LEXICAL_KNOWLEDGE_CHANGED_ROUTING_KEY,
                            basicProperties,
                            replayMessage.getBody()
                    );
                    channel.basicAck(deliveryTag, false);
                    return Boolean.TRUE;
                } catch (Exception ex) {
                    channel.basicNack(deliveryTag, false, true);
                    throw ex;
                }
            });
            if (replayed == null) {
                drained = true;
                break;
            }
            replayedCount++;
        }

        log.info("event=knowledge_sync_dlq_replayed requestedLimit={} replayedCount={} drained={}",
                requestedLimit, replayedCount, drained);
        return new RagKnowledgeSyncDlqReplayResponse(requestedLimit, replayedCount, drained);
    }

    private Message toReplayMessage(Message message) {
        MessageProperties source = message.getMessageProperties();
        MessageProperties target = new MessageProperties();
        if (source.getContentType() != null) {
            target.setContentType(source.getContentType());
        }
        if (source.getContentEncoding() != null) {
            target.setContentEncoding(source.getContentEncoding());
        }
        if (source.getDeliveryMode() != null) {
            target.setDeliveryMode(source.getDeliveryMode());
        }
        if (source.getMessageId() != null) {
            target.setMessageId(source.getMessageId());
        }
        if (source.getTimestamp() != null) {
            target.setTimestamp(source.getTimestamp());
        }
        copyHeader(source, target, "eventType");
        copyHeader(source, target, "traceId");
        return new Message(message.getBody(), target);
    }

    private void copyHeader(MessageProperties source, MessageProperties target, String headerName) {
        Object value = source.getHeaders().get(headerName);
        if (value != null) {
            target.setHeader(headerName, value);
        }
    }
}

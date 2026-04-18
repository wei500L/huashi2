package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.shared.ai.RagKnowledgeSyncDlqReplayResponse;
import com.huashi.eftransfer.shared.event.PlatformEventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSyncDlqService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSyncDlqService.class);
    private static final int DEFAULT_REPLAY_LIMIT = 50;

    private final RabbitTemplate rabbitTemplate;

    public KnowledgeSyncDlqService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public RagKnowledgeSyncDlqReplayResponse replay(Integer limit) {
        int requestedLimit = limit == null ? DEFAULT_REPLAY_LIMIT : limit;
        int replayedCount = 0;
        boolean drained = false;

        for (int index = 0; index < requestedLimit; index++) {
            Message message = rabbitTemplate.receive(PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ);
            if (message == null) {
                drained = true;
                break;
            }
            rabbitTemplate.send(
                    PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE,
                    PlatformEventTopics.LEXICAL_KNOWLEDGE_CHANGED_ROUTING_KEY,
                    toReplayMessage(message)
            );
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

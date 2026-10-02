package com.fooddelivery.chat.listener;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.service.ChatEventBroadcaster;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.common.event.OutboxEvent;
import com.fooddelivery.common.entity.IdempotencyKey;
import com.fooddelivery.common.repository.IIdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Component
@lombok.extern.slf4j.Slf4j
public class RefundDecisionListener {

    private final ChatEventBroadcaster chatEventBroadcaster;
    private final ChatMessageService messageService;
    private final IIdempotencyKeyRepository idempotencyKeyRepository;
    private final TransactionTemplate transactionTemplate;

    public RefundDecisionListener(ChatEventBroadcaster chatEventBroadcaster, ChatMessageService messageService, IIdempotencyKeyRepository idempotencyKeyRepository, TransactionTemplate transactionTemplate) {
        this.chatEventBroadcaster = chatEventBroadcaster;
        this.messageService = messageService;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @KafkaListener(topics = "chat-events", groupId = "chat-service-refund-group-refunddecisionlistener")
    public void handleRefundRecord(String payload, @org.springframework.messaging.handler.annotation.Headers java.util.Map<String, Object> headers) {
        String type = com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers, "eventType");
        if (!"CHAT_REFUND_QUOTE_RESPONSE".equals(type) && !"CHAT_REFUND_DECISION".equals(type)
                && !"CHAT_REFUND_ERROR".equals(type)) return;
        String id = com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers, "eventId");
        String sessionId = com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers,
                org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY);
        if (id == null || id.isBlank() || sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Chat refund response requires eventId and session key");
        }
        UUID.fromString(sessionId);
        handleRefundDecision(OutboxEvent.builder().id(id).type(type).aggregateId(sessionId).payload(payload).build(), headers);
    }

    public void handleRefundDecision(OutboxEvent event, @org.springframework.messaging.handler.annotation.Headers java.util.Map<String, Object> headers) {
        try {
            if ("CHAT_REFUND_QUOTE_RESPONSE".equals(event.getType()) || "CHAT_REFUND_DECISION".equals(event.getType()) || "CHAT_REFUND_ERROR".equals(event.getType())) {
                
                String extractedEventId = com.fooddelivery.common.util.KafkaHeaderUtils.extractHeaderValue(headers, "eventId");
                final String resolvedEventId;
                if (extractedEventId == null) {
                    resolvedEventId = event.getId() != null ? event.getId().toString() : UUID.randomUUID().toString();
                } else {
                    resolvedEventId = extractedEventId;
                }
                
                String idempotencyKeyStr = "processed_event:" + resolvedEventId;

                transactionTemplate.execute(status -> {
                    if (idempotencyKeyRepository.existsById(idempotencyKeyStr)) {
                        log.info("Duplicate refund decision event ignored: {}", idempotencyKeyStr);
                        return null;
                    }
                    idempotencyKeyRepository.save(new IdempotencyKey(idempotencyKeyStr));

                    UUID sessionId = UUID.fromString(event.getAggregateId());
                    String senderId = "SYSTEM";
                    String messageType = "CHAT_REFUND_QUOTE_RESPONSE".equals(event.getType()) ? "REFUND_QUOTE_RESPONSE" : ("CHAT_REFUND_DECISION".equals(event.getType()) ? "REFUND_DECISION" : "REFUND_ERROR");
                    String content = event.getPayload();
                    
                    // Save and broadcast
                    ChatMessageDto saved = messageService.saveMessage(sessionId, senderId, content, messageType);
                    chatEventBroadcaster.broadcastMessage(sessionId, saved);
                    log.info("Processed refund decision for session {}: {}", sessionId, messageType);
                    return null;
                });
            }
        } catch (Exception e) {
            // Let the Kafka error handler retry or retain this event in the DLT.
            // Returning normally would acknowledge a response that was never stored.
            throw new IllegalStateException("Failed to process refund response " + event.getId(), e);
        }
    }
}

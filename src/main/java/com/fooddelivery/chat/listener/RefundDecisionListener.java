package com.fooddelivery.chat.listener;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.common.event.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@lombok.extern.slf4j.Slf4j
public class RefundDecisionListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageService messageService;

    public RefundDecisionListener(SimpMessageSendingOperations messagingTemplate, ChatMessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    @KafkaListener(topics = "chat-events", groupId = "chat-service-refund-group")
    public void handleRefundDecision(OutboxEvent event) {
        try {
            if ("CHAT_REFUND_QUOTE_RESPONSE".equals(event.getType()) || "CHAT_REFUND_DECISION".equals(event.getType()) || "CHAT_REFUND_ERROR".equals(event.getType())) {
                UUID sessionId = UUID.fromString(event.getAggregateId());
                String senderId = "SYSTEM";
                String messageType = "CHAT_REFUND_QUOTE_RESPONSE".equals(event.getType()) ? "REFUND_QUOTE_RESPONSE" : ("CHAT_REFUND_DECISION".equals(event.getType()) ? "REFUND_DECISION" : "REFUND_ERROR");
                String content = event.getPayload();
                
                // Save and broadcast
                ChatMessageDto saved = messageService.saveMessage(sessionId, senderId, content, messageType);
                messagingTemplate.convertAndSend("/topic/chat/" + sessionId, saved);
                log.info("Processed refund decision for session {}: {}", sessionId, messageType);
            }
        } catch (Exception e) {
            log.error("Failed to process refund decision event: {}", event, e);
        }
    }
}

package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.entity.ChatMessage;
import com.fooddelivery.chat.entity.SessionParticipant;
import com.fooddelivery.chat.repository.ChatMessageRepository;
import com.fooddelivery.chat.repository.SessionParticipantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fooddelivery.common.outbox.entity.OutboxEventEntity;
import com.fooddelivery.common.outbox.repository.OutboxEventRepository;
import com.fooddelivery.common.constants.AggregateType;
import com.fooddelivery.common.constants.EventType;
import com.fooddelivery.common.enums.OutboxStatus;
import java.time.LocalDateTime;

@Service
@lombok.extern.slf4j.Slf4j
public class ChatMessageService {
    @java.lang.SuppressWarnings("all")

    private final ChatMessageRepository messageRepository;
    private final SessionParticipantRepository participantRepository;
    private final OutboxEventRepository outboxEventRepository;

    /**
     * Save a message to the database and return the enriched DTO.
     */
    @Transactional
    public ChatMessageDto saveMessage(UUID sessionId, String senderId, String content, String messageType) {
        // Retrieve the actual participant to get trusted name and type
        String trustedSenderName;
        String trustedSenderType;
        
        if ("SYSTEM".equals(senderId)) {
            trustedSenderName = "System";
            trustedSenderType = "SYSTEM";
        } else {
            SessionParticipant participant = participantRepository.findByChatSessionId(sessionId).stream().filter(p -> p.getUserId().equals(senderId)).findFirst().orElseThrow(() -> new IllegalArgumentException("Sender is not a participant in this session"));
            trustedSenderName = participant.getDisplayName();
            trustedSenderType = participant.getEntityType();
        }
        
        ChatMessage message = ChatMessage.builder().sessionId(sessionId).senderId(senderId).content(content).messageType(messageType != null ? messageType : "TEXT").createdAt(Instant.now()).build();
        message = messageRepository.save(message);
        log.info("Saved message {} in session {} from {}", message.getId(), sessionId, senderId);
        
        if ("REFUND_QUOTE_REQUEST".equals(messageType) || "REFUND_REQUEST".equals(messageType)) {
            EventType eventType = "REFUND_QUOTE_REQUEST".equals(messageType) 
                    ? EventType.CHAT_REFUND_QUOTE_REQUESTED 
                    : EventType.CHAT_REFUND_REQUESTED;
                    
            OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateType(AggregateType.CHAT_SESSION)
                .aggregateId(sessionId.toString())
                .eventType(eventType)
                .payload(content)
                .createdAt(LocalDateTime.now())
                .status(OutboxStatus.UNPROCESSED)
                .retryCount(0)
                .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Saved outbox event {} for message type {}", outboxEvent.getId(), messageType);
        }

        return ChatMessageDto.builder().id(message.getId()).sessionId(message.getSessionId()).senderId(message.getSenderId()).senderName(trustedSenderName).senderType(trustedSenderType).messageType(message.getMessageType()).content(message.getContent()).imageUrl("IMAGE".equals(message.getMessageType()) ? message.getContent() : null).timestamp(message.getCreatedAt()).build();
    }

    /**
     * Get paginated message history for a session, enriched with sender names.
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessageHistory(UUID sessionId, int page, int size) {
        // Build a lookup of userId -> displayName from participants
        Map<String, SessionParticipant> participantMap = participantRepository.findByChatSessionId(sessionId).stream().collect(Collectors.toMap(SessionParticipant::getUserId, p -> p, (a, b) -> a));
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, PageRequest.of(page, size)).map(msg -> {
            SessionParticipant sender = participantMap.get(msg.getSenderId());
            return ChatMessageDto.builder().id(msg.getId()).sessionId(msg.getSessionId()).senderId(msg.getSenderId()).senderName(sender != null ? sender.getDisplayName() : msg.getSenderId()).senderType(sender != null ? sender.getEntityType() : "UNKNOWN").messageType(msg.getMessageType()).content(msg.getContent()).imageUrl("IMAGE".equals(msg.getMessageType()) ? msg.getContent() : null).timestamp(msg.getCreatedAt()).build();
        });
    }

    /**
     * Count the number of images currently in a session for a specific user.
     */
    @Transactional(readOnly = true)
    public long countImagesInSessionByUser(UUID sessionId, String senderId) {
        return messageRepository.countBySessionIdAndSenderIdAndMessageType(sessionId, senderId, "IMAGE");
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessageService(final ChatMessageRepository messageRepository, final SessionParticipantRepository participantRepository, final OutboxEventRepository outboxEventRepository) {
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.outboxEventRepository = outboxEventRepository;
    }
}

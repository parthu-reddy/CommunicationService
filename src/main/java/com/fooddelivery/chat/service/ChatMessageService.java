package com.fooddelivery.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.entity.ChatMessage;
import com.fooddelivery.chat.entity.SessionParticipant;
import com.fooddelivery.chat.repository.ChatMessageRepository;
import com.fooddelivery.chat.repository.ChatSessionRepository;
import com.fooddelivery.chat.repository.SessionParticipantRepository;
import com.fooddelivery.common.event.ChatRefundRequestedEvent;
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

@Service
@lombok.extern.slf4j.Slf4j
public class ChatMessageService {
    @java.lang.SuppressWarnings("all")

    private final ChatMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save a message to the database and return the enriched DTO.
     */
    @Transactional
    public ChatMessageDto saveMessage(UUID sessionId, String senderId, String content, String messageType) {
        SenderIdentity sender = resolveSessionSender(sessionId, senderId);
        return saveMessage(sessionId, senderId, content, messageType, sender);
    }

    /**
     * Persists a support-moderator message without making the administrator a durable order
     * participant. Controllers call this only after a role check.
     */
    @Transactional
    public ChatMessageDto saveSupportModeratorMessage(UUID sessionId, String senderId, String content, String messageType) {
        if (senderId == null || senderId.isBlank()) {
            throw new IllegalArgumentException("Support moderator identity is required");
        }
        return saveMessage(sessionId, senderId, content, messageType,
                new SenderIdentity("Support administrator", "SUPPORT_MODERATOR"));
    }

    private ChatMessageDto saveMessage(UUID sessionId, String senderId, String content, String messageType, SenderIdentity sender) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .senderId(senderId)
                .senderName(sender.name())
                .senderType(sender.type())
                .content(content)
                .messageType(messageType != null ? messageType : "TEXT")
                .createdAt(Instant.now())
                .build();
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
                .payload(authoritativeRefundPayload(sessionId, content, senderId, sender))
                .createdAt(Instant.now())
                .status(OutboxStatus.UNPROCESSED)
                .retryCount(0)
                .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Saved outbox event {} for message type {}", outboxEvent.getId(), messageType);
        }

        return toDto(message, sender);
    }

    /**
     * Get newest-first paginated history for a session, enriched with sender names.
     * Page zero is the latest window; clients display that window in chronological order.
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessageHistory(UUID sessionId, int page, int size) {
        // Build a lookup of userId -> displayName from participants
        Map<String, SessionParticipant> participantMap = participantRepository.findByChatSessionId(sessionId).stream().collect(Collectors.toMap(SessionParticipant::getUserId, p -> p, (a, b) -> a));
        return messageRepository.findBySessionIdOrderByCreatedAtDescIdDesc(sessionId, PageRequest.of(page, size)).map(msg -> {
            SessionParticipant sender = participantMap.get(msg.getSenderId());
            SenderIdentity senderIdentity = new SenderIdentity(
                    msg.getSenderName() != null ? msg.getSenderName() : (sender != null ? sender.getDisplayName() : msg.getSenderId()),
                    msg.getSenderType() != null ? msg.getSenderType() : (sender != null ? sender.getEntityType() : "UNKNOWN"));
            return toDto(msg, senderIdentity);
        });
    }

    private SenderIdentity resolveSessionSender(UUID sessionId, String senderId) {
        if ("SYSTEM".equals(senderId)) {
            return new SenderIdentity("System", "SYSTEM");
        }
        SessionParticipant participant = participantRepository.findByChatSessionId(sessionId).stream()
                .filter(candidate -> candidate.getUserId().equals(senderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sender is not a participant in this session"));
        return new SenderIdentity(participant.getDisplayName(), participant.getEntityType());
    }

    private ChatMessageDto toDto(ChatMessage message, SenderIdentity sender) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .senderId(message.getSenderId())
                .senderName(sender.name())
                .senderType(sender.type())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .imageUrl("IMAGE".equals(message.getMessageType()) ? message.getContent() : null)
                .timestamp(message.getCreatedAt())
                .build();
    }

    private record SenderIdentity(String name, String type) {
    }

    /**
     * Refund commands reach a financial consumer through the outbox. Preserve the client-visible
     * message body as chat history, but replace the actor data in the event with the canonical
     * session sender that was resolved server-side above.
     */
    private String authoritativeRefundPayload(UUID sessionId, String content, String senderId, SenderIdentity sender) {
        try {
            ChatRefundRequestedEvent event = objectMapper.readValue(content, ChatRefundRequestedEvent.class);
            com.fooddelivery.chat.entity.ChatSession chatSession = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Chat session does not exist"));
            if (!"ORDER".equals(chatSession.getSessionType())) {
                throw new IllegalArgumentException("Refund commands require an order chat session");
            }
            event.setOrderId(UUID.fromString(chatSession.getReferenceId()));
            // The ticket customer is derived from the order by the consumer. Do not propagate a
            // browser-supplied customer identity onto a financial event at all.
            event.setCustomerId(null);
            event.setActorId(senderId);
            event.setActorType(sender.type());
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Refund request payload must be valid JSON", exception);
        }
    }

    /**
     * Count the number of images currently in a session for a specific user.
     */
    @Transactional(readOnly = true)
    public long countImagesInSessionByUser(UUID sessionId, String senderId) {
        return messageRepository.countBySessionIdAndSenderIdAndMessageType(sessionId, senderId, "IMAGE");
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessageService(final ChatMessageRepository messageRepository, final ChatSessionRepository sessionRepository, final SessionParticipantRepository participantRepository, final OutboxEventRepository outboxEventRepository, final ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }
}

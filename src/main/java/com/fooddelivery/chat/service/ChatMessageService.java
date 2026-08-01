package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.entity.ChatMessage;
import com.fooddelivery.chat.entity.SessionParticipant;
import com.fooddelivery.chat.repository.ChatMessageRepository;
import com.fooddelivery.chat.repository.SessionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatMessageRepository messageRepository;
    private final SessionParticipantRepository participantRepository;

    /**
     * Save a message to the database and return the enriched DTO.
     */
    @Transactional
    public ChatMessageDto saveMessage(UUID sessionId, String senderId, String content,
                                      String messageType, String senderName, String senderType) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .senderId(senderId)
                .content(content)
                .messageType(messageType != null ? messageType : "TEXT")
                .createdAt(Instant.now())
                .build();

        message = messageRepository.save(message);
        log.info("Saved message {} in session {} from {}", message.getId(), sessionId, senderId);

        return ChatMessageDto.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .senderType(senderType)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .timestamp(message.getCreatedAt())
                .build();
    }

    /**
     * Get paginated message history for a session, enriched with sender names.
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessageHistory(UUID sessionId, int page, int size) {
        // Build a lookup of userId -> displayName from participants
        Map<String, SessionParticipant> participantMap = participantRepository
                .findByChatSessionId(sessionId)
                .stream()
                .collect(Collectors.toMap(SessionParticipant::getUserId, p -> p, (a, b) -> a));

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, PageRequest.of(page, size))
                .map(msg -> {
                    SessionParticipant sender = participantMap.get(msg.getSenderId());
                    return ChatMessageDto.builder()
                            .id(msg.getId())
                            .sessionId(msg.getSessionId())
                            .senderId(msg.getSenderId())
                            .senderName(sender != null ? sender.getDisplayName() : msg.getSenderId())
                            .senderType(sender != null ? sender.getEntityType() : "UNKNOWN")
                            .messageType(msg.getMessageType())
                            .content(msg.getContent())
                            .timestamp(msg.getCreatedAt())
                            .build();
                });
    }
}

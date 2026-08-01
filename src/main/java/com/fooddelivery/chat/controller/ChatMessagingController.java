package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.dto.SendMessageRequest;
import com.fooddelivery.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP messaging controller for real-time chat.
 * Handles message sending and typing indicators over WebSocket.
 */
@Controller
@RequiredArgsConstructor
public class ChatMessagingController {

    private static final Logger log = LoggerFactory.getLogger(ChatMessagingController.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageService messageService;

    /**
     * Handles chat messages sent via STOMP.
     * Client sends to: /app/chat.send/{sessionId}
     * Broadcast to:     /topic/chat/{sessionId}
     */
    @MessageMapping("/chat.send/{sessionId}")
    public void handleChatMessage(@DestinationVariable String sessionId,
                                  @Payload SendMessageRequest request,
                                  Principal principal) {
        String senderId = principal != null ? principal.getName() : "anonymous";
        log.info("STOMP message from {} in session {}: {}", senderId, sessionId, request.getContent());

        // Save to database
        ChatMessageDto saved = messageService.saveMessage(
                UUID.fromString(sessionId),
                senderId,
                request.getContent(),
                request.getMessageType(),
                request.getSenderName(),
                request.getSenderType()
        );

        // Broadcast to all subscribers of this session
        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, saved);
    }

    /**
     * Handles typing indicator events.
     * Client sends to: /app/chat.typing/{sessionId}
     * Broadcast to:     /topic/chat/{sessionId}/typing
     * Not persisted — ephemeral UX indicator only.
     */
    @MessageMapping("/chat.typing/{sessionId}")
    public void handleTypingIndicator(@DestinationVariable String sessionId,
                                      Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";

        messagingTemplate.convertAndSend(
                "/topic/chat/" + sessionId + "/typing",
                Map.of("userId", userId, "typing", true)
        );
    }
}

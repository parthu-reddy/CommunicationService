package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.dto.SendMessageRequest;
import com.fooddelivery.chat.service.CallLogService;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP messaging controller for real-time chat.
 * Handles message sending and typing indicators over WebSocket.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessagingController {
private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageService messageService;
    private final CallLogService callLogService;
    private final ChatSessionService sessionService;

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

        try {
            if (!sessionService.isParticipant(UUID.fromString(sessionId), senderId)) {
                log.warn("Rejected message from non-participant {} for session {}", senderId, sessionId);
                return;
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for session: {}", sessionId);
            return;
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            log.warn("Rejected empty message from {} in session {}", senderId, sessionId);
            return;
        }
        if (request.getContent().length() > 10000) {
            log.warn("Rejected overly large message ({} chars) from {} in session {}", request.getContent().length(), senderId, sessionId);
            return;
        }

        // Security Validation: Clients can ONLY send TEXT messages directly over STOMP.
        // IMAGE and AUDIO messages MUST go through their respective secure REST upload controllers
        // to enforce file size, virus scanning (if any), and storage limits.
        if (!"TEXT".equals(request.getMessageType())) {
            log.warn("Rejected non-TEXT message type '{}' from {} in session {}. Must use REST upload endpoints.", request.getMessageType(), senderId, sessionId);
            return;
        }

        // Save to database
        ChatMessageDto saved = messageService.saveMessage(
                UUID.fromString(sessionId),
                senderId,
                request.getContent(),
                request.getMessageType()
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

        try {
            if (!sessionService.isParticipant(UUID.fromString(sessionId), userId)) {
                return; // Silently drop
            }
        } catch (IllegalArgumentException e) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/chat/" + sessionId + "/typing",
                Map.of("userId", userId, "typing", true)
        );
    }

    /**
     * WebRTC Signaling Endpoint. Securely routes SDP offers, answers, and ICE payloads.
     */
    @MessageMapping("/webrtc.signal/{targetUserId}")
    public void processWebRtcSignal(@DestinationVariable String targetUserId,
                                    @Payload com.fooddelivery.chat.dto.WebRtcSignal signal,
                                    Principal principal) {
        if (principal != null) {
            signal.setSenderId(principal.getName());
        }
        signal.setTargetUserId(targetUserId);

        if (signal.getSessionId() == null) {
            log.warn("WebRTC signal rejected: Missing sessionId from {}", signal.getSenderId());
            return;
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(signal.getSessionId());
            if (!sessionService.isParticipant(sessionId, signal.getSenderId()) ||
                !sessionService.isParticipant(sessionId, targetUserId)) {
                log.warn("WebRTC signal rejected: Unauthorized session participants sender={}, target={}", signal.getSenderId(), targetUserId);
                return;
            }
        } catch (IllegalArgumentException e) {
            log.warn("WebRTC signal rejected: Invalid sessionId format {}", signal.getSessionId());
            return;
        }
        
        log.info("Routing WebRTC signal [{}] from {} to {}", signal.getType(), signal.getSenderId(), targetUserId);

        // Intercept signals to update the CallLog state natively in the backend
        if ("OFFER".equals(signal.getType())) {
            callLogService.processOffer(sessionId, signal.getSenderId(), targetUserId);
        } else if ("ANSWER".equals(signal.getType())) {
            callLogService.processAnswer(sessionId, signal.getSenderId());
        } else if ("HANGUP".equals(signal.getType())) {
            callLogService.processHangup(sessionId, signal.getSenderId(), "USER_INITIATED");
        }

        // Routes securely to the specific target user's private queue
        messagingTemplate.convertAndSendToUser(
                targetUserId,
                "/queue/webrtc",
                signal
        );
    }
}

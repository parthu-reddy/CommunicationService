package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.dto.SendMessageRequest;
import com.fooddelivery.chat.service.CallLogService;
import com.fooddelivery.chat.service.ChatEventBroadcaster;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionAccessService;
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
@lombok.extern.slf4j.Slf4j
public class ChatMessagingController {
    @java.lang.SuppressWarnings("all")

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageService messageService;
    private final CallLogService callLogService;
    private final ChatSessionAccessService accessService;
    private final ChatEventBroadcaster chatEventBroadcaster;

    /**
     * Handles chat messages sent via STOMP.
     * Client sends to: /app/chat.send/{sessionId}
     * Delivery:          /user/queue/chat/{sessionId}
     */
    @MessageMapping("/chat.send/{sessionId}")
    public void handleChatMessage(@DestinationVariable String sessionId, @Payload SendMessageRequest request, Principal principal) {
        if (request == null) {
            return;
        }
        String senderId = principal != null ? principal.getName() : "anonymous";
        boolean supportModerator = accessService.isSupportModerator(principal);
        int contentLength = request.getContent() == null ? 0 : request.getContent().length();
        log.info("STOMP message received from {} in session {} ({} chars)",
                senderId, sessionId, contentLength);
        final UUID sessionUuid;
        try {
            sessionUuid = UUID.fromString(sessionId);
            if (!accessService.canAccessSession(sessionUuid, principal)) {
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
        // IMAGE and AUDIO messages go through secure upload controllers. Refund responses and
        // decisions are system-only events produced by the backend; a browser may request only a
        // quote or submit its own request.
        if (!"TEXT".equals(request.getMessageType())
                && !"REFUND_QUOTE_REQUEST".equals(request.getMessageType())
                && !"REFUND_REQUEST".equals(request.getMessageType())) {
            log.warn("Rejected unsupported message type '{}' from {} in session {}", request.getMessageType(), senderId, sessionId);
            return;
        }
        boolean refundCommand = "REFUND_QUOTE_REQUEST".equals(request.getMessageType())
                || "REFUND_REQUEST".equals(request.getMessageType());
        if (refundCommand && !accessService.isCanonicalCustomer(sessionUuid, principal)) {
            log.warn("Rejected refund command from non-customer {} in session {}", senderId, sessionId);
            return;
        }

        // Persist first. The recipient event contains the durable id, timestamp and canonical
        // sender metadata, so reconnect/history cannot disagree with a synthetic optimistic event.
        try {
            ChatMessageDto saved = saveAuthorizedMessage(
                    sessionUuid, senderId, request.getContent(), request.getMessageType(), supportModerator);
            chatEventBroadcaster.broadcastMessage(sessionUuid, saved);
        } catch (IllegalArgumentException exception) {
            log.warn("Rejected invalid message from {} in session {}: {}", senderId, sessionId, exception.getMessage());
        }
    }

    /**
     * Handles typing indicator events.
     * Client sends to: /app/chat.typing/{sessionId}
     * Delivery:          /user/queue/chat/{sessionId}/typing
     * Not persisted — ephemeral UX indicator only.
     */
    @MessageMapping("/chat.typing/{sessionId}")
    public void handleTypingIndicator(@DestinationVariable String sessionId, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        try {
            if (!accessService.canAccessSession(UUID.fromString(sessionId), principal)) {
                return; // Silently drop
            }
        } catch (IllegalArgumentException e) {
            return;
        }
        chatEventBroadcaster.broadcastTyping(UUID.fromString(sessionId), Map.of("userId", userId, "typing", true));
    }

    /**
     * WebRTC Signaling Endpoint. Securely routes SDP offers, answers, and ICE payloads.
     */
    @MessageMapping("/webrtc.signal/{targetUserId}")
    public void processWebRtcSignal(@DestinationVariable String targetUserId, @Payload com.fooddelivery.chat.dto.WebRtcSignal signal, Principal principal) {
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
            
            boolean senderAuthorized = accessService.canAccessSession(sessionId, principal);
            boolean targetAuthorized = accessService.isCanonicalParticipant(sessionId, targetUserId);
            if (!senderAuthorized || !targetAuthorized) {
                log.warn("WebRTC signal rejected: Unauthorized session/order participants sender={}, target={}", signal.getSenderId(), targetUserId);
                return;
            }
        } catch (IllegalArgumentException e) {
            log.warn("WebRTC signal rejected: Invalid sessionId format {}", signal.getSessionId());
            return;
        }
        log.info("Routing WebRTC signal [{}] from {} to {} for session {}", signal.getType(), signal.getSenderId(), targetUserId, sessionId);
        // Intercept signals to update the CallLog state natively in the backend
        if ("OFFER".equals(signal.getType())) {
            callLogService.processOffer(sessionId, signal.getSenderId(), targetUserId);
        } else if ("ANSWER".equals(signal.getType())) {
            callLogService.processAnswer(sessionId, signal.getSenderId());
        } else if ("HANGUP".equals(signal.getType())) {
            callLogService.processHangup(sessionId, signal.getSenderId(), "USER_INITIATED");
        }
        
        // The canonical roster already resolves a restaurant outlet to its owner identity.
        log.info("Sending WebRTC signal to target user ID: {} at destination /queue/webrtc", targetUserId);
        // Routes securely to the specific target user's private queue
        messagingTemplate.convertAndSendToUser(targetUserId, "/queue/webrtc", signal);
        log.info("WebRTC signal sent successfully to target user ID: {}", targetUserId);
    }

    private ChatMessageDto saveAuthorizedMessage(UUID sessionId,
                                                  String senderId,
                                                  String content,
                                                  String messageType,
                                                  boolean supportModerator) {
        return supportModerator
                ? messageService.saveSupportModeratorMessage(sessionId, senderId, content, messageType)
                : messageService.saveMessage(sessionId, senderId, content, messageType);
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessagingController(final SimpMessageSendingOperations messagingTemplate, final ChatMessageService messageService, final CallLogService callLogService, final ChatSessionAccessService accessService, final ChatEventBroadcaster chatEventBroadcaster) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.callLogService = callLogService;
        this.accessService = accessService;
        this.chatEventBroadcaster = chatEventBroadcaster;
    }
}

package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.dto.SendMessageRequest;
import com.fooddelivery.chat.service.CallLogService;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionService;
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
    private final ChatSessionService sessionService;

    /**
     * Handles chat messages sent via STOMP.
     * Client sends to: /app/chat.send/{sessionId}
     * Broadcast to:     /topic/chat/{sessionId}
     */
    @MessageMapping("/chat.send/{sessionId}")
    public void handleChatMessage(@DestinationVariable String sessionId, @Payload SendMessageRequest request, Principal principal) {
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
        if (!"TEXT".equals(request.getMessageType()) &&
            !"REFUND_QUOTE_REQUEST".equals(request.getMessageType()) &&
            !"REFUND_QUOTE_RESPONSE".equals(request.getMessageType()) &&
            !"REFUND_REQUEST".equals(request.getMessageType()) &&
            !"REFUND_DECISION".equals(request.getMessageType())) {
            log.warn("Rejected non-TEXT/REFUND message type \'{}\' from {} in session {}. Must use REST upload endpoints.", request.getMessageType(), senderId, sessionId);
            return;
        }
        // For refund-related messages, we MUST persist synchronously to guarantee OutboxEvent creation
        if (!"TEXT".equals(request.getMessageType())) {
            ChatMessageDto saved = messageService.saveMessage(UUID.fromString(sessionId), senderId, request.getContent(), request.getMessageType());
            messagingTemplate.convertAndSend("/topic/chat/" + sessionId, saved);
            return;
        }

        // For standard text messages, broadcast immediately to reduce perceived latency
        ChatMessageDto immediateDto = ChatMessageDto.builder()
                .id(UUID.randomUUID())
                .sessionId(UUID.fromString(sessionId))
                .senderId(senderId)
                .senderName(senderId) // Fallback, UI usually styles by senderId
                .senderType("USER")
                .messageType("TEXT")
                .content(request.getContent())
                .timestamp(java.time.Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, immediateDto);

        // Save to database asynchronously
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                messageService.saveMessage(UUID.fromString(sessionId), senderId, request.getContent(), request.getMessageType());
            } catch (Exception e) {
                log.error("Failed to save chat message asynchronously", e);
            }
        });
    }

    /**
     * Handles typing indicator events.
     * Client sends to: /app/chat.typing/{sessionId}
     * Broadcast to:     /topic/chat/{sessionId}/typing
     * Not persisted — ephemeral UX indicator only.
     */
    @MessageMapping("/chat.typing/{sessionId}")
    public void handleTypingIndicator(@DestinationVariable String sessionId, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        try {
            if (!sessionService.isParticipant(UUID.fromString(sessionId), userId)) {
                return; // Silently drop
            }
        } catch (IllegalArgumentException e) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/chat/" + sessionId + "/typing", Map.of("userId", userId, "typing", true));
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
            
            boolean senderAuthorized = sessionService.isParticipant(sessionId, signal.getSenderId()) || ownsParticipantRestaurant(sessionId, signal.getSenderId());
            boolean targetAuthorized = sessionService.isParticipant(sessionId, targetUserId) || ownsParticipantRestaurant(sessionId, targetUserId);
            
            if (!senderAuthorized || !targetAuthorized) {
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
        
        String finalTargetUserId = targetUserId;
        if (targetUserId != null && targetUserId.length() == 36) {
            String ownerId = getRestaurantOwner(targetUserId);
            if (ownerId != null) {
                finalTargetUserId = ownerId;
            }
        }
        
        // Routes securely to the specific target user's private queue
        messagingTemplate.convertAndSendToUser(finalTargetUserId, "/queue/webrtc", signal);
    }
    
    private final java.util.concurrent.ConcurrentHashMap<String, String> restaurantOwnerCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> nonRestaurantCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final org.springframework.web.client.RestTemplate restTemplate;

    private boolean ownsParticipantRestaurant(UUID sessionId, String potentialOwnerId) {
        com.fooddelivery.chat.dto.ChatSessionResponse session = sessionService.getSessionById(sessionId).orElse(null);
        if (session == null) return false;
        
        for (com.fooddelivery.chat.dto.ParticipantDto p : session.getParticipants()) {
            if ("RESTAURANT".equals(p.getEntityType()) || p.getUserId().length() == 36) {
                String restaurantId = p.getUserId();
                String actualOwner = getRestaurantOwner(restaurantId);
                if (potentialOwnerId.equals(actualOwner)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getRestaurantOwner(String targetId) {
        if (targetId == null || targetId.length() != 36) return null;
        String cachedOwner = restaurantOwnerCache.get(targetId);
        if (cachedOwner != null) return cachedOwner;
        
        if (nonRestaurantCache.containsKey(targetId)) return null;
        
        try {
            org.springframework.http.ResponseEntity<Map> response = restTemplate.getForEntity("http://restaurant-service/api/v1/internal/restaurants/" + targetId, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String ownerId = (String) response.getBody().get("ownerId");
                if (ownerId != null) {
                    restaurantOwnerCache.put(targetId, ownerId);
                    return ownerId;
                }
            }
        } catch (Exception e) {
            // Ignore exceptions like 404
        }
        nonRestaurantCache.put(targetId, true);
        return null;
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessagingController(final SimpMessageSendingOperations messagingTemplate, final ChatMessageService messageService, final CallLogService callLogService, final ChatSessionService sessionService, final org.springframework.web.client.RestTemplate restTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.callLogService = callLogService;
        this.sessionService = sessionService;
        this.restTemplate = restTemplate;
    }
}

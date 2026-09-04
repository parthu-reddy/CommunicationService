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
import java.util.List;
import com.fooddelivery.common.client.RestaurantServiceClient;
import com.fooddelivery.common.client.CustomerServiceClient;

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
                boolean isOrderAuthorized = checkOrderParticipants(sessionId, signal.getSenderId(), targetUserId);
                if (!isOrderAuthorized) {
                    log.warn("WebRTC signal rejected: Unauthorized session/order participants sender={}, target={}", signal.getSenderId(), targetUserId);
                    return;
                }
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
        
        String finalTargetUserId = targetUserId;
        if (targetUserId != null && targetUserId.length() == 36) {
            log.info("Target user ID looks like a UUID ({}). Checking if it's a restaurant...", targetUserId);
            String ownerId = getRestaurantOwner(targetUserId);
            if (ownerId != null) {
                log.info("Target user {} is a restaurant owned by {}. Re-routing signal to owner.", targetUserId, ownerId);
                finalTargetUserId = ownerId;
            } else {
                log.info("Target user {} is NOT a restaurant (owner not found). Proceeding with target {}", targetUserId, targetUserId);
            }
        }
        
        log.info("Sending WebRTC signal to final target user ID: {} at destination /queue/webrtc", finalTargetUserId);
        // Routes securely to the specific target user's private queue
        messagingTemplate.convertAndSendToUser(finalTargetUserId, "/queue/webrtc", signal);
        log.info("WebRTC signal sent successfully to target user ID: {}", finalTargetUserId);
    }
    
    private final java.util.concurrent.ConcurrentHashMap<String, String> restaurantOwnerCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> nonRestaurantCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final RestaurantServiceClient restaurantServiceClient;
    private final CustomerServiceClient customerServiceClient;

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
        if (cachedOwner != null) {
            log.info("Found cached owner {} for restaurant {}", cachedOwner, targetId);
            return cachedOwner;
        }
        
        if (nonRestaurantCache.containsKey(targetId)) {
            log.info("Target {} is in nonRestaurantCache", targetId);
            return null;
        }
        
        try {
            log.info("Querying restaurant-service for owner of outlet {}", targetId);
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restaurantServiceClient.getOutletOwner(targetId, "communication-service");
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String ownerId = (String) response.getBody().get("ownerId");
                if (ownerId != null) {
                    log.info("Successfully fetched owner {} for restaurant {}", ownerId, targetId);
                    restaurantOwnerCache.put(targetId, ownerId);
                    return ownerId;
                }
            } else {
                log.warn("restaurant-service returned status {} for target {}", response.getStatusCode(), targetId);
            }
        } catch (Exception e) {
            log.error("Error querying restaurant-service for target {}: {}", targetId, e.getMessage());
            // Ignore exceptions like 404
        }
        nonRestaurantCache.put(targetId, true);
        return null;
    }

    private boolean checkOrderParticipants(UUID orderId, String senderId, String targetUserId) {
        try {
            log.info("Session authorization failed. Checking if {} is a valid order ID for WebRTC call.", orderId);
            org.springframework.http.ResponseEntity<List<String>> response = customerServiceClient.getOrderParticipants(orderId.toString(), "communication-service");
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<String> participants = response.getBody();
                
                boolean senderInOrder = participants.contains(senderId);
                if (!senderInOrder) {
                    for (String pId : participants) {
                        if (senderId.equals(getRestaurantOwner(pId))) {
                            senderInOrder = true;
                            break;
                        }
                    }
                }
                
                boolean targetInOrder = participants.contains(targetUserId);
                if (!targetInOrder) {
                    for (String pId : participants) {
                        if (targetUserId.equals(getRestaurantOwner(pId))) {
                            targetInOrder = true;
                            break;
                        }
                    }
                }
                
                if (senderInOrder && targetInOrder) {
                    log.info("WebRTC signal authorized via order ID: {}", orderId);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check order participants for ID {}: {}", orderId, e.getMessage());
        }
        return false;
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessagingController(final SimpMessageSendingOperations messagingTemplate, final ChatMessageService messageService, final CallLogService callLogService, final ChatSessionService sessionService, final RestaurantServiceClient restaurantServiceClient, final CustomerServiceClient customerServiceClient) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.callLogService = callLogService;
        this.sessionService = sessionService;
        this.restaurantServiceClient = restaurantServiceClient;
        this.customerServiceClient = customerServiceClient;
    }
}

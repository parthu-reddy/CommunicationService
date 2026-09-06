package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.*;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import com.fooddelivery.common.client.RestaurantServiceClient;
import com.fooddelivery.common.client.CustomerServiceClient;
import com.fooddelivery.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/chat")
@lombok.extern.slf4j.Slf4j
public class ChatSessionController {
    @java.lang.SuppressWarnings("all")

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final RestaurantServiceClient restaurantServiceClient;
    private final CustomerServiceClient customerServiceClient;

    /**
     * Create or retrieve a chat session for an order.
     * Idempotent — safe to call multiple times.
     */
    /** Chat is between identified participants; the controller resolves the caller from the security context. */
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @PostMapping("/sessions")
    @Operation(summary = "Create or retrieve a chat session for an order")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createOrGetSession(@Valid @RequestBody CreateSessionRequest request, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        boolean isRestaurant = authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT"));
        // Authorization: Ensure the creator is actually part of the session they are trying to create
        boolean isSelfParticipant = request.getParticipants().stream().anyMatch(p -> p.getUserId().equals(userId));
        if (!isSelfParticipant && !isAdmin(authentication) && !isRestaurant) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: You must be a participant to create a session"));
        }
        // Additional Security: Synchronous validation with CustomerApplication to prevent Horizontal Privilege Escalation
        if (!isAdmin(authentication)) {
            try {
                org.springframework.http.ResponseEntity<List<String>> response = customerServiceClient.getOrderParticipants(request.getOrderId(), "communication-service");
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    java.util.List<String> authorizedParticipants = response.getBody();
                    boolean isAuthorized = authorizedParticipants.contains(userId);
                    // If not directly authorized, check if user is a restaurant owner who owns the participating restaurant
                    if (!isAuthorized && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT"))) {
                        try {
                            List<String> ownedOutlets = restaurantServiceClient.getOwnerOutlets(userId, "communication-service");
                            for (String outletId : ownedOutlets) {
                                if (authorizedParticipants.contains(outletId)) {
                                    isAuthorized = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to fetch owned outlets for restaurant owner {}", userId, e);
                        }
                    }
                    if (!isAuthorized) {
                        log.warn("Privilege escalation attempt! User {} tried to access order {}", userId, request.getOrderId());
                        return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: You are not authorized for this order"));
                    }
                } else {
                    return ResponseEntity.status(403).body(ApiResponse.error("Access Denied"));
                }
            } catch (Exception e) {
                log.error("Failed to validate order participants with customer-service for order {}", request.getOrderId(), e);
                return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Unable to verify permissions"));
            }
        }
        log.info("Create/get chat session for order {} by user {}", request.getOrderId(), userId);
        ChatSessionResponse session = sessionService.createOrGetSession(request);
        return ResponseEntity.ok(ApiResponse.success(session, "Chat session ready"));
    }

    /**
     * Get the chat session for a specific order.
     */
    /** Chat is between identified participants; the controller resolves the caller from the security context. */
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @GetMapping("/sessions")
    @Operation(summary = "Get the chat session for a specific order")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> getSessionByOrderId(@RequestParam String orderId, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        return 
        // Authorization Check
        sessionService.getSessionByOrderId(orderId).map(session -> {
            if (userId == null || !sessionService.isParticipant(session.getSessionId(), userId)) {
                return ResponseEntity.status(403).body(ApiResponse.<ChatSessionResponse>error("Access Denied: Not a participant of this chat session"));
            }
            return ResponseEntity.ok(ApiResponse.success(session, "Success"));
        }).orElse(ResponseEntity.ok(ApiResponse.error("No chat session found for order: " + orderId)));
    }

    /**
     * Get paginated message history for a session.
     */
    /** Chat is between identified participants; the controller resolves the caller from the security context. */
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Get paginated message history for a session")
    public ResponseEntity<ApiResponse<com.fooddelivery.common.dto.PageResponseDto<ChatMessageDto>>> getMessages(@PathVariable UUID sessionId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        if (userId == null || !sessionService.isParticipant(sessionId, userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Not a participant of this chat session"));
        }
        Page<ChatMessageDto> messages = messageService.getMessageHistory(sessionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(com.fooddelivery.common.dto.PageResponseDto.of(messages), "Success"));
    }

    /**
     * Add a participant to an existing session (e.g., when a rider is assigned).
     */
    /** Chat is between identified participants; the controller resolves the caller from the security context. */
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @PostMapping("/sessions/{sessionId}/participants")
    @Operation(summary = "Add a participant to an existing session")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> addParticipant(@PathVariable UUID sessionId, @Valid @RequestBody ParticipantDto participantDto, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        // Ensure the person adding a participant is already in the chat, 
        // OR the person being added is themselves (e.g. a rider joining).
        boolean isAlreadyParticipant = sessionService.isParticipant(sessionId, userId);
        boolean isAddingSelf = participantDto.getUserId().equals(userId);
        if (!isAlreadyParticipant && !isAddingSelf && !isAdmin(authentication)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Cannot add participant to this session"));
        }
        // Additional Security: Synchronous validation with CustomerApplication to prevent Horizontal Privilege Escalation
        if (isAddingSelf && !isAlreadyParticipant && !isAdmin(authentication)) {
            try {
                // We need the orderId (referenceId) from the session
                ChatSessionResponse sessionOpt = sessionService.getSessionById(sessionId).orElse(null);
                if (sessionOpt == null) {
                    return ResponseEntity.status(404).body(ApiResponse.error("Session not found"));
                }
                String orderId = sessionOpt.getReferenceId();
                org.springframework.http.ResponseEntity<List<String>> response = customerServiceClient.getOrderParticipants(orderId, "communication-service");
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    java.util.List<String> authorizedParticipants = response.getBody();
                    if (!authorizedParticipants.contains(userId)) {
                        log.warn("Privilege escalation attempt! User {} tried to join session {} for order {}", userId, sessionId, orderId);
                        return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: You are not authorized to join this chat session"));
                    }
                } else {
                    return ResponseEntity.status(403).body(ApiResponse.error("Access Denied"));
                }
            } catch (Exception e) {
                log.error("Failed to validate participant addition with customer-service for session {}", sessionId, e);
                return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Unable to verify permissions"));
            }
        }
        log.info("Adding participant {} to session {} by user {}", participantDto.getUserId(), sessionId, userId);
        ChatSessionResponse session = sessionService.addParticipant(sessionId, participantDto);
        return ResponseEntity.ok(ApiResponse.success(session, "Participant added"));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SYSTEM"));
    }

    @java.lang.SuppressWarnings("all")
    public ChatSessionController(final ChatSessionService sessionService, final ChatMessageService messageService, final RestaurantServiceClient restaurantServiceClient, final CustomerServiceClient customerServiceClient) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.restaurantServiceClient = restaurantServiceClient;
        this.customerServiceClient = customerServiceClient;
    }
}

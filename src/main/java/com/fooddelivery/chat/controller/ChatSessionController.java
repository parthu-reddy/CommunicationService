package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.*;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionAccessService;
import com.fooddelivery.chat.service.ChatSessionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.chat.service.OrderChatRosterService;

@RestController
@RequestMapping("/api/v1/chat")
@lombok.extern.slf4j.Slf4j
public class ChatSessionController {
    @java.lang.SuppressWarnings("all")

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final ChatSessionAccessService accessService;
    private final OrderChatRosterService orderChatRosterService;

    /**
     * Create or retrieve a chat session for an order.
     * Idempotent — safe to call multiple times.
     */
    /** Chat is between identified participants; the controller resolves the caller from the security context. */
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @PostMapping("/sessions")
    @Operation(summary = "Create or retrieve a chat session for an order")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createOrGetSession(@RequestBody CreateSessionRequest request, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        if (request == null || request.getOrderId() == null || request.getOrderId().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Order id is required"));
        }
        try {
            // Legacy client-supplied fields are discarded. The order is the sole authority.
            List<ParticipantDto> canonicalParticipants =
                    orderChatRosterService.resolveCanonicalParticipants(request.getOrderId());
            boolean callerIsOrderParticipant = userId != null && canonicalParticipants.stream()
                    .anyMatch(participant -> userId.equals(participant.getUserId()));
            if (!callerIsOrderParticipant && !accessService.isSupportModerator(authentication)) {
                log.warn("Chat access denied for user {} on order {}", userId, request.getOrderId());
                return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: You are not authorized for this order"));
            }
            ChatSessionResponse session = sessionService.createOrGetSession(request.getOrderId(), canonicalParticipants);
            log.info("Create/get canonical chat session for order {} by user {}", request.getOrderId(), userId);
            return ResponseEntity.ok(ApiResponse.success(session, "Chat session ready"));
        } catch (RuntimeException exception) {
            log.error("Unable to verify canonical chat roster for order {}", request.getOrderId(), exception);
            return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Unable to verify permissions"));
        }
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
        if (orderId == null || orderId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Order id is required"));
        }
        try {
            // Check the order before looking for a session so an authenticated but unrelated
            // caller cannot use this endpoint to discover whether another order has a chat.
            if (userId == null || !accessService.canAccessOrder(orderId, authentication)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: You are not authorized for this order"));
            }
            return sessionService.getSessionByOrderId(orderId).map(session -> {
                if (!accessService.canAccessSession(session.getSessionId(), authentication)) {
                    return ResponseEntity.status(403).body(ApiResponse.<ChatSessionResponse>error("Access Denied: Not a participant of this chat session"));
                }
                ChatSessionResponse reconciled = sessionService.getSessionById(session.getSessionId()).orElse(session);
                return ResponseEntity.ok(ApiResponse.success(reconciled, "Success"));
            }).orElse(ResponseEntity.ok(ApiResponse.error("No chat session found for order: " + orderId)));
        } catch (RuntimeException exception) {
            log.error("Unable to verify chat access for order {}", orderId, exception);
            return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Unable to verify permissions"));
        }
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
        if (userId == null || !accessService.canAccessSession(sessionId, authentication)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Not a participant of this chat session"));
        }
        Page<ChatMessageDto> messages = messageService.getMessageHistory(sessionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(com.fooddelivery.common.dto.PageResponseDto.of(messages), "Success"));
    }

    /**
     * Reconcile an existing order session with its authoritative roster.
     */
    /** Chat is between identified participants; the controller resolves the caller from the security context. */
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @PostMapping("/sessions/{sessionId}/participants")
    @Operation(summary = "Synchronize an existing order session with its authoritative roster")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> addParticipant(@PathVariable UUID sessionId, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        try {
            // Authorize before loading the session so callers cannot distinguish an
            // unknown session ID from one they are not allowed to access.
            if (userId == null || !accessService.canAccessSession(sessionId, authentication)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Not authorized for this chat session"));
            }
            ChatSessionResponse existing = sessionService.getSessionById(sessionId).orElse(null);
            // A session can be deleted after the authorization lookup. Preserve the
            // same response so that race cannot reintroduce an existence disclosure.
            if (existing == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Not authorized for this chat session"));
            }
            List<ParticipantDto> canonicalParticipants =
                    orderChatRosterService.resolveCanonicalParticipants(existing.getReferenceId());
            ChatSessionResponse session = sessionService.synchronizeParticipants(sessionId, canonicalParticipants);
            log.info("Synchronized canonical chat roster for session {} by user {}", sessionId, userId);
            return ResponseEntity.ok(ApiResponse.success(session, "Chat participants synchronized"));
        } catch (RuntimeException exception) {
            log.error("Unable to synchronize canonical chat roster for session {}", sessionId, exception);
            return ResponseEntity.status(403).body(ApiResponse.error("Access Denied: Unable to verify permissions"));
        }
    }

    @java.lang.SuppressWarnings("all")
    public ChatSessionController(final ChatSessionService sessionService, final ChatMessageService messageService, final ChatSessionAccessService accessService, final OrderChatRosterService orderChatRosterService) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.accessService = accessService;
        this.orderChatRosterService = orderChatRosterService;
    }
}

package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.*;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatSessionController {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionController.class);

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;

    /**
     * Create or retrieve a chat session for an order.
     * Idempotent — safe to call multiple times.
     */
    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createOrGetSession(
            @Valid @RequestBody CreateSessionRequest request,
            Authentication authentication) {

        log.info("Create/get chat session for order {} by user {}", request.getOrderId(),
                authentication != null ? authentication.getName() : "anonymous");

        ChatSessionResponse session = sessionService.createOrGetSession(request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Chat session ready",
                "data", session
        ));
    }

    /**
     * Get the chat session for a specific order.
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessionByOrderId(
            @RequestParam String orderId,
            Authentication authentication) {

        String userId = authentication != null ? authentication.getName() : null;
        
        return sessionService.getSessionByOrderId(orderId)
                .map(session -> {
                    // Authorization Check
                    if (userId == null || !sessionService.isParticipant(session.getSessionId(), userId)) {
                        return ResponseEntity.status(403).body(Map.<String, Object>of(
                                "success", false,
                                "message", "Access Denied: Not a participant of this chat session"
                        ));
                    }
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "success", true,
                            "data", session
                    ));
                })
                .orElse(ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No chat session found for order: " + orderId
                )));
    }

    /**
     * Get paginated message history for a session.
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        if (userId == null || !sessionService.isParticipant(sessionId, userId)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Access Denied: Not a participant of this chat session"
            ));
        }

        Page<ChatMessageDto> messages = messageService.getMessageHistory(sessionId, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", messages.getContent(),
                "totalPages", messages.getTotalPages(),
                "totalElements", messages.getTotalElements(),
                "currentPage", messages.getNumber()
        ));
    }

    /**
     * Add a participant to an existing session (e.g., when a rider is assigned).
     */
    @PostMapping("/sessions/{sessionId}/participants")
    public ResponseEntity<Map<String, Object>> addParticipant(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ParticipantDto participantDto,
            Authentication authentication) {

        log.info("Adding participant {} to session {}", participantDto.getUserId(), sessionId);

        ChatSessionResponse session = sessionService.addParticipant(sessionId, participantDto);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Participant added",
                "data", session
        ));
    }
}

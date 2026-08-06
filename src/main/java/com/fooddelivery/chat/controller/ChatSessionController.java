package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.*;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatSessionController {
private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    /**
     * Create or retrieve a chat session for an order.
     * Idempotent — safe to call multiple times.
     */
    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createOrGetSession(
            @Valid @RequestBody CreateSessionRequest request,
            Authentication authentication) {

        String userId = authentication != null ? authentication.getName() : null;
        
        // Authorization: Ensure the creator is actually part of the session they are trying to create
        boolean isSelfParticipant = request.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
                
        if (!isSelfParticipant && !isAdmin(authentication)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Access Denied: You must be a participant to create a session"
            ));
        }

        // Additional Security: Synchronous validation with CustomerApplication to prevent Horizontal Privilege Escalation
        if (!isAdmin(authentication)) {
            try {
                String url = "http://customer-service/api/v1/internal/orders/" + request.getOrderId() + "/participants";
                org.springframework.http.ResponseEntity<String[]> response = restTemplate.getForEntity(url, String[].class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    java.util.List<String> authorizedParticipants = new java.util.ArrayList<>(java.util.Arrays.asList(response.getBody()));
                    
                    boolean isAuthorized = authorizedParticipants.contains(userId);
                    
                    // If not directly authorized, check if user is a restaurant owner who owns the participating restaurant
                    if (!isAuthorized && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT"))) {
                        try {
                            String restUrl = "http://restaurant-service/api/v1/internal/restaurants/owner/" + userId + "/outlets";
                            org.springframework.http.ResponseEntity<String[]> restResponse = restTemplate.getForEntity(restUrl, String[].class);
                            if (restResponse.getStatusCode().is2xxSuccessful() && restResponse.getBody() != null) {
                                java.util.List<String> ownedOutlets = java.util.Arrays.asList(restResponse.getBody());
                                for (String outletId : ownedOutlets) {
                                    if (authorizedParticipants.contains(outletId)) {
                                        isAuthorized = true;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to fetch owned outlets for restaurant owner {}", userId, e);
                        }
                    }
                    
                    if (!isAuthorized) {
                        log.warn("Privilege escalation attempt! User {} tried to access order {}", userId, request.getOrderId());
                        return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "message", "Access Denied: You are not authorized for this order"
                        ));
                    }
                } else {
                    return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access Denied"));
                }
            } catch (Exception e) {
                log.error("Failed to validate order participants with customer-service for order {}", request.getOrderId(), e);
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access Denied: Unable to verify permissions"));
            }
        }

        log.info("Create/get chat session for order {} by user {}", request.getOrderId(), userId);

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

        String userId = authentication != null ? authentication.getName() : null;

        // Ensure the person adding a participant is already in the chat, 
        // OR the person being added is themselves (e.g. a rider joining).
        boolean isAlreadyParticipant = sessionService.isParticipant(sessionId, userId);
        boolean isAddingSelf = participantDto.getUserId().equals(userId);
        
        if (!isAlreadyParticipant && !isAddingSelf && !isAdmin(authentication)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Access Denied: Cannot add participant to this session"
            ));
        }

        // Additional Security: Synchronous validation with CustomerApplication to prevent Horizontal Privilege Escalation
        if (isAddingSelf && !isAlreadyParticipant && !isAdmin(authentication)) {
            try {
                // We need the orderId (referenceId) from the session
                ChatSessionResponse sessionOpt = sessionService.getSessionById(sessionId).orElse(null);
                if (sessionOpt == null) {
                    return ResponseEntity.status(404).body(Map.of("success", false, "message", "Session not found"));
                }
                
                String orderId = sessionOpt.getReferenceId();
                String url = "http://customer-service/api/v1/internal/orders/" + orderId + "/participants";
                org.springframework.http.ResponseEntity<String[]> response = restTemplate.getForEntity(url, String[].class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    java.util.List<String> authorizedParticipants = java.util.Arrays.asList(response.getBody());
                    if (!authorizedParticipants.contains(userId)) {
                        log.warn("Privilege escalation attempt! User {} tried to join session {} for order {}", userId, sessionId, orderId);
                        return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "message", "Access Denied: You are not authorized to join this chat session"
                        ));
                    }
                } else {
                    return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access Denied"));
                }
            } catch (Exception e) {
                log.error("Failed to validate participant addition with customer-service for session {}", sessionId, e);
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access Denied: Unable to verify permissions"));
            }
        }

        log.info("Adding participant {} to session {} by user {}", participantDto.getUserId(), sessionId, userId);

        ChatSessionResponse session = sessionService.addParticipant(sessionId, participantDto);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Participant added",
                "data", session
        ));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SYSTEM"));
    }
}

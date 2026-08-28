package com.fooddelivery.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.chat.controller.ChatSessionController;
import com.fooddelivery.chat.controller.TurnCredentialController;
import com.fooddelivery.chat.dto.CreateSessionRequest;
import com.fooddelivery.chat.dto.ParticipantDto;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import java.util.UUID;

@Service
@lombok.extern.slf4j.Slf4j
public class ChatMcpService {
    @java.lang.SuppressWarnings("all")

    private final ChatSessionController chatSessionController;
    private final TurnCredentialController turnCredentialController;
    private final ObjectMapper objectMapper;

    public ChatMcpService(ChatSessionController chatSessionController, TurnCredentialController turnCredentialController, ObjectMapper objectMapper) {
        this.chatSessionController = chatSessionController;
        this.turnCredentialController = turnCredentialController;
        this.objectMapper = objectMapper;
    }

    /**
     * The acting principal, taken from the security context exactly as a controller would.
     *
     * <p>This replaced {@code createMockAuthentication(String userId)}, which built an
     * {@code Authentication} from a caller-supplied string, marked it authenticated, granted it
     * {@code ROLE_ADMIN} and handed it to the real controllers — satisfying every downstream
     * ownership check by construction. It was the platform's own IDOR rule inverted: derive the
     * acting entity from the authenticated principal, never from a request field.
     *
     * <p>A tool is a controller with a different transport. If the transport cannot establish a
     * security context, the tool cannot act on a user's behalf and must say so rather than invent
     * one. The MCP endpoints sit behind the same default-deny chain as everything else
     * ({@code ChatSecurityConfig} ends {@code anyRequest().authenticated()}), so a request that
     * reaches a tool has already been authenticated by {@code SecurityContextFilter} from the
     * gateway's HMAC-signed identity headers.
     */
    private Authentication requireCaller() {
        Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "No authenticated caller in the security context. MCP tools act on behalf of the "
                    + "request's principal and cannot be invoked anonymously.");
        }
        return authentication;
    }

    @Tool(description = "Create or get a chat session for the authenticated caller. Provide a JSON string of CreateSessionRequest (orderId).")
    public String createOrGetSession(String requestJson) {
        try {
            CreateSessionRequest req = objectMapper.readValue(requestJson, CreateSessionRequest.class);
            return objectMapper.writeValueAsString(chatSessionController.createOrGetSession(req, requireCaller()).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a chat session by order ID, as the authenticated caller. Provide orderId.")
    public String getSessionByOrderId(String orderId) {
        try {
            return objectMapper.writeValueAsString(chatSessionController.getSessionByOrderId(orderId, requireCaller()).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get messages for a chat session, as the authenticated caller. Provide sessionId, page, and size.")
    public String getMessages(String sessionId, int page, int size) {
        try {
            return objectMapper.writeValueAsString(chatSessionController.getMessages(UUID.fromString(sessionId), page, size, requireCaller()).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Add a participant to a chat session, as the authenticated caller. Provide sessionId and a JSON string of ParticipantDto (userId, role, name). The userId in that DTO is the participant being added, not the caller.")
    public String addParticipant(String sessionId, String requestJson) {
        try {
            ParticipantDto req = objectMapper.readValue(requestJson, ParticipantDto.class);
            return objectMapper.writeValueAsString(chatSessionController.addParticipant(UUID.fromString(sessionId), req, requireCaller()).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get ICE servers for WebRTC.")
    public String getIceServers() {
        try {
            return objectMapper.writeValueAsString(turnCredentialController.getIceServers().getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

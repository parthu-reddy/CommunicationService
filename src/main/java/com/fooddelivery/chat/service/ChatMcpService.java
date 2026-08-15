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

    private Authentication createMockAuthentication(String userId) {
        return new Authentication() {
            @Override
            public String getName() {
                return userId;
            }
            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return java.util.List.of(() -> "ROLE_ADMIN");
            }
            @Override
            public Object getCredentials() {
                return null;
            }
            @Override
            public Object getDetails() {
                return null;
            }
            @Override
            public Object getPrincipal() {
                return userId;
            }
            @Override
            public boolean isAuthenticated() {
                return true;
            }
            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }
        };
    }

    @Tool(description = "Create or get chat session. Provide userId (caller) and JSON string of CreateSessionRequest (orderId).")
    public String createOrGetSession(String userId, String requestJson) {
        try {
            CreateSessionRequest req = objectMapper.readValue(requestJson, CreateSessionRequest.class);
            return objectMapper.writeValueAsString(chatSessionController.createOrGetSession(req, createMockAuthentication(userId)).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get chat session by order ID. Provide userId (caller) and orderId.")
    public String getSessionByOrderId(String userId, String orderId) {
        try {
            return objectMapper.writeValueAsString(chatSessionController.getSessionByOrderId(orderId, createMockAuthentication(userId)).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get messages for a chat session. Provide userId (caller), sessionId, page, and size.")
    public String getMessages(String userId, String sessionId, int page, int size) {
        try {
            return objectMapper.writeValueAsString(chatSessionController.getMessages(UUID.fromString(sessionId), page, size, createMockAuthentication(userId)).getBody());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Add participant to chat session. Provide userId (caller), sessionId, and JSON string of ParticipantDto (userId, role, name).")
    public String addParticipant(String userId, String sessionId, String requestJson) {
        try {
            ParticipantDto req = objectMapper.readValue(requestJson, ParticipantDto.class);
            return objectMapper.writeValueAsString(chatSessionController.addParticipant(UUID.fromString(sessionId), req, createMockAuthentication(userId)).getBody());
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

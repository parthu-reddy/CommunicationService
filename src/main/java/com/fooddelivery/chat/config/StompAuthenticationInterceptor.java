package com.fooddelivery.chat.config;

import com.fooddelivery.common.constants.HeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.fooddelivery.chat.repository.SessionParticipantRepository;
import org.springframework.messaging.MessageDeliveryException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Intercepts STOMP CONNECT frames and sets the authenticated user principal
 * from the WebSocket session attributes (populated by WebSocketSecurityInterceptor
 * during the HTTP upgrade handshake).
 * <p>
 * Since the API Gateway already validates the JWT and forwards X-User-Id / X-User-Roles
 * headers, we trust those headers and use them for authentication — exactly like
 * the SecurityContextFilter does for REST endpoints.
 */
@Component
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthenticationInterceptor.class);

    private final SessionParticipantRepository participantRepository;

    public StompAuthenticationInterceptor(SessionParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes != null) {
                String userId = (String) sessionAttributes.get("userId");
                String roles = (String) sessionAttributes.get("roles");

                if (userId != null) {
                    List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                    if (roles != null && !roles.isEmpty()) {
                        authorities = Arrays.stream(roles.split(","))
                                .map(String::trim)
                                .filter(r -> !r.isEmpty())
                                .map(r -> {
                                    String upper = r.toUpperCase();
                                    return new SimpleGrantedAuthority(upper.startsWith("ROLE_") ? upper : "ROLE_" + upper);
                                })
                                .collect(Collectors.toList());
                    }

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    accessor.setUser(auth);
                    log.info("STOMP CONNECT authenticated: userId={}, roles={}", userId, authorities);
                } else {
                    log.warn("STOMP CONNECT with no userId in session attributes — allowing anonymous for SockJS info requests");
                }
            }
        } else if (accessor != null && (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand()))) {
            String destination = accessor.getDestination();
            if (destination != null) {
                // Dest could be /topic/chat/{sessionId} or /app/chat.send/{sessionId}
                try {
                    String[] parts = destination.split("/");
                    String lastPart = parts[parts.length - 1];
                    UUID sessionId = UUID.fromString(lastPart);
                    
                    String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;
                    if (userId == null || !participantRepository.existsByChatSessionIdAndUserId(sessionId, userId)) {
                        log.warn("User {} denied access to destination {}", userId, destination);
                        throw new MessageDeliveryException("Access Denied: Not a participant of this chat session");
                    }
                } catch (IllegalArgumentException e) {
                    // Not a UUID, maybe some other destination, ignore or allow depending on your security posture
                    // We only care about chat sessions
                }
            }
        }

        return message;
    }
}

package com.fooddelivery.chat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.fooddelivery.chat.service.ChatSessionAccessService;
import com.fooddelivery.chat.service.ChatSupportSubscriptionRegistry;
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
 * The handshake interceptor takes these attributes only from SecurityContextFilter after it has
 * verified the API Gateway's HMAC-signed identity contract. Raw identity headers never reach this
 * interceptor as a source of truth.
 */
@Component
@lombok.extern.slf4j.Slf4j
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private final ChatSessionAccessService accessService;
    private final ChatSupportSubscriptionRegistry supportSubscriptions;

    public StompAuthenticationInterceptor(ChatSessionAccessService accessService,
                                          ChatSupportSubscriptionRegistry supportSubscriptions) {
        this.accessService = accessService;
        this.supportSubscriptions = supportSubscriptions;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

            if (sessionAttributes != null) {
                String userId = (String) sessionAttributes.get("userId");
                String roles = (String) sessionAttributes.get("roles");

                if (userId != null && !userId.isBlank()) {
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
                    throw new MessageDeliveryException("Access Denied: authenticated WebSocket handshake required");
                }
            } else {
                throw new MessageDeliveryException("Access Denied: authenticated WebSocket handshake required");
            }
        } else if (accessor != null && (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand()))) {
            String destination = accessor.getDestination();
            if (destination != null) {
                java.util.regex.Matcher matcher = chatDestinationMatcher(accessor.getCommand(), destination);
                if (matcher.matches()) {
                    try {
                        UUID sessionId = UUID.fromString(matcher.group(1));
                        
                        String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;
                        if (userId == null || !accessService.canAccessSession(sessionId, accessor.getUser())) {
                            log.warn("User {} denied access to destination {}", userId, destination);
                            throw new MessageDeliveryException("Access Denied: Not a participant of this chat session");
                        }
                        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                                && accessService.isSupportModerator(accessor.getUser())) {
                            supportSubscriptions.register(sessionId, userId, accessor.getSessionId());
                        }
                    } catch (IllegalArgumentException e) {
                        log.error("Failed to parse UUID from matched destination: {}", destination, e);
                    }
                } else if (isChatDestination(destination)) {
                    // Client subscriptions must use the protected logical user destination. Raw
                    // /topic and /queue destinations can otherwise bypass roster authorization.
                    throw new MessageDeliveryException("Access Denied: unsupported chat destination");
                }
            }
        }

        return message;
    }

    private java.util.regex.Matcher chatDestinationMatcher(StompCommand command, String destination) {
        String uuid = "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})";
        if (StompCommand.SEND.equals(command)) {
            return java.util.regex.Pattern.compile("^/app/chat\\.(?:send|typing)/" + uuid + "$")
                    .matcher(destination);
        }
        return java.util.regex.Pattern.compile("^/user/queue/chat/" + uuid + "(?:/typing)?$")
                .matcher(destination);
    }

    private boolean isChatDestination(String destination) {
        return destination.startsWith("/topic/chat/")
                || destination.startsWith("/queue/chat/")
                || destination.startsWith("/user/queue/chat/")
                || destination.startsWith("/app/chat.");
    }
}

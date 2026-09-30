package com.fooddelivery.chat.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks authenticated support-moderator chat subscriptions without making a moderator a durable
 * order participant. A moderator is recorded only after the STOMP interceptor authorizes their
 * logical user-queue subscription, and is removed when that STOMP connection closes.
 */
@Service
public class ChatSupportSubscriptionRegistry {

    private final Map<UUID, Map<String, Set<String>>> connectionsBySession = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> sessionsByConnection = new ConcurrentHashMap<>();

    public void register(UUID chatSessionId, String moderatorId, String stompSessionId) {
        if (chatSessionId == null || moderatorId == null || moderatorId.isBlank()
                || stompSessionId == null || stompSessionId.isBlank()) {
            return;
        }
        connectionsBySession
                .computeIfAbsent(chatSessionId, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(moderatorId, ignored -> ConcurrentHashMap.newKeySet())
                .add(stompSessionId);
        sessionsByConnection
                .computeIfAbsent(stompSessionId, ignored -> ConcurrentHashMap.newKeySet())
                .add(chatSessionId);
    }

    public Set<String> moderatorIds(UUID chatSessionId) {
        Map<String, Set<String>> moderators = connectionsBySession.get(chatSessionId);
        return moderators == null ? Set.of() : Set.copyOf(moderators.keySet());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String stompSessionId = event.getSessionId();
        if (stompSessionId == null) {
            return;
        }
        Set<UUID> chatSessionIds = sessionsByConnection.remove(stompSessionId);
        if (chatSessionIds == null) {
            return;
        }
        for (UUID chatSessionId : chatSessionIds) {
            connectionsBySession.computeIfPresent(chatSessionId, (ignored, moderators) -> {
                moderators.values().forEach(connections -> connections.remove(stompSessionId));
                moderators.entrySet().removeIf(entry -> entry.getValue().isEmpty());
                return moderators.isEmpty() ? null : moderators;
            });
        }
    }
}

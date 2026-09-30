package com.fooddelivery.chat.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatSupportSubscriptionRegistryTest {

    @Test
    void disconnectRemovesOnlyThatModeratorsStompConnection() {
        UUID chatSessionId = UUID.randomUUID();
        ChatSupportSubscriptionRegistry registry = new ChatSupportSubscriptionRegistry();
        registry.register(chatSessionId, "support-1", "connection-a");
        registry.register(chatSessionId, "support-1", "connection-b");
        SessionDisconnectEvent disconnect = mock(SessionDisconnectEvent.class);
        when(disconnect.getSessionId()).thenReturn("connection-a");

        registry.onDisconnect(disconnect);

        assertThat(registry.moderatorIds(chatSessionId)).containsExactly("support-1");
        when(disconnect.getSessionId()).thenReturn("connection-b");
        registry.onDisconnect(disconnect);
        assertThat(registry.moderatorIds(chatSessionId)).isEmpty();
    }
}

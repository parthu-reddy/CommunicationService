package com.fooddelivery.chat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatEventBroadcasterTest {

    @Mock private SimpMessageSendingOperations messagingTemplate;
    @Mock private ChatSessionAccessService accessService;
    @Mock private ChatSupportSubscriptionRegistry supportSubscriptions;

    @Test
    void sendsOnlyCurrentCanonicalParticipantsAndAuthorizedSupportModerators() {
        UUID sessionId = UUID.randomUUID();
        when(accessService.canonicalParticipantIds(sessionId)).thenReturn(Set.of("customer-1", "rider-1"));
        when(supportSubscriptions.moderatorIds(sessionId)).thenReturn(Set.of("support-1"));
        ChatEventBroadcaster broadcaster = new ChatEventBroadcaster(
                messagingTemplate, accessService, supportSubscriptions);

        broadcaster.broadcastMessage(sessionId, "durable-message");

        String destination = "/queue/chat/" + sessionId;
        verify(messagingTemplate).convertAndSendToUser("customer-1", destination, "durable-message");
        verify(messagingTemplate).convertAndSendToUser("rider-1", destination, "durable-message");
        verify(messagingTemplate).convertAndSendToUser("support-1", destination, "durable-message");
        verify(messagingTemplate, never()).convertAndSendToUser("removed-user", destination, "durable-message");
        verify(messagingTemplate, never()).convertAndSend(
                (String) org.mockito.ArgumentMatchers.anyString(),
                (Object) org.mockito.ArgumentMatchers.any());
    }
}

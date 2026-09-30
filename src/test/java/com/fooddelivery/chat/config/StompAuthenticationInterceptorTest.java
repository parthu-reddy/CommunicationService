package com.fooddelivery.chat.config;

import com.fooddelivery.chat.service.ChatSessionAccessService;
import com.fooddelivery.chat.service.ChatSupportSubscriptionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthenticationInterceptorTest {

    @Mock private ChatSessionAccessService accessService;
    @Mock private ChatSupportSubscriptionRegistry supportSubscriptions;
    @Mock private MessageChannel channel;

    @Test
    void rejectsUnauthorizedApplicationSendBeforeItReachesTheMessageHandler() {
        UUID sessionId = UUID.randomUUID();
        StompAuthenticationInterceptor interceptor = new StompAuthenticationInterceptor(accessService, supportSubscriptions);
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("attacker-1", null);
        Message<byte[]> message = message(StompCommand.SEND, "/app/chat.send/" + sessionId, user);
        when(accessService.canAccessSession(eq(sessionId), eq(user))).thenReturn(false);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, channel));

        verify(accessService).canAccessSession(sessionId, user);
    }

    @Test
    void rejectsUnauthorizedLogicalUserSubscriptionBeforeRealtimeDataIsExposed() {
        UUID sessionId = UUID.randomUUID();
        StompAuthenticationInterceptor interceptor = new StompAuthenticationInterceptor(accessService, supportSubscriptions);
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("attacker-1", null);
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, "/user/queue/chat/" + sessionId, user);
        when(accessService.canAccessSession(eq(sessionId), eq(user))).thenReturn(false);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, channel));

        verify(accessService).canAccessSession(sessionId, user);
    }

    @Test
    void rejectsRawTopicSubscriptionEvenWhenTheCallerIsAChatParticipant() {
        UUID sessionId = UUID.randomUUID();
        StompAuthenticationInterceptor interceptor = new StompAuthenticationInterceptor(accessService, supportSubscriptions);
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("customer-1", null);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                message(StompCommand.SUBSCRIBE, "/topic/chat/" + sessionId, user), channel));

        verifyNoInteractions(accessService, supportSubscriptions);
    }

    @Test
    void rejectsRawPrivateQueueSubscriptionEvenWhenTheCallerIsAChatParticipant() {
        UUID sessionId = UUID.randomUUID();
        StompAuthenticationInterceptor interceptor = new StompAuthenticationInterceptor(accessService, supportSubscriptions);
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("customer-1", null);

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(
                message(StompCommand.SUBSCRIBE, "/queue/chat/" + sessionId + "-userconnection", user), channel));

        verifyNoInteractions(accessService, supportSubscriptions);
    }

    @Test
    void registersAnAuthorizedSupportModeratorOnTheirLogicalUserSubscription() {
        UUID sessionId = UUID.randomUUID();
        StompAuthenticationInterceptor interceptor = new StompAuthenticationInterceptor(accessService, supportSubscriptions);
        UsernamePasswordAuthenticationToken moderator = new UsernamePasswordAuthenticationToken(
                "support-1", null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPPORT_MODERATOR")));
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, "/user/queue/chat/" + sessionId, moderator, "stomp-123");
        when(accessService.canAccessSession(eq(sessionId), eq(moderator))).thenReturn(true);
        when(accessService.isSupportModerator(moderator)).thenReturn(true);

        interceptor.preSend(message, channel);

        verify(supportSubscriptions).register(sessionId, "support-1", "stomp-123");
    }

    private Message<byte[]> message(StompCommand command, String destination,
                                    UsernamePasswordAuthenticationToken user) {
        return message(command, destination, user, null);
    }

    private Message<byte[]> message(StompCommand command, String destination,
                                    UsernamePasswordAuthenticationToken user, String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(user);
        if (sessionId != null) {
            accessor.setSessionId(sessionId);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

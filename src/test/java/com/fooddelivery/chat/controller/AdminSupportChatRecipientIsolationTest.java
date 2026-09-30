package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.config.StompAuthenticationInterceptor;
import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.dto.SendMessageRequest;
import com.fooddelivery.chat.service.CallLogService;
import com.fooddelivery.chat.service.ChatEventBroadcaster;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionAccessService;
import com.fooddelivery.chat.service.ChatSupportSubscriptionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service-level delivery contract for an administrator replying in an order-support chat.
 *
 * <p>The browser only uses logical user destinations. This check drives the same STOMP
 * authorization, support-subscription registry, message controller, and recipient fanout that
 * serve that browser path without creating a shared session or sending a real message.</p>
 */
@ExtendWith(MockitoExtension.class)
class AdminSupportChatRecipientIsolationTest {

    private static final String ADMIN_ID = "support-admin-1";
    private static final String CUSTOMER_ID = "customer-1";
    private static final String RESTAURANT_OWNER_ID = "restaurant-owner-1";
    private static final String RIDER_ID = "rider-1";
    private static final String UNRELATED_ID = "unrelated-user-1";

    @Mock private SimpMessageSendingOperations messagingTemplate;
    @Mock private ChatMessageService messageService;
    @Mock private CallLogService callLogService;
    @Mock private ChatSessionAccessService accessService;
    @Mock private MessageChannel messageChannel;

    @Test
    void adminMessageReachesOnlyCanonicalOrderParticipantsAndTheCurrentAdminWhileUnrelatedIdentityIsDenied() {
        UUID sessionId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken admin = new UsernamePasswordAuthenticationToken(
                ADMIN_ID, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPORT_MODERATOR")));
        UsernamePasswordAuthenticationToken unrelated =
                new UsernamePasswordAuthenticationToken(UNRELATED_ID, null);
        ChatSupportSubscriptionRegistry supportSubscriptions = new ChatSupportSubscriptionRegistry();
        StompAuthenticationInterceptor interceptor = new StompAuthenticationInterceptor(
                accessService, supportSubscriptions);

        when(accessService.canAccessSession(sessionId, admin)).thenReturn(true);
        when(accessService.isSupportModerator(admin)).thenReturn(true);
        interceptor.preSend(subscription(sessionId, admin, "admin-stomp-connection"), messageChannel);

        when(accessService.canAccessSession(sessionId, unrelated)).thenReturn(false);
        assertThatThrownBy(() -> interceptor.preSend(
                subscription(sessionId, unrelated, "unrelated-stomp-connection"), messageChannel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Not a participant");
        assertThat(supportSubscriptions.moderatorIds(sessionId)).containsExactly(ADMIN_ID);

        ChatMessageDto durableAdminMessage = ChatMessageDto.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .senderId(ADMIN_ID)
                .senderType("SUPPORT_MODERATOR")
                .messageType("TEXT")
                .content("We are investigating your request.")
                .build();
        when(messageService.saveSupportModeratorMessage(sessionId, ADMIN_ID,
                "We are investigating your request.", "TEXT"))
                .thenReturn(durableAdminMessage);
        when(accessService.canonicalParticipantIds(sessionId))
                .thenReturn(Set.of(CUSTOMER_ID, RESTAURANT_OWNER_ID, RIDER_ID));

        ChatEventBroadcaster broadcaster = new ChatEventBroadcaster(
                messagingTemplate, accessService, supportSubscriptions);
        ChatMessagingController controller = new ChatMessagingController(
                messagingTemplate, messageService, callLogService, accessService, broadcaster);
        SendMessageRequest message = SendMessageRequest.builder()
                .content("We are investigating your request.")
                .messageType("TEXT")
                .build();

        controller.handleChatMessage(sessionId.toString(), message, admin);

        ArgumentCaptor<String> recipientCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(4)).convertAndSendToUser(
                recipientCaptor.capture(), destinationCaptor.capture(), payloadCaptor.capture());
        assertThat(recipientCaptor.getAllValues()).containsExactlyInAnyOrder(
                CUSTOMER_ID, RESTAURANT_OWNER_ID, RIDER_ID, ADMIN_ID);
        assertThat(recipientCaptor.getAllValues()).doesNotContain(UNRELATED_ID);
        assertThat(destinationCaptor.getAllValues())
                .containsOnly("/queue/chat/" + sessionId);
        assertThat(payloadCaptor.getAllValues()).containsOnly(durableAdminMessage);

        controller.handleChatMessage(sessionId.toString(), message, unrelated);

        verify(messageService, never()).saveMessage(eq(sessionId), eq(UNRELATED_ID), anyString(), eq("TEXT"));
        verify(messageService, never()).saveSupportModeratorMessage(
                eq(sessionId), eq(UNRELATED_ID), anyString(), eq("TEXT"));
        verify(messagingTemplate, times(4)).convertAndSendToUser(
                recipientCaptor.capture(), destinationCaptor.capture(), payloadCaptor.capture());
        verify(accessService, times(2)).canAccessSession(sessionId, unrelated);
    }

    private static Message<byte[]> subscription(UUID sessionId,
                                                 UsernamePasswordAuthenticationToken user,
                                                 String stompSessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/user/queue/chat/" + sessionId);
        accessor.setUser(user);
        accessor.setSessionId(stompSessionId);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

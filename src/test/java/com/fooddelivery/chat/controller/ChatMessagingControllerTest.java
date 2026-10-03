package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.dto.SendMessageRequest;
import com.fooddelivery.chat.service.CallLogService;
import com.fooddelivery.chat.service.ChatEventBroadcaster;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessagingControllerTest {

    @Mock private SimpMessageSendingOperations messagingTemplate;
    @Mock private ChatMessageService messageService;
    @Mock private CallLogService callLogService;
    @Mock private ChatSessionAccessService accessService;
    @Mock private ChatEventBroadcaster chatEventBroadcaster;

    @Test
    void persistsTextBeforeBroadcastingItsDurableMessage() {
        UUID sessionId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken customer = new UsernamePasswordAuthenticationToken("customer-1", null);
        ChatMessageDto saved = ChatMessageDto.builder().id(UUID.randomUUID()).sessionId(sessionId).content("Hello").build();
        when(accessService.isSupportModerator(customer)).thenReturn(false);
        when(accessService.canAccessSession(sessionId, customer)).thenReturn(true);
        when(messageService.saveMessage(sessionId, "customer-1", "Hello", "TEXT", null)).thenReturn(saved);
        ChatMessagingController controller = controller();

        controller.handleChatMessage(sessionId.toString(), SendMessageRequest.builder()
                .content("Hello").messageType("TEXT").build(), customer);

        InOrder inOrder = inOrder(messageService, chatEventBroadcaster);
        inOrder.verify(messageService).saveMessage(sessionId, "customer-1", "Hello", "TEXT", null);
        inOrder.verify(chatEventBroadcaster).broadcastMessage(sessionId, saved);
    }

    @Test
    void doesNotBroadcastTextWhenPersistenceFails() {
        UUID sessionId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken customer = new UsernamePasswordAuthenticationToken("customer-1", null);
        when(accessService.isSupportModerator(customer)).thenReturn(false);
        when(accessService.canAccessSession(sessionId, customer)).thenReturn(true);
        when(messageService.saveMessage(sessionId, "customer-1", "Hello", "TEXT", null))
                .thenThrow(new IllegalArgumentException("database rejected message"));

        controller().handleChatMessage(sessionId.toString(), SendMessageRequest.builder()
                .content("Hello").messageType("TEXT").build(), customer);

        verify(chatEventBroadcaster, never()).broadcastMessage(eq(sessionId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsRefundCommandFromRestaurantBeforeCreatingAnOutboxEvent() {
        UUID sessionId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken restaurant = new UsernamePasswordAuthenticationToken("restaurant-1", null);
        when(accessService.isSupportModerator(restaurant)).thenReturn(false);
        when(accessService.canAccessSession(sessionId, restaurant)).thenReturn(true);
        when(accessService.isCanonicalCustomer(sessionId, restaurant)).thenReturn(false);

        controller().handleChatMessage(sessionId.toString(), SendMessageRequest.builder()
                .content("{\"refundType\":\"FULL\"}").messageType("REFUND_REQUEST").build(), restaurant);

        verify(messageService, never()).saveMessage(eq(sessionId), eq("restaurant-1"), org.mockito.ArgumentMatchers.any(), eq("REFUND_REQUEST"), org.mockito.ArgumentMatchers.isNull());
        verify(chatEventBroadcaster, never()).broadcastMessage(eq(sessionId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsBrowserCreatedRefundDecisionBeforeItCanRenderAsAuthoritative() {
        UUID sessionId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken customer = new UsernamePasswordAuthenticationToken("customer-1", null);
        when(accessService.isSupportModerator(customer)).thenReturn(false);
        when(accessService.canAccessSession(sessionId, customer)).thenReturn(true);

        controller().handleChatMessage(sessionId.toString(), SendMessageRequest.builder()
                .content("{\"status\":\"APPROVED\"}").messageType("REFUND_DECISION").build(), customer);

        verify(messageService, never()).saveMessage(eq(sessionId), eq("customer-1"), org.mockito.ArgumentMatchers.any(), eq("REFUND_DECISION"), org.mockito.ArgumentMatchers.isNull());
        verify(chatEventBroadcaster, never()).broadcastMessage(eq(sessionId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsRefundCommandOnlyAfterTheCanonicalCustomerCheck() {
        UUID sessionId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken customer = new UsernamePasswordAuthenticationToken("customer-1", null);
        ChatMessageDto saved = ChatMessageDto.builder().id(UUID.randomUUID()).sessionId(sessionId).build();
        when(accessService.isSupportModerator(customer)).thenReturn(false);
        when(accessService.canAccessSession(sessionId, customer)).thenReturn(true);
        when(accessService.isCanonicalCustomer(sessionId, customer)).thenReturn(true);
        when(messageService.saveMessage(sessionId, "customer-1", "{\"refundType\":\"FULL\"}", "REFUND_REQUEST", null))
                .thenReturn(saved);

        controller().handleChatMessage(sessionId.toString(), SendMessageRequest.builder()
                .content("{\"refundType\":\"FULL\"}").messageType("REFUND_REQUEST").build(), customer);

        verify(accessService).isCanonicalCustomer(sessionId, customer);
        verify(chatEventBroadcaster).broadcastMessage(sessionId, saved);
    }

    private ChatMessagingController controller() {
        return new ChatMessagingController(
                messagingTemplate, messageService, callLogService, accessService, chatEventBroadcaster);
    }
}

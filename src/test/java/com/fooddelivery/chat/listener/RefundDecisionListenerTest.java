package com.fooddelivery.chat.listener;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.service.ChatEventBroadcaster;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.common.repository.IIdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class RefundDecisionListenerTest {
    private final ChatEventBroadcaster broadcaster = mock(ChatEventBroadcaster.class);
    private final ChatMessageService messages = mock(ChatMessageService.class);
    private final IIdempotencyKeyRepository keys = mock(IIdempotencyKeyRepository.class);
    private final TransactionTemplate transaction = new TransactionTemplate(mock(org.springframework.transaction.PlatformTransactionManager.class));
    private final RefundDecisionListener listener = new RefundDecisionListener(broadcaster, messages, keys, transaction);
    private final String session = UUID.randomUUID().toString();
    private final String eventId = UUID.randomUUID().toString();

    private void deliver(String type, String payload) throws Exception {
        var method = RefundDecisionListener.class.getMethod("handleRefundRecord", String.class, Map.class);
        var factory = new org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory();
        factory.afterPropertiesSet();
        var adapter = new org.springframework.kafka.listener.adapter.RecordMessagingMessageListenerAdapter<String, String>(listener, method);
        adapter.setHandlerMethod(new org.springframework.kafka.listener.adapter.HandlerAdapter(factory.createInvocableHandlerMethod(listener, method)));
        var record = new org.apache.kafka.clients.consumer.ConsumerRecord<String, String>("chat-events", 0, 0, session, payload);
        record.headers().add("eventType", type.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        record.headers().add("eventId", eventId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        adapter.onMessage(record, null, null);
    }

    @ParameterizedTest @CsvSource({"CHAT_REFUND_QUOTE_RESPONSE,REFUND_QUOTE_RESPONSE", "CHAT_REFUND_DECISION,REFUND_DECISION", "CHAT_REFUND_ERROR,REFUND_ERROR"})
    void storesAndBroadcastsRawOutboxPayloadOnce(String eventType, String messageType) throws Exception {
        String payload = "{\"quoteAmount\":18.00,\"refundType\":\"PARTIAL\"}";
        ChatMessageDto saved = ChatMessageDto.builder().id(UUID.randomUUID()).build();
        when(messages.saveMessage(UUID.fromString(session), "SYSTEM", payload, messageType)).thenReturn(saved);
        when(keys.existsById("processed_event:" + eventId)).thenReturn(false, true);
        deliver(eventType, payload); deliver(eventType, payload);
        verify(messages).saveMessage(UUID.fromString(session), "SYSTEM", payload, messageType);
        verify(broadcaster).broadcastMessage(UUID.fromString(session), saved);
        verify(keys).save(any());
    }

    @Test void storageFailureEscapesToKafkaRecovery() {
        when(messages.saveMessage(any(), anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> deliver("CHAT_REFUND_ERROR", "{}"))
                .hasStackTraceContaining("database unavailable");
        verifyNoInteractions(broadcaster);
    }

    @Test void missingDurableIdentityIsRejectedBeforePersistence() {
        assertThatThrownBy(() -> listener.handleRefundRecord("{}", Map.of("eventType", "CHAT_REFUND_ERROR")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(messages, keys, broadcaster);
    }
}

package com.fooddelivery.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.chat.entity.ChatMessage;
import com.fooddelivery.chat.entity.ChatSession;
import com.fooddelivery.chat.entity.SessionParticipant;
import com.fooddelivery.chat.repository.ChatMessageRepository;
import com.fooddelivery.chat.repository.ChatSessionRepository;
import com.fooddelivery.chat.repository.SessionParticipantRepository;
import com.fooddelivery.common.outbox.entity.OutboxEventEntity;
import com.fooddelivery.common.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock private ChatMessageRepository messageRepository;
    @Mock private ChatSessionRepository sessionRepository;
    @Mock private SessionParticipantRepository participantRepository;
    @Mock private OutboxEventRepository outboxEventRepository;

    @Test
    void refundOutboxOverwritesBrowserSuppliedOrderCustomerAndActorIdentity() throws Exception {
        UUID chatSessionId = UUID.randomUUID();
        UUID trustedOrderId = UUID.randomUUID();
        UUID browserOrderId = UUID.randomUUID();
        UUID browserCustomerId = UUID.randomUUID();
        ChatSession session = ChatSession.builder()
                .id(chatSessionId)
                .sessionType("ORDER")
                .referenceId(trustedOrderId.toString())
                .build();
        SessionParticipant customer = SessionParticipant.builder()
                .userId("customer-1")
                .entityType("CUSTOMER")
                .displayName("Customer One")
                .build();
        when(participantRepository.findByChatSessionId(chatSessionId)).thenReturn(List.of(customer));
        when(sessionRepository.findById(chatSessionId)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            return message;
        });

        ChatMessageService service = new ChatMessageService(
                messageRepository, sessionRepository, participantRepository, outboxEventRepository, new ObjectMapper());
        service.saveMessage(chatSessionId, "customer-1", "{\"orderId\":\"" + browserOrderId
                + "\",\"customerId\":\"" + browserCustomerId
                + "\",\"actorId\":\"attacker\",\"actorType\":\"ADMIN\",\"refundType\":\"FULL\"}", "REFUND_REQUEST");

        ArgumentCaptor<OutboxEventEntity> outbox = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outbox.capture());
        JsonNode payload = new ObjectMapper().readTree(outbox.getValue().getPayload());
        assertThat(payload.path("orderId").asText()).isEqualTo(trustedOrderId.toString());
        assertThat(payload.path("customerId").isMissingNode() || payload.path("customerId").isNull()).isTrue();
        assertThat(payload.path("actorId").asText()).isEqualTo("customer-1");
        assertThat(payload.path("actorType").asText()).isEqualTo("CUSTOMER");
    }
}

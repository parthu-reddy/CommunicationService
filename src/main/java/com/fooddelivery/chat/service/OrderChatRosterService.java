package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.ParticipantDto;
import com.fooddelivery.common.client.CustomerServiceClient;
import com.fooddelivery.common.dto.order.OrderChatParticipantDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the only identities that may become order-chat participants.
 *
 * <p>The browser can request a chat for an order, but it cannot nominate identities for that
 * chat. CustomerApplication owns the roster, and RestaurantApplication translates an outlet id
 * to an organisation when the outlet acts in the chat.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderChatRosterService {

    private static final String CALLING_SERVICE = "communication-service";

    private final CustomerServiceClient customerServiceClient;

    public List<ParticipantDto> resolveCanonicalParticipants(String orderId) {
        ResponseEntity<List<OrderChatParticipantDto>> response =
                customerServiceClient.getOrderChatParticipants(orderId, CALLING_SERVICE);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isEmpty()) {
            throw new IllegalStateException("Unable to load canonical chat participants");
        }

        Map<String, ParticipantDto> participantsByEntity = new LinkedHashMap<>();
        for (OrderChatParticipantDto participant : response.getBody()) {
            if (participant == null || participant.getId() == null || participant.getParticipantType() == null) {
                throw new IllegalStateException("Order returned an invalid chat participant");
            }

            switch (participant.getParticipantType()) {
                case "CUSTOMER" -> addParticipant(participantsByEntity, participant.getId().toString(), "CUSTOMER", participant.getDisplayName());
                case "DELIVERY" -> addParticipant(participantsByEntity, participant.getId().toString(), "DELIVERY", participant.getDisplayName());
                case "RESTAURANT_OUTLET" -> addParticipant(
                        participantsByEntity,
                        participant.getId().toString(),
                        "RESTAURANT",
                        participant.getDisplayName());
                default -> throw new IllegalStateException("Order returned an unsupported chat participant type");
            }
        }

        if (participantsByEntity.isEmpty()) {
            throw new IllegalStateException("Order has no canonical chat participants");
        }
        return List.copyOf(participantsByEntity.values());
    }

    private void addParticipant(Map<String, ParticipantDto> participantsByEntity,
                                String entityId,
                                String entityType,
                                String displayName) {
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalStateException("Order returned an empty chat participant identity");
        }
        participantsByEntity.putIfAbsent(entityType + ":" + entityId, ParticipantDto.builder()
                .userId("RESTAURANT".equals(entityType) ? null : entityId)
                .entityId(entityId)
                .entityType(entityType)
                .displayName(displayName == null || displayName.isBlank() ? entityType : displayName.trim())
                .build());
    }

}

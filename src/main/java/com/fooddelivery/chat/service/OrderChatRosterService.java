package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.ParticipantDto;
import com.fooddelivery.common.client.CustomerServiceClient;
import com.fooddelivery.common.client.RestaurantServiceClient;
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
 * to its owner before the result is stored locally.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderChatRosterService {

    private static final String CALLING_SERVICE = "communication-service";

    private final CustomerServiceClient customerServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;

    public List<ParticipantDto> resolveCanonicalParticipants(String orderId) {
        ResponseEntity<List<OrderChatParticipantDto>> response =
                customerServiceClient.getOrderChatParticipants(orderId, CALLING_SERVICE);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isEmpty()) {
            throw new IllegalStateException("Unable to load canonical chat participants");
        }

        Map<String, ParticipantDto> participantsByUserId = new LinkedHashMap<>();
        for (OrderChatParticipantDto participant : response.getBody()) {
            if (participant == null || participant.getId() == null || participant.getParticipantType() == null) {
                throw new IllegalStateException("Order returned an invalid chat participant");
            }

            switch (participant.getParticipantType()) {
                case "CUSTOMER" -> addParticipant(participantsByUserId, participant.getId().toString(), "CUSTOMER", participant.getDisplayName());
                case "DELIVERY" -> addParticipant(participantsByUserId, participant.getId().toString(), "DELIVERY", participant.getDisplayName());
                case "RESTAURANT_OUTLET" -> addParticipant(
                        participantsByUserId,
                        resolveOutletOwner(participant.getId().toString()),
                        "RESTAURANT",
                        participant.getDisplayName());
                default -> throw new IllegalStateException("Order returned an unsupported chat participant type");
            }
        }

        if (participantsByUserId.isEmpty()) {
            throw new IllegalStateException("Order has no canonical chat participants");
        }
        return List.copyOf(participantsByUserId.values());
    }

    private void addParticipant(Map<String, ParticipantDto> participantsByUserId,
                                String userId,
                                String entityType,
                                String displayName) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Order returned an empty chat participant identity");
        }
        participantsByUserId.putIfAbsent(userId, ParticipantDto.builder()
                .userId(userId)
                .entityType(entityType)
                .displayName(displayName == null || displayName.isBlank() ? entityType : displayName.trim())
                .build());
    }

    private String resolveOutletOwner(String outletId) {
        ResponseEntity<Map<String, Object>> response = restaurantServiceClient.getOutletOwner(outletId, CALLING_SERVICE);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Unable to resolve restaurant owner for chat");
        }
        Object ownerId = response.getBody().get("ownerId");
        if (!(ownerId instanceof String owner) || owner.isBlank()) {
            log.warn("Restaurant service returned no owner for outlet {} while resolving an order chat", outletId);
            throw new IllegalStateException("Unable to resolve restaurant owner for chat");
        }
        return owner;
    }
}

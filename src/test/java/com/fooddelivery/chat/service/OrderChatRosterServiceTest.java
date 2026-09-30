package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.ParticipantDto;
import com.fooddelivery.common.client.CustomerServiceClient;
import com.fooddelivery.common.client.RestaurantServiceClient;
import com.fooddelivery.common.dto.order.OrderChatParticipantDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderChatRosterServiceTest {

    private static final String ORDER_ID = "order-123";
    private static final String CALLING_SERVICE = "communication-service";

    @Mock private CustomerServiceClient customerServiceClient;
    @Mock private RestaurantServiceClient restaurantServiceClient;
    @InjectMocks private OrderChatRosterService rosterService;

    @Test
    void resolvesOutletToOwnerAndDoesNotPersistOutletAsAChatIdentity() {
        UUID customerId = UUID.randomUUID();
        UUID outletId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        when(customerServiceClient.getOrderChatParticipants(ORDER_ID, CALLING_SERVICE)).thenReturn(ResponseEntity.ok(List.of(
                participant(customerId, "CUSTOMER", "Customer One"),
                participant(outletId, "RESTAURANT_OUTLET", "Outlet One"),
                participant(driverId, "DELIVERY", "Delivery One"))));
        when(restaurantServiceClient.getOutletOwner(outletId.toString(), CALLING_SERVICE))
                .thenReturn(ResponseEntity.ok(Map.of("ownerId", "restaurant-owner-1")));

        List<ParticipantDto> result = rosterService.resolveCanonicalParticipants(ORDER_ID);

        assertThat(result).extracting(ParticipantDto::getUserId)
                .containsExactly(customerId.toString(), "restaurant-owner-1", driverId.toString())
                .doesNotContain(outletId.toString());
        assertThat(result).extracting(ParticipantDto::getEntityType)
                .containsExactly("CUSTOMER", "RESTAURANT", "DELIVERY");
    }

    @Test
    void failsClosedWhenRestaurantOwnerCannotBeResolved() {
        UUID outletId = UUID.randomUUID();
        when(customerServiceClient.getOrderChatParticipants(ORDER_ID, CALLING_SERVICE)).thenReturn(ResponseEntity.ok(List.of(
                participant(outletId, "RESTAURANT_OUTLET", "Outlet One"))));
        when(restaurantServiceClient.getOutletOwner(eq(outletId.toString()), eq(CALLING_SERVICE)))
                .thenReturn(ResponseEntity.ok(Map.of()));

        assertThatThrownBy(() -> rosterService.resolveCanonicalParticipants(ORDER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to resolve restaurant owner");
    }

    @Test
    void failsClosedForAnUnsupportedSourceParticipantType() {
        when(customerServiceClient.getOrderChatParticipants(ORDER_ID, CALLING_SERVICE)).thenReturn(ResponseEntity.ok(List.of(
                participant(UUID.randomUUID(), "ADMIN", "Injected admin"))));

        assertThatThrownBy(() -> rosterService.resolveCanonicalParticipants(ORDER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsupported chat participant type");
    }

    private OrderChatParticipantDto participant(UUID id, String type, String displayName) {
        return OrderChatParticipantDto.builder()
                .id(id)
                .participantType(type)
                .displayName(displayName)
                .build();
    }
}

package com.fooddelivery.chat.service;
import com.fooddelivery.chat.dto.ParticipantDto;
import com.fooddelivery.common.client.CustomerServiceClient;
import com.fooddelivery.common.dto.order.OrderChatParticipantDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderChatRosterServiceTest {
    CustomerServiceClient customers=mock(CustomerServiceClient.class);
    OrderChatRosterService roster=new OrderChatRosterService(customers);
    @Test void outletIsAnEntityAndSharedUserIdentitiesNeverDropAnotherParticipantType() {
        UUID shared=UUID.randomUUID(),outlet=UUID.randomUUID();
        when(customers.getOrderChatParticipants("order", "communication-service")).thenReturn(ResponseEntity.ok(List.of(
                participant(shared,"CUSTOMER"), participant(outlet,"RESTAURANT_OUTLET"), participant(shared,"DELIVERY"))));
        var result=roster.resolveCanonicalParticipants("order");
        assertThat(result).extracting(ParticipantDto::getEntityType).containsExactly("CUSTOMER","RESTAURANT","DELIVERY");
        assertThat(result).extracting(ParticipantDto::getEntityId).containsExactly(shared.toString(),outlet.toString(),shared.toString());
        assertThat(result).extracting(ParticipantDto::getUserId).containsExactly(shared.toString(),null,shared.toString());
    }
    @Test void failsClosedForUnsupportedOrMalformedParticipants() {
        when(customers.getOrderChatParticipants("order","communication-service")).thenReturn(ResponseEntity.ok(List.of(participant(UUID.randomUUID(),"ADMIN"))));
        assertThatThrownBy(()->roster.resolveCanonicalParticipants("order")).isInstanceOf(IllegalStateException.class);
        when(customers.getOrderChatParticipants("order","communication-service")).thenReturn(ResponseEntity.ok(List.of(participant(null,"CUSTOMER"))));
        assertThatThrownBy(()->roster.resolveCanonicalParticipants("order")).isInstanceOf(IllegalStateException.class);
    }
    private OrderChatParticipantDto participant(UUID id,String type){return OrderChatParticipantDto.builder().id(id).participantType(type).displayName(type).build();}
}

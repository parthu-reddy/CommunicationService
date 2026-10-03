package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.*;
import com.fooddelivery.common.client.*;
import com.fooddelivery.common.dto.restaurant.OutletOrganisationDto;
import com.fooddelivery.common.enums.OrganisationPermission;
import com.fooddelivery.common.security.organisation.OrganisationAccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** All entry points use current ORDERS_OPERATE access; no restaurant owner is persisted. */
class ChatRestaurantMemberAccessTest {
    ChatSessionService sessions=mock(ChatSessionService.class);
    OrderChatRosterService roster=mock(OrderChatRosterService.class);
    RestaurantServiceClient restaurants=mock(RestaurantServiceClient.class);
    OrganisationServiceClient organisations=mock(OrganisationServiceClient.class);
    OrganisationAccessPolicy policy=mock(OrganisationAccessPolicy.class);
    ChatSessionAccessService access=new ChatSessionAccessService(sessions,roster,restaurants,organisations,policy);
    UUID sessionId=UUID.randomUUID(),outletId=UUID.randomUUID(),orgId=UUID.randomUUID(),staff=UUID.randomUUID();
    ParticipantDto outlet=ParticipantDto.builder().entityId(outletId.toString()).entityType("RESTAURANT").displayName("Outlet").build();
    void setup() {
        var response=ChatSessionResponse.builder().sessionId(sessionId).referenceId("order").participants(List.of(outlet)).build();
        when(sessions.getSessionById(sessionId)).thenReturn(Optional.of(response));
        when(roster.resolveCanonicalParticipants("order")).thenReturn(List.of(outlet));
        when(restaurants.getOutletOrganisation(outletId)).thenReturn(new OutletOrganisationDto(outletId,UUID.randomUUID(),orgId));
    }
    @Test void staffAccessRestHistorySubscriptionSendingAndSignallingViaTheSamePermission() {
        setup();when(policy.canUser(staff,orgId,OrganisationPermission.ORDERS_OPERATE)).thenReturn(true);
        var caller=new UsernamePasswordAuthenticationToken(staff.toString(),null,List.of(new SimpleGrantedAuthority("ROLE_RESTAURANT")));
        assertTrue(access.canAccessOrder("order",caller));
        assertTrue(access.canAccessSession(sessionId,caller));
        assertTrue(access.isCanonicalParticipant(sessionId,staff.toString()));
        assertEquals(outletId.toString(),access.participantForUser(sessionId,staff.toString(),"RESTAURANT").orElseThrow().getEntityId());
        assertFalse(access.isCanonicalCustomer(sessionId,caller));
        verify(policy,times(4)).canUser(staff,orgId,OrganisationPermission.ORDERS_OPERATE);
    }
    @Test void removedAndUnrelatedUsersFailEveryEntryPointEvenWithAServiceOrPhantomModeratorAuthority() {
        setup();
        var caller=new UsernamePasswordAuthenticationToken(staff.toString(),null,List.of(new SimpleGrantedAuthority("ROLE_SERVICE"),new SimpleGrantedAuthority("ROLE_SUPPORT_MODERATOR")));
        assertFalse(access.canAccessOrder("order",caller));
        assertFalse(access.canAccessSession(sessionId,caller));
        assertFalse(access.isCanonicalParticipant(sessionId,staff.toString()));
        assertTrue(access.participantForUser(sessionId,staff.toString(),"RESTAURANT").isEmpty());
        verify(sessions,never()).synchronizeParticipants(any(),any());
    }
    @Test void deliveryRecipientsComeFromCurrentMembersAndNotOldPersistedOwnerData() {
        setup();
        when(organisations.getMembers(orgId,OrganisationPermission.ORDERS_OPERATE)).thenReturn(List.of(staff));
        assertEquals(Set.of(staff.toString()),access.canonicalParticipantIds(sessionId));
        when(organisations.getMembers(orgId,OrganisationPermission.ORDERS_OPERATE)).thenReturn(List.of());
        assertTrue(access.canonicalParticipantIds(sessionId).isEmpty());
        when(organisations.getMembers(orgId,OrganisationPermission.ORDERS_OPERATE)).thenThrow(new IllegalStateException("Identity unavailable"));
        assertTrue(access.canonicalParticipantIds(sessionId).isEmpty());
    }
    @Test void aCustomerWhoIsAlsoStaffCanChooseAValidatedRestaurantEntityWithoutDroppingTheirCustomerEntity() {
        setup();
        var customer=ParticipantDto.builder().entityId(staff.toString()).userId(staff.toString()).entityType("CUSTOMER").displayName("Customer").build();
        when(roster.resolveCanonicalParticipants("order")).thenReturn(List.of(customer,outlet));
        when(policy.canUser(staff,orgId,OrganisationPermission.ORDERS_OPERATE)).thenReturn(true);
        assertEquals("CUSTOMER",access.participantForUser(sessionId,staff.toString()).orElseThrow().getEntityType());
        assertEquals("RESTAURANT",access.participantForUser(sessionId,staff.toString(),"RESTAURANT").orElseThrow().getEntityType());
        assertTrue(access.participantForUser(sessionId,staff.toString(),"DELIVERY").isEmpty());
    }
}

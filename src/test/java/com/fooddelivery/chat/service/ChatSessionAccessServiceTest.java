package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.ChatSessionResponse;
import com.fooddelivery.chat.dto.ParticipantDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionAccessServiceTest {

    private static final String ORDER_ID = "order-123";

    @Mock private ChatSessionService sessionService;
    @Mock private OrderChatRosterService rosterService;
    @Mock private Authentication authentication;

    @InjectMocks private ChatSessionAccessService accessService;

    @Test
    void deniedCallerDoesNotSynchronizeTheSessionRoster() {
        UUID sessionId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("attacker-1");
        when(authentication.getAuthorities()).thenReturn(List.of());
        when(sessionService.getSessionById(sessionId))
                .thenReturn(Optional.of(session(sessionId, List.of(participant("customer-1", "CUSTOMER")))));
        when(rosterService.resolveCanonicalParticipants(ORDER_ID))
                .thenReturn(List.of(participant("customer-1", "CUSTOMER")));

        assertThat(accessService.canAccessSession(sessionId, authentication)).isFalse();

        verify(sessionService, never()).synchronizeParticipants(any(), any());
    }

    @Test
    void authorizedCallerReconcilesOnlyAStaleRosterAgainstTheCanonicalOrderRoster() {
        UUID sessionId = UUID.randomUUID();
        List<ParticipantDto> canonical = List.of(participant("customer-1", "CUSTOMER"));
        when(authentication.getName()).thenReturn("customer-1");
        when(authentication.getAuthorities()).thenReturn(List.of());
        when(sessionService.getSessionById(sessionId))
                .thenReturn(Optional.of(session(sessionId, List.of(
                        participant("customer-1", "CUSTOMER"),
                        participant("injected-user", "ADMIN")))));
        when(rosterService.resolveCanonicalParticipants(ORDER_ID)).thenReturn(canonical);
        when(sessionService.synchronizeParticipants(sessionId, canonical))
                .thenReturn(session(sessionId, canonical));

        assertThat(accessService.canAccessSession(sessionId, authentication)).isTrue();

        verify(sessionService).synchronizeParticipants(sessionId, canonical);
    }

    @Test
    void reconciliationFailureFailsClosedForAnOtherwiseAuthorizedCaller() {
        UUID sessionId = UUID.randomUUID();
        List<ParticipantDto> canonical = List.of(participant("customer-1", "CUSTOMER"));
        when(authentication.getName()).thenReturn("customer-1");
        when(authentication.getAuthorities()).thenReturn(List.of());
        when(sessionService.getSessionById(sessionId))
                .thenReturn(Optional.of(session(sessionId, List.of(participant("injected-user", "ADMIN")))));
        when(rosterService.resolveCanonicalParticipants(ORDER_ID)).thenReturn(canonical);
        when(sessionService.synchronizeParticipants(sessionId, canonical))
                .thenThrow(new IllegalStateException("persistence unavailable"));

        assertThat(accessService.canAccessSession(sessionId, authentication)).isFalse();
    }

    @Test
    void canonicalRecipientLookupUsesTheAuthoritativeRosterWithoutWritingIt() {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.getSessionById(sessionId))
                .thenReturn(Optional.of(session(sessionId, List.of(participant("injected-user", "ADMIN")))));
        when(rosterService.resolveCanonicalParticipants(ORDER_ID))
                .thenReturn(List.of(participant("customer-1", "CUSTOMER"), participant("rider-1", "DELIVERY")));

        assertThat(accessService.canonicalParticipantIds(sessionId))
                .containsExactlyInAnyOrder("customer-1", "rider-1")
                .doesNotContain("injected-user");

        verify(sessionService, never()).synchronizeParticipants(any(), any());
    }

    private ChatSessionResponse session(UUID sessionId, List<ParticipantDto> participants) {
        return ChatSessionResponse.builder()
                .sessionId(sessionId)
                .referenceId(ORDER_ID)
                .sessionType("ORDER")
                .isActive(true)
                .participants(participants)
                .build();
    }

    private ParticipantDto participant(String userId, String entityType) {
        return ParticipantDto.builder()
                .userId(userId)
                .entityType(entityType)
                .displayName(userId)
                .build();
    }
}

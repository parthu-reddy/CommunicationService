package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.ChatSessionResponse;
import com.fooddelivery.chat.dto.ParticipantDto;
import com.fooddelivery.chat.entity.ChatSession;
import com.fooddelivery.chat.entity.SessionParticipant;
import com.fooddelivery.chat.repository.ChatSessionRepository;
import com.fooddelivery.chat.repository.SessionParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock private ChatSessionRepository sessionRepository;
    @Mock private SessionParticipantRepository participantRepository;
    @InjectMocks private ChatSessionService sessionService;

    @Test
    void synchronizeParticipantsRemovesPollutedMembershipAndRestoresTheCanonicalRoster() {
        UUID sessionId = UUID.randomUUID();
        ChatSession session = ChatSession.builder()
                .id(sessionId)
                .sessionType("ORDER")
                .referenceId("order-123")
                .isActive(true)
                .participants(new ArrayList<>())
                .build();
        session.getParticipants().add(participant(session, "customer-1", "OLD_TYPE", "Old customer label"));
        session.getParticipants().add(participant(session, "attacker-1", "ADMIN", "Injected participant"));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        ChatSessionResponse response = sessionService.synchronizeParticipants(sessionId, List.of(
                canonical("customer-1", "CUSTOMER", "Customer One"),
                canonical("restaurant-owner-1", "RESTAURANT", "Restaurant One")));

        assertThat(response.getParticipants()).extracting(ParticipantDto::getUserId)
                .containsExactly("customer-1", "restaurant-owner-1")
                .doesNotContain("attacker-1");
        assertThat(response.getParticipants()).extracting(ParticipantDto::getEntityType)
                .containsExactly("CUSTOMER", "RESTAURANT");
        verify(sessionRepository).save(session);
    }

    @Test
    void createOrGetSessionLocksTheOrderBeforeLookingUpItsCanonicalSession() {
        ChatSession session = ChatSession.builder()
                .id(UUID.randomUUID())
                .sessionType("ORDER")
                .referenceId("order-123")
                .isActive(true)
                .participants(new ArrayList<>())
                .build();
        when(sessionRepository.findBySessionTypeAndReferenceId("ORDER", "order-123"))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        sessionService.createOrGetSession("order-123", List.of(canonical("customer-1", "CUSTOMER", "Customer One")));

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(sessionRepository);
        inOrder.verify(sessionRepository).lockOrderSessionCreation("ORDER:order-123");
        inOrder.verify(sessionRepository).findBySessionTypeAndReferenceId("ORDER", "order-123");
    }

    private SessionParticipant participant(ChatSession session, String userId, String type, String name) {
        return SessionParticipant.builder()
                .chatSession(session)
                .userId(userId)
                .entityType(type)
                .displayName(name)
                .build();
    }

    private ParticipantDto canonical(String userId, String type, String name) {
        return ParticipantDto.builder().userId(userId).entityType(type).displayName(name).build();
    }
}

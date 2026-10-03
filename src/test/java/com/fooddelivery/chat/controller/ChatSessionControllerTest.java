package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatSessionResponse;
import com.fooddelivery.chat.dto.CreateSessionRequest;
import com.fooddelivery.chat.dto.ParticipantDto;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionAccessService;
import com.fooddelivery.chat.service.ChatSessionService;
import com.fooddelivery.chat.service.OrderChatRosterService;
import com.fooddelivery.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionControllerTest {

    private static final String ORDER_ID = "order-123";
    private static final String USER_ID = "customer-1";
    private static final String ATTACKER_ID = "attacker-1";

    @Mock private ChatSessionService sessionService;
    @Mock private ChatMessageService messageService;
    @Mock private ChatSessionAccessService accessService;
    @Mock private OrderChatRosterService orderChatRosterService;
    @Mock private Authentication authentication;

    @InjectMocks private ChatSessionController controller;

    @BeforeEach
    void setUp() {
        lenient().when(authentication.getName()).thenReturn(USER_ID);
        lenient().when(accessService.isSupportModerator(authentication)).thenReturn(false);
        lenient().when(accessService.withCallContacts(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createOrGetSession_ignoresCallerParticipantListAndPersistsCanonicalRoster() {
        CreateSessionRequest request = requestForOrder();
        List<ParticipantDto> canonical = List.of(participant(USER_ID, "CUSTOMER"), participant("restaurant-owner", "RESTAURANT"));
        ChatSessionResponse responseSession = response(ORDER_ID, canonical);
        when(orderChatRosterService.resolveCanonicalParticipants(ORDER_ID)).thenReturn(canonical);
        when(sessionService.createOrGetSession(eq(ORDER_ID), any())).thenReturn(responseSession);
        when(accessService.canAccessParticipants(canonical, authentication)).thenReturn(true);

        ResponseEntity<ApiResponse<ChatSessionResponse>> response = controller.createOrGetSession(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<List<ParticipantDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(sessionService).createOrGetSession(eq(ORDER_ID), captor.capture());
        assertThat(captor.getValue()).extracting(ParticipantDto::getUserId)
                .containsExactly(USER_ID, null)
                .doesNotContain(ATTACKER_ID);
    }

    @Test
    void createOrGetSession_deniesCallerOutsideCanonicalRoster() {
        when(orderChatRosterService.resolveCanonicalParticipants(ORDER_ID))
                .thenReturn(List.of(participant("other-customer", "CUSTOMER")));

        ResponseEntity<ApiResponse<ChatSessionResponse>> response = controller.createOrGetSession(requestForOrder(), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(sessionService, never()).createOrGetSession(any(), any());
    }

    @Test
    void createOrGetSession_adminDoesNotPersistArbitraryParticipant() {
        when(authentication.getName()).thenReturn("admin-1");
        when(accessService.isSupportModerator(authentication)).thenReturn(true);
        List<ParticipantDto> canonical = List.of(participant(USER_ID, "CUSTOMER"));
        when(orderChatRosterService.resolveCanonicalParticipants(ORDER_ID)).thenReturn(canonical);
        when(sessionService.createOrGetSession(eq(ORDER_ID), any())).thenReturn(response(ORDER_ID, canonical));

        ResponseEntity<ApiResponse<ChatSessionResponse>> response = controller.createOrGetSession(requestForOrder(), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<List<ParticipantDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(sessionService).createOrGetSession(eq(ORDER_ID), captor.capture());
        assertThat(captor.getValue()).extracting(ParticipantDto::getUserId)
                .containsExactly(USER_ID)
                .doesNotContain(ATTACKER_ID);
    }

    @Test
    void addParticipant_synchronizesCanonicalRosterAndIgnoresRequestIdentity() {
        UUID sessionId = UUID.randomUUID();
        List<ParticipantDto> canonical = List.of(participant(USER_ID, "CUSTOMER"), participant("delivery-1", "DELIVERY"));
        when(sessionService.getSessionById(sessionId)).thenReturn(Optional.of(response(ORDER_ID, List.of(participant(USER_ID, "CUSTOMER")))));
        when(accessService.canAccessSession(sessionId, authentication)).thenReturn(true);
        when(orderChatRosterService.resolveCanonicalParticipants(ORDER_ID)).thenReturn(canonical);
        when(sessionService.synchronizeParticipants(sessionId, canonical)).thenReturn(response(ORDER_ID, canonical));

        ResponseEntity<ApiResponse<ChatSessionResponse>> response = controller.addParticipant(sessionId, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(sessionService).synchronizeParticipants(sessionId, canonical);
    }

    @Test
    void addParticipant_doesNotDiscloseWhetherAnUnauthorizedSessionExists() {
        UUID existingSessionId = UUID.randomUUID();
        UUID unknownSessionId = UUID.randomUUID();
        when(accessService.canAccessSession(existingSessionId, authentication)).thenReturn(false);
        when(accessService.canAccessSession(unknownSessionId, authentication)).thenReturn(false);

        ResponseEntity<ApiResponse<ChatSessionResponse>> existingResponse =
                controller.addParticipant(existingSessionId, authentication);
        ResponseEntity<ApiResponse<ChatSessionResponse>> unknownResponse =
                controller.addParticipant(unknownSessionId, authentication);

        assertThat(existingResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(unknownResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(existingResponse.getBody().getMessage())
                .isEqualTo(unknownResponse.getBody().getMessage());
        verify(sessionService, never()).getSessionById(any());
        verify(sessionService, never()).synchronizeParticipants(any(), any());
    }

    @Test
    void addParticipant_keepsTheSameDenialWhenSessionIsDeletedAfterAuthorization() {
        UUID sessionId = UUID.randomUUID();
        when(accessService.canAccessSession(sessionId, authentication)).thenReturn(true);
        when(sessionService.getSessionById(sessionId)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<ChatSessionResponse>> response = controller.addParticipant(sessionId, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Access Denied: Not authorized for this chat session");
        verify(sessionService, never()).synchronizeParticipants(any(), any());
    }

    @Test
    void getMessages_deniesUntrustedPersistedMembership() {
        UUID sessionId = UUID.randomUUID();
        when(accessService.canAccessSession(sessionId, authentication)).thenReturn(false);

        ResponseEntity<?> response = controller.getMessages(sessionId, 0, 50, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(messageService, never()).getMessageHistory(any(), anyInt(), anyInt());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"-1,50", "0,0", "0,101"})
    void getMessages_refusesAPageOutsideTheBounds(int page, int size) {
        UUID sessionId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("participant");
        when(accessService.canAccessSession(sessionId, authentication)).thenReturn(true);

        ResponseEntity<?> response = controller.getMessages(sessionId, page, size, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(messageService, never()).getMessageHistory(any(), anyInt(), anyInt());
    }

    /** Control: the largest allowed page reaches the history query unchanged. */
    @Test
    void getMessages_servesTheLargestAllowedPage() {
        UUID sessionId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("participant");
        when(accessService.canAccessSession(sessionId, authentication)).thenReturn(true);
        when(messageService.getMessageHistory(sessionId, 2, ChatSessionController.MAX_HISTORY_PAGE_SIZE))
                .thenReturn(org.springframework.data.domain.Page.empty());

        ResponseEntity<?> response = controller.getMessages(sessionId, 2, ChatSessionController.MAX_HISTORY_PAGE_SIZE, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getSessionByOrderId_deniesAnUnrelatedCallerBeforeDisclosingSessionExistence() {
        when(accessService.canAccessOrder(ORDER_ID, authentication)).thenReturn(false);

        ResponseEntity<ApiResponse<ChatSessionResponse>> response =
                controller.getSessionByOrderId(ORDER_ID, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(sessionService, never()).getSessionByOrderId(any());
    }

    @Test
    void createSessionRequestDiscardsLegacyCallerSelectedParticipantsAtTheJsonBoundary() throws Exception {
        CreateSessionRequest request = new ObjectMapper().readValue("""
                {"orderId":"order-123","participants":[{"userId":"attacker-1","entityType":"ADMIN"}]}
                """, CreateSessionRequest.class);

        assertThat(request.getOrderId()).isEqualTo(ORDER_ID);
    }

    private CreateSessionRequest requestForOrder() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setOrderId(ORDER_ID);
        return request;
    }

    private ParticipantDto participant(String userId, String entityType) {
        return ParticipantDto.builder().userId("RESTAURANT".equals(entityType) ? null : userId).entityId(userId).entityType(entityType).displayName(userId).build();
    }

    private ChatSessionResponse response(String orderId, List<ParticipantDto> participants) {
        return ChatSessionResponse.builder()
                .sessionId(UUID.randomUUID())
                .referenceId(orderId)
                .sessionType("ORDER")
                .isActive(true)
                .participants(participants)
                .build();
    }
}

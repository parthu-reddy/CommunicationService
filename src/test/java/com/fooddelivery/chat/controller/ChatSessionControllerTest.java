package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatSessionResponse;
import com.fooddelivery.chat.dto.CreateSessionRequest;
import com.fooddelivery.chat.dto.ParticipantDto;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionService;
import com.fooddelivery.common.client.CustomerServiceClient;
import com.fooddelivery.common.client.RestaurantServiceClient;
import com.fooddelivery.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatSessionControllerTest {

    @Mock
    private ChatSessionService sessionService;

    @Mock
    private ChatMessageService messageService;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ChatSessionController chatSessionController;

    private static final String ORDER_ID = "order-123";
    private static final String USER_ID = "user-1";
    private static final String UNAUTHORIZED_USER_ID = "hacker-1";

    @BeforeEach
    void setUp() {
        lenient().when(authentication.getName()).thenReturn(USER_ID);
        lenient().when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
    }

    @Test
    void createOrGetSession_DeniesAccess_WhenInitiatorNotAuthorized() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setOrderId(ORDER_ID);
        ParticipantDto p1 = new ParticipantDto();
        p1.setUserId(USER_ID);
        request.setParticipants(List.of(p1));

        when(customerServiceClient.getOrderParticipants(eq(ORDER_ID), anyString()))
                .thenReturn(ResponseEntity.ok(List.of("other-user-1", "other-user-2")));

        ResponseEntity<ApiResponse<ChatSessionResponse>> response = chatSessionController.createOrGetSession(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).contains("You are not authorized for this order");
    }

    @Test
    void createOrGetSession_DeniesAccess_WhenAddingUnauthorizedParticipants() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setOrderId(ORDER_ID);
        ParticipantDto p1 = new ParticipantDto();
        p1.setUserId(USER_ID);
        ParticipantDto unauthorizedParticipant = new ParticipantDto();
        unauthorizedParticipant.setUserId(UNAUTHORIZED_USER_ID);
        request.setParticipants(List.of(p1, unauthorizedParticipant));

        when(customerServiceClient.getOrderParticipants(eq(ORDER_ID), anyString()))
                .thenReturn(ResponseEntity.ok(List.of(USER_ID, "restaurant-1")));

        ResponseEntity<ApiResponse<ChatSessionResponse>> response = chatSessionController.createOrGetSession(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).contains("One or more participants are not authorized");
    }

    @Test
    void createOrGetSession_AllowsAccess_WhenAllParticipantsAuthorized() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setOrderId(ORDER_ID);
        ParticipantDto p1 = new ParticipantDto();
        p1.setUserId(USER_ID);
        ParticipantDto p2 = new ParticipantDto();
        p2.setUserId("restaurant-1");
        request.setParticipants(List.of(p1, p2));

        when(customerServiceClient.getOrderParticipants(eq(ORDER_ID), anyString()))
                .thenReturn(ResponseEntity.ok(List.of(USER_ID, "restaurant-1", "delivery-1")));
                
        ChatSessionResponse mockSession = new ChatSessionResponse();
        when(sessionService.createOrGetSession(any(CreateSessionRequest.class))).thenReturn(mockSession);

        ResponseEntity<ApiResponse<ChatSessionResponse>> response = chatSessionController.createOrGetSession(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    void addParticipant_DeniesAccess_WhenNewParticipantNotAuthorized() {
        UUID sessionId = UUID.randomUUID();
        ParticipantDto newParticipant = new ParticipantDto();
        newParticipant.setUserId(UNAUTHORIZED_USER_ID);
        
        ChatSessionResponse sessionOpt = new ChatSessionResponse();
        sessionOpt.setSessionId(sessionId);
        sessionOpt.setReferenceId(ORDER_ID);
        ParticipantDto p1 = new ParticipantDto();
        p1.setUserId(USER_ID);
        sessionOpt.setParticipants(List.of(p1));
        
        when(sessionService.getSessionById(sessionId)).thenReturn(Optional.of(sessionOpt));
        when(sessionService.isParticipant(sessionId, USER_ID)).thenReturn(true);
        when(customerServiceClient.getOrderParticipants(eq(ORDER_ID), anyString()))
                .thenReturn(ResponseEntity.ok(List.of(USER_ID, "restaurant-1")));
                
        ResponseEntity<ApiResponse<ChatSessionResponse>> response = chatSessionController.addParticipant(sessionId, newParticipant, authentication);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).contains("Participant is not authorized for this order");
    }
}

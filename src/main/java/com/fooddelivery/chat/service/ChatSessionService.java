package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.*;
import com.fooddelivery.chat.entity.ChatSession;
import com.fooddelivery.chat.entity.SessionParticipant;
import com.fooddelivery.chat.repository.ChatSessionRepository;
import com.fooddelivery.chat.repository.SessionParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatSessionService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatSessionService.class);
    private final ChatSessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;

    /**
     * Creates a new chat session for an order, or returns the existing one.
     * Idempotent: if a session for this orderId already exists, it is returned
     * with any new participants added.
     */
    @Transactional
    public ChatSessionResponse createOrGetSession(CreateSessionRequest request) {
        Optional<ChatSession> existing = sessionRepository.findByReferenceId(request.getOrderId());
        ChatSession session;
        if (existing.isPresent()) {
            session = existing.get();
            log.info("Found existing chat session {} for order {}", session.getId(), request.getOrderId());
            // Add any new participants that don't already exist
            addMissingParticipants(session, request.getParticipants());
        } else {
            session = ChatSession.builder().sessionType("ORDER").referenceId(request.getOrderId()).isActive(true).build();
            session = sessionRepository.save(session);
            log.info("Created new chat session {} for order {}", session.getId(), request.getOrderId());
            // Add all participants
            for (ParticipantDto p : request.getParticipants()) {
                SessionParticipant participant = SessionParticipant.builder().chatSession(session).userId(p.getUserId()).entityType(p.getEntityType()).displayName(p.getDisplayName()).build();
                session.getParticipants().add(participant);
            }
            session = sessionRepository.save(session);
        }
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public Optional<ChatSessionResponse> getSessionByOrderId(String orderId) {
        return sessionRepository.findByReferenceId(orderId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ChatSessionResponse> getSessionById(UUID sessionId) {
        return sessionRepository.findById(sessionId).map(this::toResponse);
    }

    /**
     * Adds a participant to an existing session (e.g., when a rider gets assigned).
     */
    @Transactional
    public ChatSessionResponse addParticipant(UUID sessionId, ParticipantDto participantDto) {
        ChatSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        if (!participantRepository.existsByChatSessionIdAndUserId(sessionId, participantDto.getUserId())) {
            SessionParticipant participant = SessionParticipant.builder().chatSession(session).userId(participantDto.getUserId()).entityType(participantDto.getEntityType()).displayName(participantDto.getDisplayName()).build();
            session.getParticipants().add(participant);
            session = sessionRepository.save(session);
            log.info("Added participant {} to session {}", participantDto.getUserId(), sessionId);
        }
        return toResponse(session);
    }

    /**
     * Check if a user is a participant in a given session.
     */
    @Transactional(readOnly = true)
    public boolean isParticipant(UUID sessionId, String userId) {
        return participantRepository.existsByChatSessionIdAndUserId(sessionId, userId);
    }

    private void addMissingParticipants(ChatSession session, List<ParticipantDto> participants) {
        for (ParticipantDto p : participants) {
            if (!participantRepository.existsByChatSessionIdAndUserId(session.getId(), p.getUserId())) {
                SessionParticipant participant = SessionParticipant.builder().chatSession(session).userId(p.getUserId()).entityType(p.getEntityType()).displayName(p.getDisplayName()).build();
                session.getParticipants().add(participant);
                log.info("Added missing participant {} to session {}", p.getUserId(), session.getId());
            }
        }
        sessionRepository.save(session);
    }

    private ChatSessionResponse toResponse(ChatSession session) {
        List<ParticipantDto> participants = session.getParticipants().stream().map(p -> ParticipantDto.builder().userId(p.getUserId()).entityType(p.getEntityType()).displayName(p.getDisplayName()).build()).collect(Collectors.toList());
        return ChatSessionResponse.builder().sessionId(session.getId()).sessionType(session.getSessionType()).referenceId(session.getReferenceId()).isActive(session.getIsActive()).createdAt(session.getCreatedAt()).participants(participants).build();
    }

    @java.lang.SuppressWarnings("all")
    public ChatSessionService(final ChatSessionRepository sessionRepository, final SessionParticipantRepository participantRepository) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
    }
}

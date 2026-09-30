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
@lombok.extern.slf4j.Slf4j
public class ChatSessionService {
    private static final String ORDER_SESSION_TYPE = "ORDER";
    @java.lang.SuppressWarnings("all")

    private final ChatSessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;

    /**
     * Creates or reconciles an order chat using a roster resolved by trusted services.
     *
     * <p>Any existing participant that is no longer canonical is removed. This repairs old
     * polluted sessions as they are accessed and prevents caller-provided identities from
     * persisting as chat members.
     */
    @Transactional
    public ChatSessionResponse createOrGetSession(String orderId, List<ParticipantDto> canonicalParticipants) {
        validateCanonicalParticipants(canonicalParticipants);
        sessionRepository.lockOrderSessionCreation(ORDER_SESSION_TYPE + ":" + orderId);
        Optional<ChatSession> existing = sessionRepository.findBySessionTypeAndReferenceId(ORDER_SESSION_TYPE, orderId);
        ChatSession session;
        if (existing.isPresent()) {
            session = existing.get();
            log.info("Found existing chat session {} for order {}", session.getId(), orderId);
        } else {
            session = ChatSession.builder().sessionType(ORDER_SESSION_TYPE).referenceId(orderId).isActive(true).build();
            session = sessionRepository.save(session);
            log.info("Created new chat session {} for order {}", session.getId(), orderId);
        }
        session = synchronize(session, canonicalParticipants);
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public Optional<ChatSessionResponse> getSessionByOrderId(String orderId) {
        return sessionRepository.findBySessionTypeAndReferenceId(ORDER_SESSION_TYPE, orderId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ChatSessionResponse> getSessionById(UUID sessionId) {
        return sessionRepository.findById(sessionId).map(this::toResponse);
    }

    /** Synchronizes an existing session to the supplied canonical roster. */
    @Transactional
    public ChatSessionResponse synchronizeParticipants(UUID sessionId, List<ParticipantDto> canonicalParticipants) {
        ChatSession session = sessionRepository.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        validateCanonicalParticipants(canonicalParticipants);
        session = synchronize(session, canonicalParticipants);
        return toResponse(session);
    }

    /**
     * Check if a user is a participant in a given session.
     */
    @Transactional(readOnly = true)
    public boolean isParticipant(UUID sessionId, String userId) {
        return participantRepository.existsByChatSessionIdAndUserId(sessionId, userId);
    }

    private ChatSession synchronize(ChatSession session, List<ParticipantDto> canonicalParticipants) {
        java.util.Map<String, ParticipantDto> canonicalByUserId = canonicalParticipants.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ParticipantDto::getUserId,
                        participant -> participant,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new));

        session.getParticipants().removeIf(existing -> !canonicalByUserId.containsKey(existing.getUserId()));
        for (SessionParticipant existing : session.getParticipants()) {
            ParticipantDto canonical = canonicalByUserId.get(existing.getUserId());
            existing.setEntityType(canonical.getEntityType());
            existing.setDisplayName(canonical.getDisplayName());
        }
        java.util.Set<String> existingUserIds = session.getParticipants().stream()
                .map(SessionParticipant::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        for (ParticipantDto canonical : canonicalParticipants) {
            if (!existingUserIds.contains(canonical.getUserId())) {
                session.getParticipants().add(SessionParticipant.builder()
                        .chatSession(session)
                        .userId(canonical.getUserId())
                        .entityType(canonical.getEntityType())
                        .displayName(canonical.getDisplayName())
                        .build());
            }
        }
        return sessionRepository.save(session);
    }

    private void validateCanonicalParticipants(List<ParticipantDto> canonicalParticipants) {
        if (canonicalParticipants == null || canonicalParticipants.isEmpty()
                || canonicalParticipants.stream().anyMatch(participant -> participant == null
                || participant.getUserId() == null || participant.getUserId().isBlank()
                || participant.getEntityType() == null || participant.getEntityType().isBlank())) {
            throw new IllegalArgumentException("Canonical chat participants are required");
        }
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

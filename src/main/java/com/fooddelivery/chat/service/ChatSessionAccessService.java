package com.fooddelivery.chat.service;

import com.fooddelivery.chat.dto.ChatSessionResponse;
import com.fooddelivery.chat.dto.ParticipantDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Authorization and roster synchronization for all chat entry points. */
@Service
@RequiredArgsConstructor
public class ChatSessionAccessService {

    private final ChatSessionService sessionService;
    private final OrderChatRosterService orderChatRosterService;

    public ChatSessionResponse synchronizeOrderSession(String orderId) {
        List<ParticipantDto> canonicalParticipants = orderChatRosterService.resolveCanonicalParticipants(orderId);
        return sessionService.createOrGetSession(orderId, canonicalParticipants);
    }

    public boolean canAccessSession(UUID sessionId, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return false;
        }
        try {
            return resolvedSession(sessionId)
                    .map(resolved -> {
                        boolean authorized = isSupportModerator(principal)
                                || containsUser(resolved.canonicalParticipants(), principal.getName());
                        if (authorized) {
                            reconcileIfStale(resolved);
                        }
                        return authorized;
                    })
                    .orElse(false);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean canAccessOrder(String orderId, Principal principal) {
        if (isSupportModerator(principal)) {
            return true;
        }
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return false;
        }
        return orderChatRosterService.resolveCanonicalParticipants(orderId).stream()
                .anyMatch(participant -> principal.getName().equals(participant.getUserId()));
    }

    public boolean isCanonicalParticipant(UUID sessionId, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return resolvedSession(sessionId)
                .map(resolved -> containsUser(resolved.canonicalParticipants(), userId))
                .orElse(false);
    }

    /** Returns the currently synchronized canonical roster for recipient-specific event delivery. */
    public Set<String> canonicalParticipantIds(UUID sessionId) {
        return resolvedSession(sessionId)
                .map(resolved -> resolved.canonicalParticipants() == null
                        ? Set.<String>of()
                        : resolved.canonicalParticipants().stream()
                                .map(ParticipantDto::getUserId)
                                .filter(userId -> userId != null && !userId.isBlank())
                                .collect(Collectors.toCollection(LinkedHashSet::new)))
                .map(Set::copyOf)
                .orElseGet(Set::of);
    }

    /** Refund requests are a customer-only action, even though other participants can use chat. */
    public boolean isCanonicalCustomer(UUID sessionId, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return false;
        }
        return resolvedSession(sessionId)
                .map(resolved -> resolved.canonicalParticipants().stream().anyMatch(participant ->
                        principal.getName().equals(participant.getUserId())
                                && "CUSTOMER".equalsIgnoreCase(participant.getEntityType())))
                .orElse(false);
    }

    public boolean isSupportModerator(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_SUPPORT_MODERATOR".equals(authority.getAuthority())
                        || "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Optional<ResolvedSession> resolvedSession(UUID sessionId) {
        try {
            return sessionService.getSessionById(sessionId).map(session ->
                    new ResolvedSession(sessionId, session,
                            orderChatRosterService.resolveCanonicalParticipants(session.getReferenceId())));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private boolean containsUser(List<ParticipantDto> participants, String userId) {
        return participants != null && participants.stream().anyMatch(participant ->
                participant != null && userId.equals(participant.getUserId()));
    }

    private void reconcileIfStale(ResolvedSession resolved) {
        if (!rosterMatches(resolved.session().getParticipants(), resolved.canonicalParticipants())) {
            sessionService.synchronizeParticipants(resolved.sessionId(), resolved.canonicalParticipants());
        }
    }

    private boolean rosterMatches(List<ParticipantDto> persisted, List<ParticipantDto> canonical) {
        List<ParticipantDto> persistedParticipants = persisted == null ? List.of() : persisted;
        List<ParticipantDto> canonicalParticipants = canonical == null ? List.of() : canonical;
        return persistedParticipants.size() == canonicalParticipants.size()
                && rosterMembers(persistedParticipants).equals(rosterMembers(canonicalParticipants));
    }

    private Set<RosterMember> rosterMembers(List<ParticipantDto> participants) {
        return participants.stream()
                .filter(Objects::nonNull)
                .map(participant -> new RosterMember(
                        participant.getUserId(), participant.getEntityType(), participant.getDisplayName()))
                .collect(Collectors.toSet());
    }

    private record ResolvedSession(
            UUID sessionId,
            ChatSessionResponse session,
            List<ParticipantDto> canonicalParticipants) {
    }

    private record RosterMember(String userId, String entityType, String displayName) {
    }
}

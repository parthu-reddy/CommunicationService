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
    private final com.fooddelivery.common.client.RestaurantServiceClient restaurantServiceClient;
    private final com.fooddelivery.common.client.OrganisationServiceClient organisationServiceClient;
    private final com.fooddelivery.common.security.organisation.OrganisationAccessPolicy organisationAccessPolicy;

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
                                || participantForUser(resolved.canonicalParticipants(), principal.getName()).isPresent();
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
        return participantForUser(orderChatRosterService.resolveCanonicalParticipants(orderId), principal.getName()).isPresent();
    }

    public boolean isCanonicalParticipant(UUID sessionId, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return resolvedSession(sessionId)
                .map(resolved -> participantForUser(resolved.canonicalParticipants(), userId).isPresent())
                .orElse(false);
    }

    /** Fresh server-side membership controls per-user delivery; removed staff never remain durable recipients. */
    public Set<String> canonicalParticipantIds(UUID sessionId) {
        return resolvedSession(sessionId).map(resolved -> {
            Set<String> recipients = new LinkedHashSet<>();
            for (ParticipantDto participant : resolved.canonicalParticipants()) {
                if ("RESTAURANT".equals(participant.getEntityType())) {
                    try {
                        var outlet = restaurantServiceClient.getOutletOrganisation(UUID.fromString(participant.getEntityId()));
                        if (outlet != null && participant.getEntityId().equals(outlet.outletId().toString()) && outlet.organisationId() != null) {
                            var members = organisationServiceClient.getMembers(outlet.organisationId(),
                                    com.fooddelivery.common.enums.OrganisationPermission.ORDERS_OPERATE);
                            if (members != null) { members.stream().filter(Objects::nonNull).map(UUID::toString).forEach(recipients::add); }
                        }
                    } catch (RuntimeException unavailable) { /* Fail closed for this outlet's recipients. */ }
                } else if (participant.getUserId() != null) { recipients.add(participant.getUserId()); }
            }
            return Set.copyOf(recipients);
        }).orElseGet(Set::of);
    }

    /** Resolve call contacts after order authorization; provider outages leave calls unavailable. */
    public ChatSessionResponse withCallContacts(ChatSessionResponse session) {
        if (session.getParticipants() == null) { return session; }
        for (ParticipantDto participant : session.getParticipants()) {
            if (!"RESTAURANT".equals(participant.getEntityType())) {
                participant.setContactUserIds(participant.getUserId() == null ? List.of() : List.of(participant.getUserId()));
                continue;
            }
            try {
                var outlet = restaurantServiceClient.getOutletOrganisation(UUID.fromString(participant.getEntityId()));
                if (outlet == null || !participant.getEntityId().equals(outlet.outletId().toString()) || outlet.organisationId() == null) {
                    participant.setContactUserIds(List.of()); continue;
                }
                var members = organisationServiceClient.getMembers(outlet.organisationId(), com.fooddelivery.common.enums.OrganisationPermission.ORDERS_OPERATE);
                participant.setContactUserIds(members == null ? List.of() : members.stream().filter(Objects::nonNull).map(UUID::toString).distinct().sorted().toList());
            } catch (RuntimeException unavailable) { participant.setContactUserIds(List.of()); }
        }
        return session;
    }

    public Optional<ParticipantDto> participantForUser(UUID sessionId, String userId) {
        return participantForUser(sessionId, userId, null);
    }
    public Optional<ParticipantDto> participantForUser(UUID sessionId, String userId, String entityType) {
        return resolvedSession(sessionId).flatMap(resolved -> participantForUser(resolved.canonicalParticipants(), userId, entityType));
    }

    public boolean canAccessParticipants(List<ParticipantDto> participants, Principal principal) {
        return principal != null && (isSupportModerator(principal) || participantForUser(participants, principal.getName()).isPresent());
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
                "ROLE_ADMIN".equals(authority.getAuthority()));
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

    private Optional<ParticipantDto> participantForUser(List<ParticipantDto> participants, String userId) {
        return participantForUser(participants,userId,null);
    }
    private Optional<ParticipantDto> participantForUser(List<ParticipantDto> participants, String userId, String entityType) {
        if (participants == null || userId == null || userId.isBlank()) { return Optional.empty(); }
        // An actor who is both customer and staff speaks for themselves by default.
        var person = participants.stream().filter(Objects::nonNull)
                .filter(p -> !"RESTAURANT".equals(p.getEntityType()) && userId.equals(p.getUserId())
                    && (entityType == null || entityType.equals(p.getEntityType()))).findFirst();
        if (person.isPresent()) { return person; }
        if (entityType != null && !"RESTAURANT".equals(entityType)) { return Optional.empty(); }
        try {
            UUID actor = UUID.fromString(userId);
            return participants.stream().filter(Objects::nonNull).filter(p -> "RESTAURANT".equals(p.getEntityType()))
                .filter(p -> {
                    var outlet = restaurantServiceClient.getOutletOrganisation(UUID.fromString(p.getEntityId()));
                    return outlet != null && p.getEntityId().equals(outlet.outletId().toString()) && outlet.organisationId() != null
                        && organisationAccessPolicy.canUser(actor, outlet.organisationId(), com.fooddelivery.common.enums.OrganisationPermission.ORDERS_OPERATE);
                }).findFirst();
        } catch (RuntimeException unavailable) { return Optional.empty(); }
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
                        participant.getUserId(), participant.getEntityId(), participant.getEntityType(), participant.getDisplayName()))
                .collect(Collectors.toSet());
    }

    private record ResolvedSession(
            UUID sessionId,
            ChatSessionResponse session,
            List<ParticipantDto> canonicalParticipants) {
    }

    private record RosterMember(String userId, String entityId, String entityType, String displayName) {
    }
}

package com.fooddelivery.chat.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SessionParticipantTest {

    @Test
    void initializesJoinedAtBeforePersistingANewParticipant() {
        SessionParticipant participant = SessionParticipant.builder()
                .userId("user-1")
                .entityType("CUSTOMER")
                .build();

        participant.initializeJoinedAt();

        assertThat(participant.getJoinedAt()).isNotNull();
    }

    @Test
    void preservesAnExplicitJoinedAt() {
        Instant joinedAt = Instant.parse("2026-09-29T01:00:00Z");
        SessionParticipant participant = SessionParticipant.builder()
                .userId("user-1")
                .entityType("CUSTOMER")
                .joinedAt(joinedAt)
                .build();

        participant.initializeJoinedAt();

        assertThat(participant.getJoinedAt()).isEqualTo(joinedAt);
    }
}

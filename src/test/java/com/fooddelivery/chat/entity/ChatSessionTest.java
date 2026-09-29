package com.fooddelivery.chat.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSessionTest {

    @Test
    void initializesCreatedAtBeforePersistingANewSession() {
        ChatSession session = ChatSession.builder()
                .sessionType("ORDER")
                .referenceId("order-123")
                .isActive(true)
                .build();

        session.initializeCreatedAt();

        assertThat(session.getCreatedAt()).isNotNull();
    }

    @Test
    void preservesAnExplicitCreatedAt() {
        Instant createdAt = Instant.parse("2026-09-29T01:00:00Z");
        ChatSession session = ChatSession.builder()
                .sessionType("ORDER")
                .referenceId("order-123")
                .isActive(true)
                .createdAt(createdAt)
                .build();

        session.initializeCreatedAt();

        assertThat(session.getCreatedAt()).isEqualTo(createdAt);
    }
}

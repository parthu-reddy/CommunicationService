package com.fooddelivery.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_participants", uniqueConstraints = {@UniqueConstraint(columnNames = {"session_id", "user_id"})})@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder

public class SessionParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession chatSession;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "entity_type", nullable = false)
    private String entityType;
    @Column(name = "display_name")
    private String displayName;
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

}

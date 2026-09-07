package com.fooddelivery.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder

public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    @Column(name = "sender_id", nullable = false)
    private String senderId;
    @Column(name = "message_type", nullable = false)
    private String messageType;
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}

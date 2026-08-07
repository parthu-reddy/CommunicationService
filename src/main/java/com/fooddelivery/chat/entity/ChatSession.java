package com.fooddelivery.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "session_type", nullable = false)
    private String sessionType;
    @Column(name = "reference_id")
    private String referenceId;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SessionParticipant> participants;

    @java.lang.SuppressWarnings("all")
    private static String $default$sessionType() {
        return "ORDER";
    }

    @java.lang.SuppressWarnings("all")
    private static Boolean $default$isActive() {
        return true;
    }

    @java.lang.SuppressWarnings("all")
    private static Instant $default$createdAt() {
        return Instant.now();
    }

    @java.lang.SuppressWarnings("all")
    private static List<SessionParticipant> $default$participants() {
        return new ArrayList<>();
    }


    @java.lang.SuppressWarnings("all")
    public static class ChatSessionBuilder {
        @java.lang.SuppressWarnings("all")
        private UUID id;
        @java.lang.SuppressWarnings("all")
        private boolean sessionType$set;
        @java.lang.SuppressWarnings("all")
        private String sessionType$value;
        @java.lang.SuppressWarnings("all")
        private String referenceId;
        @java.lang.SuppressWarnings("all")
        private boolean isActive$set;
        @java.lang.SuppressWarnings("all")
        private Boolean isActive$value;
        @java.lang.SuppressWarnings("all")
        private boolean createdAt$set;
        @java.lang.SuppressWarnings("all")
        private Instant createdAt$value;
        @java.lang.SuppressWarnings("all")
        private boolean participants$set;
        @java.lang.SuppressWarnings("all")
        private List<SessionParticipant> participants$value;

        @java.lang.SuppressWarnings("all")
        ChatSessionBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSession.ChatSessionBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSession.ChatSessionBuilder sessionType(final String sessionType) {
            this.sessionType$value = sessionType;
            sessionType$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSession.ChatSessionBuilder referenceId(final String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSession.ChatSessionBuilder isActive(final Boolean isActive) {
            this.isActive$value = isActive;
            isActive$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSession.ChatSessionBuilder createdAt(final Instant createdAt) {
            this.createdAt$value = createdAt;
            createdAt$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSession.ChatSessionBuilder participants(final List<SessionParticipant> participants) {
            this.participants$value = participants;
            participants$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ChatSession build() {
            String sessionType$value = this.sessionType$value;
            if (!this.sessionType$set) sessionType$value = ChatSession.$default$sessionType();
            Boolean isActive$value = this.isActive$value;
            if (!this.isActive$set) isActive$value = ChatSession.$default$isActive();
            Instant createdAt$value = this.createdAt$value;
            if (!this.createdAt$set) createdAt$value = ChatSession.$default$createdAt();
            List<SessionParticipant> participants$value = this.participants$value;
            if (!this.participants$set) participants$value = ChatSession.$default$participants();
            return new ChatSession(this.id, sessionType$value, this.referenceId, isActive$value, createdAt$value, participants$value);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ChatSession.ChatSessionBuilder(id=" + this.id + ", sessionType$value=" + this.sessionType$value + ", referenceId=" + this.referenceId + ", isActive$value=" + this.isActive$value + ", createdAt$value=" + this.createdAt$value + ", participants$value=" + this.participants$value + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ChatSession.ChatSessionBuilder builder() {
        return new ChatSession.ChatSessionBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public UUID getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getSessionType() {
        return this.sessionType;
    }

    @java.lang.SuppressWarnings("all")
    public String getReferenceId() {
        return this.referenceId;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getIsActive() {
        return this.isActive;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public List<SessionParticipant> getParticipants() {
        return this.participants;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final UUID id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionType(final String sessionType) {
        this.sessionType = sessionType;
    }

    @java.lang.SuppressWarnings("all")
    public void setReferenceId(final String referenceId) {
        this.referenceId = referenceId;
    }

    @java.lang.SuppressWarnings("all")
    public void setIsActive(final Boolean isActive) {
        this.isActive = isActive;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setParticipants(final List<SessionParticipant> participants) {
        this.participants = participants;
    }

    @java.lang.SuppressWarnings("all")
    public ChatSession() {
        this.sessionType = ChatSession.$default$sessionType();
        this.isActive = ChatSession.$default$isActive();
        this.createdAt = ChatSession.$default$createdAt();
        this.participants = ChatSession.$default$participants();
    }

    @java.lang.SuppressWarnings("all")
    public ChatSession(final UUID id, final String sessionType, final String referenceId, final Boolean isActive, final Instant createdAt, final List<SessionParticipant> participants) {
        this.id = id;
        this.sessionType = sessionType;
        this.referenceId = referenceId;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.participants = participants;
    }
}

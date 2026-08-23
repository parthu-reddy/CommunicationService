package com.fooddelivery.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_participants", uniqueConstraints = {@UniqueConstraint(columnNames = {"session_id", "user_id"})})
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

        @java.lang.SuppressWarnings("all")
    public static class SessionParticipantBuilder {
        @java.lang.SuppressWarnings("all")
        private UUID id;
        @java.lang.SuppressWarnings("all")
        private ChatSession chatSession;
        @java.lang.SuppressWarnings("all")
        private String userId;
        @java.lang.SuppressWarnings("all")
        private String entityType;
        @java.lang.SuppressWarnings("all")
        private String displayName;
        @java.lang.SuppressWarnings("all")
        private boolean joinedAt$set;
        @java.lang.SuppressWarnings("all")
        private Instant joinedAt$value;

        @java.lang.SuppressWarnings("all")
        SessionParticipantBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionParticipant.SessionParticipantBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionParticipant.SessionParticipantBuilder chatSession(final ChatSession chatSession) {
            this.chatSession = chatSession;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionParticipant.SessionParticipantBuilder userId(final String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionParticipant.SessionParticipantBuilder entityType(final String entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionParticipant.SessionParticipantBuilder displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SessionParticipant.SessionParticipantBuilder joinedAt(final Instant joinedAt) {
            this.joinedAt$value = joinedAt;
            joinedAt$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public SessionParticipant build() {
            Instant joinedAt$value = this.joinedAt$value;
            if (!this.joinedAt$set) joinedAt$value = SessionParticipant.$default$joinedAt();
            return new SessionParticipant(this.id, this.chatSession, this.userId, this.entityType, this.displayName, joinedAt$value);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "SessionParticipant.SessionParticipantBuilder(id=" + this.id + ", chatSession=" + this.chatSession + ", userId=" + this.userId + ", entityType=" + this.entityType + ", displayName=" + this.displayName + ", joinedAt$value=" + this.joinedAt$value + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static SessionParticipant.SessionParticipantBuilder builder() {
        return new SessionParticipant.SessionParticipantBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public UUID getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public ChatSession getChatSession() {
        return this.chatSession;
    }

    @java.lang.SuppressWarnings("all")
    public String getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public String getEntityType() {
        return this.entityType;
    }

    @java.lang.SuppressWarnings("all")
    public String getDisplayName() {
        return this.displayName;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getJoinedAt() {
        return this.joinedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final UUID id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setChatSession(final ChatSession chatSession) {
        this.chatSession = chatSession;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntityType(final String entityType) {
        this.entityType = entityType;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @java.lang.SuppressWarnings("all")
    public void setJoinedAt(final Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    @java.lang.SuppressWarnings("all")
    public SessionParticipant() {
        this.joinedAt = SessionParticipant.$default$joinedAt();
    }

    @java.lang.SuppressWarnings("all")
    public SessionParticipant(final UUID id, final ChatSession chatSession, final String userId, final String entityType, final String displayName, final Instant joinedAt) {
        this.id = id;
        this.chatSession = chatSession;
        this.userId = userId;
        this.entityType = entityType;
        this.displayName = displayName;
        this.joinedAt = joinedAt;
    }

    private static Instant $default$joinedAt() {
        return Instant.now();
    }
}

package com.fooddelivery.chat.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ChatSessionResponse {
    private UUID sessionId;
    private String sessionType;
    private String referenceId;
    private Boolean isActive;
    private Instant createdAt;
    private List<ParticipantDto> participants;


    @java.lang.SuppressWarnings("all")
    public static class ChatSessionResponseBuilder {
        @java.lang.SuppressWarnings("all")
        private UUID sessionId;
        @java.lang.SuppressWarnings("all")
        private String sessionType;
        @java.lang.SuppressWarnings("all")
        private String referenceId;
        @java.lang.SuppressWarnings("all")
        private Boolean isActive;
        @java.lang.SuppressWarnings("all")
        private Instant createdAt;
        @java.lang.SuppressWarnings("all")
        private List<ParticipantDto> participants;

        @java.lang.SuppressWarnings("all")
        ChatSessionResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSessionResponse.ChatSessionResponseBuilder sessionId(final UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSessionResponse.ChatSessionResponseBuilder sessionType(final String sessionType) {
            this.sessionType = sessionType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSessionResponse.ChatSessionResponseBuilder referenceId(final String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSessionResponse.ChatSessionResponseBuilder isActive(final Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSessionResponse.ChatSessionResponseBuilder createdAt(final Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatSessionResponse.ChatSessionResponseBuilder participants(final List<ParticipantDto> participants) {
            this.participants = participants;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ChatSessionResponse build() {
            return new ChatSessionResponse(this.sessionId, this.sessionType, this.referenceId, this.isActive, this.createdAt, this.participants);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ChatSessionResponse.ChatSessionResponseBuilder(sessionId=" + this.sessionId + ", sessionType=" + this.sessionType + ", referenceId=" + this.referenceId + ", isActive=" + this.isActive + ", createdAt=" + this.createdAt + ", participants=" + this.participants + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ChatSessionResponse.ChatSessionResponseBuilder builder() {
        return new ChatSessionResponse.ChatSessionResponseBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public UUID getSessionId() {
        return this.sessionId;
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
    public List<ParticipantDto> getParticipants() {
        return this.participants;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionId(final UUID sessionId) {
        this.sessionId = sessionId;
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
    public void setParticipants(final List<ParticipantDto> participants) {
        this.participants = participants;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ChatSessionResponse)) return false;
        final ChatSessionResponse other = (ChatSessionResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$isActive = this.getIsActive();
        final java.lang.Object other$isActive = other.getIsActive();
        if (this$isActive == null ? other$isActive != null : !this$isActive.equals(other$isActive)) return false;
        final java.lang.Object this$sessionId = this.getSessionId();
        final java.lang.Object other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) return false;
        final java.lang.Object this$sessionType = this.getSessionType();
        final java.lang.Object other$sessionType = other.getSessionType();
        if (this$sessionType == null ? other$sessionType != null : !this$sessionType.equals(other$sessionType)) return false;
        final java.lang.Object this$referenceId = this.getReferenceId();
        final java.lang.Object other$referenceId = other.getReferenceId();
        if (this$referenceId == null ? other$referenceId != null : !this$referenceId.equals(other$referenceId)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$participants = this.getParticipants();
        final java.lang.Object other$participants = other.getParticipants();
        if (this$participants == null ? other$participants != null : !this$participants.equals(other$participants)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ChatSessionResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $isActive = this.getIsActive();
        result = result * PRIME + ($isActive == null ? 43 : $isActive.hashCode());
        final java.lang.Object $sessionId = this.getSessionId();
        result = result * PRIME + ($sessionId == null ? 43 : $sessionId.hashCode());
        final java.lang.Object $sessionType = this.getSessionType();
        result = result * PRIME + ($sessionType == null ? 43 : $sessionType.hashCode());
        final java.lang.Object $referenceId = this.getReferenceId();
        result = result * PRIME + ($referenceId == null ? 43 : $referenceId.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $participants = this.getParticipants();
        result = result * PRIME + ($participants == null ? 43 : $participants.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ChatSessionResponse(sessionId=" + this.getSessionId() + ", sessionType=" + this.getSessionType() + ", referenceId=" + this.getReferenceId() + ", isActive=" + this.getIsActive() + ", createdAt=" + this.getCreatedAt() + ", participants=" + this.getParticipants() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public ChatSessionResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public ChatSessionResponse(final UUID sessionId, final String sessionType, final String referenceId, final Boolean isActive, final Instant createdAt, final List<ParticipantDto> participants) {
        this.sessionId = sessionId;
        this.sessionType = sessionType;
        this.referenceId = referenceId;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.participants = participants;
    }
}

package com.fooddelivery.chat.dto;

import jakarta.validation.constraints.NotBlank;

public class ParticipantDto {
    @NotBlank
    private String userId;
    @NotBlank
    private String entityType;
    private String displayName;


    @java.lang.SuppressWarnings("all")
    public static class ParticipantDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private String userId;
        @java.lang.SuppressWarnings("all")
        private String entityType;
        @java.lang.SuppressWarnings("all")
        private String displayName;

        @java.lang.SuppressWarnings("all")
        ParticipantDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ParticipantDto.ParticipantDtoBuilder userId(final String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ParticipantDto.ParticipantDtoBuilder entityType(final String entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ParticipantDto.ParticipantDtoBuilder displayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ParticipantDto build() {
            return new ParticipantDto(this.userId, this.entityType, this.displayName);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ParticipantDto.ParticipantDtoBuilder(userId=" + this.userId + ", entityType=" + this.entityType + ", displayName=" + this.displayName + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ParticipantDto.ParticipantDtoBuilder builder() {
        return new ParticipantDto.ParticipantDtoBuilder();
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

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ParticipantDto)) return false;
        final ParticipantDto other = (ParticipantDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$entityType = this.getEntityType();
        final java.lang.Object other$entityType = other.getEntityType();
        if (this$entityType == null ? other$entityType != null : !this$entityType.equals(other$entityType)) return false;
        final java.lang.Object this$displayName = this.getDisplayName();
        final java.lang.Object other$displayName = other.getDisplayName();
        if (this$displayName == null ? other$displayName != null : !this$displayName.equals(other$displayName)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ParticipantDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $entityType = this.getEntityType();
        result = result * PRIME + ($entityType == null ? 43 : $entityType.hashCode());
        final java.lang.Object $displayName = this.getDisplayName();
        result = result * PRIME + ($displayName == null ? 43 : $displayName.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ParticipantDto(userId=" + this.getUserId() + ", entityType=" + this.getEntityType() + ", displayName=" + this.getDisplayName() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public ParticipantDto() {
    }

    @java.lang.SuppressWarnings("all")
    public ParticipantDto(final String userId, final String entityType, final String displayName) {
        this.userId = userId;
        this.entityType = entityType;
        this.displayName = displayName;
    }
}

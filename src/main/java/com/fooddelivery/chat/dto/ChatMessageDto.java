package com.fooddelivery.chat.dto;

import java.time.Instant;
import java.util.UUID;

public class ChatMessageDto {
    private UUID id;
    private UUID sessionId;
    private String senderId;
    private String senderName;
    private String senderType;
    private String messageType;
    private String content;
    private String imageUrl;
    private Instant timestamp;


    @java.lang.SuppressWarnings("all")
    public static class ChatMessageDtoBuilder {
        @java.lang.SuppressWarnings("all")
        private UUID id;
        @java.lang.SuppressWarnings("all")
        private UUID sessionId;
        @java.lang.SuppressWarnings("all")
        private String senderId;
        @java.lang.SuppressWarnings("all")
        private String senderName;
        @java.lang.SuppressWarnings("all")
        private String senderType;
        @java.lang.SuppressWarnings("all")
        private String messageType;
        @java.lang.SuppressWarnings("all")
        private String content;
        @java.lang.SuppressWarnings("all")
        private String imageUrl;
        @java.lang.SuppressWarnings("all")
        private Instant timestamp;

        @java.lang.SuppressWarnings("all")
        ChatMessageDtoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder sessionId(final UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder senderId(final String senderId) {
            this.senderId = senderId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder senderName(final String senderName) {
            this.senderName = senderName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder senderType(final String senderType) {
            this.senderType = senderType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder messageType(final String messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder content(final String content) {
            this.content = content;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder imageUrl(final String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessageDto.ChatMessageDtoBuilder timestamp(final Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ChatMessageDto build() {
            return new ChatMessageDto(this.id, this.sessionId, this.senderId, this.senderName, this.senderType, this.messageType, this.content, this.imageUrl, this.timestamp);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ChatMessageDto.ChatMessageDtoBuilder(id=" + this.id + ", sessionId=" + this.sessionId + ", senderId=" + this.senderId + ", senderName=" + this.senderName + ", senderType=" + this.senderType + ", messageType=" + this.messageType + ", content=" + this.content + ", imageUrl=" + this.imageUrl + ", timestamp=" + this.timestamp + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ChatMessageDto.ChatMessageDtoBuilder builder() {
        return new ChatMessageDto.ChatMessageDtoBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public UUID getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public UUID getSessionId() {
        return this.sessionId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSenderId() {
        return this.senderId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSenderName() {
        return this.senderName;
    }

    @java.lang.SuppressWarnings("all")
    public String getSenderType() {
        return this.senderType;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessageType() {
        return this.messageType;
    }

    @java.lang.SuppressWarnings("all")
    public String getContent() {
        return this.content;
    }

    @java.lang.SuppressWarnings("all")
    public String getImageUrl() {
        return this.imageUrl;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getTimestamp() {
        return this.timestamp;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final UUID id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionId(final UUID sessionId) {
        this.sessionId = sessionId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSenderId(final String senderId) {
        this.senderId = senderId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSenderName(final String senderName) {
        this.senderName = senderName;
    }

    @java.lang.SuppressWarnings("all")
    public void setSenderType(final String senderType) {
        this.senderType = senderType;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    @java.lang.SuppressWarnings("all")
    public void setContent(final String content) {
        this.content = content;
    }

    @java.lang.SuppressWarnings("all")
    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @java.lang.SuppressWarnings("all")
    public void setTimestamp(final Instant timestamp) {
        this.timestamp = timestamp;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ChatMessageDto)) return false;
        final ChatMessageDto other = (ChatMessageDto) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$sessionId = this.getSessionId();
        final java.lang.Object other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) return false;
        final java.lang.Object this$senderId = this.getSenderId();
        final java.lang.Object other$senderId = other.getSenderId();
        if (this$senderId == null ? other$senderId != null : !this$senderId.equals(other$senderId)) return false;
        final java.lang.Object this$senderName = this.getSenderName();
        final java.lang.Object other$senderName = other.getSenderName();
        if (this$senderName == null ? other$senderName != null : !this$senderName.equals(other$senderName)) return false;
        final java.lang.Object this$senderType = this.getSenderType();
        final java.lang.Object other$senderType = other.getSenderType();
        if (this$senderType == null ? other$senderType != null : !this$senderType.equals(other$senderType)) return false;
        final java.lang.Object this$messageType = this.getMessageType();
        final java.lang.Object other$messageType = other.getMessageType();
        if (this$messageType == null ? other$messageType != null : !this$messageType.equals(other$messageType)) return false;
        final java.lang.Object this$content = this.getContent();
        final java.lang.Object other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
        final java.lang.Object this$imageUrl = this.getImageUrl();
        final java.lang.Object other$imageUrl = other.getImageUrl();
        if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) return false;
        final java.lang.Object this$timestamp = this.getTimestamp();
        final java.lang.Object other$timestamp = other.getTimestamp();
        if (this$timestamp == null ? other$timestamp != null : !this$timestamp.equals(other$timestamp)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ChatMessageDto;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $sessionId = this.getSessionId();
        result = result * PRIME + ($sessionId == null ? 43 : $sessionId.hashCode());
        final java.lang.Object $senderId = this.getSenderId();
        result = result * PRIME + ($senderId == null ? 43 : $senderId.hashCode());
        final java.lang.Object $senderName = this.getSenderName();
        result = result * PRIME + ($senderName == null ? 43 : $senderName.hashCode());
        final java.lang.Object $senderType = this.getSenderType();
        result = result * PRIME + ($senderType == null ? 43 : $senderType.hashCode());
        final java.lang.Object $messageType = this.getMessageType();
        result = result * PRIME + ($messageType == null ? 43 : $messageType.hashCode());
        final java.lang.Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        final java.lang.Object $imageUrl = this.getImageUrl();
        result = result * PRIME + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        final java.lang.Object $timestamp = this.getTimestamp();
        result = result * PRIME + ($timestamp == null ? 43 : $timestamp.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ChatMessageDto(id=" + this.getId() + ", sessionId=" + this.getSessionId() + ", senderId=" + this.getSenderId() + ", senderName=" + this.getSenderName() + ", senderType=" + this.getSenderType() + ", messageType=" + this.getMessageType() + ", content=" + this.getContent() + ", imageUrl=" + this.getImageUrl() + ", timestamp=" + this.getTimestamp() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessageDto() {
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessageDto(final UUID id, final UUID sessionId, final String senderId, final String senderName, final String senderType, final String messageType, final String content, final String imageUrl, final Instant timestamp) {
        this.id = id;
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderType = senderType;
        this.messageType = messageType;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }
}

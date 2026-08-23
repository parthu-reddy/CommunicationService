package com.fooddelivery.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
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

            @java.lang.SuppressWarnings("all")
    public static class ChatMessageBuilder {
        @java.lang.SuppressWarnings("all")
        private UUID id;
        @java.lang.SuppressWarnings("all")
        private UUID sessionId;
        @java.lang.SuppressWarnings("all")
        private String senderId;
        @java.lang.SuppressWarnings("all")
        private boolean messageType$set;
        @java.lang.SuppressWarnings("all")
        private String messageType$value;
        @java.lang.SuppressWarnings("all")
        private String content;
        @java.lang.SuppressWarnings("all")
        private boolean createdAt$set;
        @java.lang.SuppressWarnings("all")
        private Instant createdAt$value;

        @java.lang.SuppressWarnings("all")
        ChatMessageBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessage.ChatMessageBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessage.ChatMessageBuilder sessionId(final UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessage.ChatMessageBuilder senderId(final String senderId) {
            this.senderId = senderId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessage.ChatMessageBuilder messageType(final String messageType) {
            this.messageType$value = messageType;
            messageType$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessage.ChatMessageBuilder content(final String content) {
            this.content = content;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public ChatMessage.ChatMessageBuilder createdAt(final Instant createdAt) {
            this.createdAt$value = createdAt;
            createdAt$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ChatMessage build() {
            String messageType$value = this.messageType$value;
            if (!this.messageType$set) messageType$value = ChatMessage.$default$messageType();
            Instant createdAt$value = this.createdAt$value;
            if (!this.createdAt$set) createdAt$value = ChatMessage.$default$createdAt();
            return new ChatMessage(this.id, this.sessionId, this.senderId, messageType$value, this.content, createdAt$value);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ChatMessage.ChatMessageBuilder(id=" + this.id + ", sessionId=" + this.sessionId + ", senderId=" + this.senderId + ", messageType$value=" + this.messageType$value + ", content=" + this.content + ", createdAt$value=" + this.createdAt$value + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ChatMessage.ChatMessageBuilder builder() {
        return new ChatMessage.ChatMessageBuilder();
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
    public String getMessageType() {
        return this.messageType;
    }

    @java.lang.SuppressWarnings("all")
    public String getContent() {
        return this.content;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getCreatedAt() {
        return this.createdAt;
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
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    @java.lang.SuppressWarnings("all")
    public void setContent(final String content) {
        this.content = content;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessage() {
        this.messageType = ChatMessage.$default$messageType();
        this.createdAt = ChatMessage.$default$createdAt();
    }

    @java.lang.SuppressWarnings("all")
    public ChatMessage(final UUID id, final UUID sessionId, final String senderId, final String messageType, final String content, final Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.messageType = messageType;
        this.content = content;
        this.createdAt = createdAt;
    }

    private static Instant $default$createdAt() {
        return Instant.now();
    }

    private static String $default$messageType() {
        return "TEXT";
    }
}

package com.fooddelivery.chat.dto;


public class SendMessageRequest {
    private String content;
    private String messageType;


    @java.lang.SuppressWarnings("all")
    public static class SendMessageRequestBuilder {
        @java.lang.SuppressWarnings("all")
        private String content;
        @java.lang.SuppressWarnings("all")
        private String messageType;

        @java.lang.SuppressWarnings("all")
        SendMessageRequestBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SendMessageRequest.SendMessageRequestBuilder content(final String content) {
            this.content = content;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SendMessageRequest.SendMessageRequestBuilder messageType(final String messageType) {
            this.messageType = messageType;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public SendMessageRequest build() {
            return new SendMessageRequest(this.content, this.messageType);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "SendMessageRequest.SendMessageRequestBuilder(content=" + this.content + ", messageType=" + this.messageType + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static SendMessageRequest.SendMessageRequestBuilder builder() {
        return new SendMessageRequest.SendMessageRequestBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getContent() {
        return this.content;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessageType() {
        return this.messageType;
    }

    @java.lang.SuppressWarnings("all")
    public void setContent(final String content) {
        this.content = content;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SendMessageRequest)) return false;
        final SendMessageRequest other = (SendMessageRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$content = this.getContent();
        final java.lang.Object other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
        final java.lang.Object this$messageType = this.getMessageType();
        final java.lang.Object other$messageType = other.getMessageType();
        if (this$messageType == null ? other$messageType != null : !this$messageType.equals(other$messageType)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SendMessageRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        final java.lang.Object $messageType = this.getMessageType();
        result = result * PRIME + ($messageType == null ? 43 : $messageType.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SendMessageRequest(content=" + this.getContent() + ", messageType=" + this.getMessageType() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public SendMessageRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public SendMessageRequest(final String content, final String messageType) {
        this.content = content;
        this.messageType = messageType;
    }
}

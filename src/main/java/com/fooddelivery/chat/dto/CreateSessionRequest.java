package com.fooddelivery.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateSessionRequest {
    @NotBlank
    private String orderId;
    @NotEmpty
    @Valid
    private List<ParticipantDto> participants;


    @java.lang.SuppressWarnings("all")
    public static class CreateSessionRequestBuilder {
        @java.lang.SuppressWarnings("all")
        private String orderId;
        @java.lang.SuppressWarnings("all")
        private List<ParticipantDto> participants;

        @java.lang.SuppressWarnings("all")
        CreateSessionRequestBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CreateSessionRequest.CreateSessionRequestBuilder orderId(final String orderId) {
            this.orderId = orderId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CreateSessionRequest.CreateSessionRequestBuilder participants(final List<ParticipantDto> participants) {
            this.participants = participants;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public CreateSessionRequest build() {
            return new CreateSessionRequest(this.orderId, this.participants);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "CreateSessionRequest.CreateSessionRequestBuilder(orderId=" + this.orderId + ", participants=" + this.participants + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static CreateSessionRequest.CreateSessionRequestBuilder builder() {
        return new CreateSessionRequest.CreateSessionRequestBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getOrderId() {
        return this.orderId;
    }

    @java.lang.SuppressWarnings("all")
    public List<ParticipantDto> getParticipants() {
        return this.participants;
    }

    @java.lang.SuppressWarnings("all")
    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    @java.lang.SuppressWarnings("all")
    public void setParticipants(final List<ParticipantDto> participants) {
        this.participants = participants;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CreateSessionRequest)) return false;
        final CreateSessionRequest other = (CreateSessionRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$participants = this.getParticipants();
        final java.lang.Object other$participants = other.getParticipants();
        if (this$participants == null ? other$participants != null : !this$participants.equals(other$participants)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CreateSessionRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $participants = this.getParticipants();
        result = result * PRIME + ($participants == null ? 43 : $participants.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "CreateSessionRequest(orderId=" + this.getOrderId() + ", participants=" + this.getParticipants() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public CreateSessionRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public CreateSessionRequest(final String orderId, final List<ParticipantDto> participants) {
        this.orderId = orderId;
        this.participants = participants;
    }
}

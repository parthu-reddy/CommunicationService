package com.fooddelivery.chat.dto;

public class WebRtcSignal {
    private String sessionId;
    private String senderId;
    private String targetUserId;
    private String type; // e.g. "OFFER", "ANSWER", "CANDIDATE", "HANGUP"
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;


    @java.lang.SuppressWarnings("all")
    public static class WebRtcSignalBuilder {
        @java.lang.SuppressWarnings("all")
        private String sessionId;
        @java.lang.SuppressWarnings("all")
        private String senderId;
        @java.lang.SuppressWarnings("all")
        private String targetUserId;
        @java.lang.SuppressWarnings("all")
        private String type;
        @java.lang.SuppressWarnings("all")
        private String sdp;
        @java.lang.SuppressWarnings("all")
        private String candidate;
        @java.lang.SuppressWarnings("all")
        private String sdpMid;
        @java.lang.SuppressWarnings("all")
        private Integer sdpMLineIndex;

        @java.lang.SuppressWarnings("all")
        WebRtcSignalBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WebRtcSignal.WebRtcSignalBuilder sessionId(final String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WebRtcSignal.WebRtcSignalBuilder senderId(final String senderId) {
            this.senderId = senderId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WebRtcSignal.WebRtcSignalBuilder targetUserId(final String targetUserId) {
            this.targetUserId = targetUserId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WebRtcSignal.WebRtcSignalBuilder type(final String type) {
            this.type = type;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WebRtcSignal.WebRtcSignalBuilder sdp(final String sdp) {
            this.sdp = sdp;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WebRtcSignal.WebRtcSignalBuilder candidate(final String candidate) {
            this.candidate = candidate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WebRtcSignal.WebRtcSignalBuilder sdpMid(final String sdpMid) {
            this.sdpMid = sdpMid;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public WebRtcSignal.WebRtcSignalBuilder sdpMLineIndex(final Integer sdpMLineIndex) {
            this.sdpMLineIndex = sdpMLineIndex;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public WebRtcSignal build() {
            return new WebRtcSignal(this.sessionId, this.senderId, this.targetUserId, this.type, this.sdp, this.candidate, this.sdpMid, this.sdpMLineIndex);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "WebRtcSignal.WebRtcSignalBuilder(sessionId=" + this.sessionId + ", senderId=" + this.senderId + ", targetUserId=" + this.targetUserId + ", type=" + this.type + ", sdp=" + this.sdp + ", candidate=" + this.candidate + ", sdpMid=" + this.sdpMid + ", sdpMLineIndex=" + this.sdpMLineIndex + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static WebRtcSignal.WebRtcSignalBuilder builder() {
        return new WebRtcSignal.WebRtcSignalBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public String getSessionId() {
        return this.sessionId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSenderId() {
        return this.senderId;
    }

    @java.lang.SuppressWarnings("all")
    public String getTargetUserId() {
        return this.targetUserId;
    }

    @java.lang.SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    @java.lang.SuppressWarnings("all")
    public String getSdp() {
        return this.sdp;
    }

    @java.lang.SuppressWarnings("all")
    public String getCandidate() {
        return this.candidate;
    }

    @java.lang.SuppressWarnings("all")
    public String getSdpMid() {
        return this.sdpMid;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getSdpMLineIndex() {
        return this.sdpMLineIndex;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSenderId(final String senderId) {
        this.senderId = senderId;
    }

    @java.lang.SuppressWarnings("all")
    public void setTargetUserId(final String targetUserId) {
        this.targetUserId = targetUserId;
    }

    @java.lang.SuppressWarnings("all")
    public void setType(final String type) {
        this.type = type;
    }

    @java.lang.SuppressWarnings("all")
    public void setSdp(final String sdp) {
        this.sdp = sdp;
    }

    @java.lang.SuppressWarnings("all")
    public void setCandidate(final String candidate) {
        this.candidate = candidate;
    }

    @java.lang.SuppressWarnings("all")
    public void setSdpMid(final String sdpMid) {
        this.sdpMid = sdpMid;
    }

    @java.lang.SuppressWarnings("all")
    public void setSdpMLineIndex(final Integer sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof WebRtcSignal)) return false;
        final WebRtcSignal other = (WebRtcSignal) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$sdpMLineIndex = this.getSdpMLineIndex();
        final java.lang.Object other$sdpMLineIndex = other.getSdpMLineIndex();
        if (this$sdpMLineIndex == null ? other$sdpMLineIndex != null : !this$sdpMLineIndex.equals(other$sdpMLineIndex)) return false;
        final java.lang.Object this$sessionId = this.getSessionId();
        final java.lang.Object other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) return false;
        final java.lang.Object this$senderId = this.getSenderId();
        final java.lang.Object other$senderId = other.getSenderId();
        if (this$senderId == null ? other$senderId != null : !this$senderId.equals(other$senderId)) return false;
        final java.lang.Object this$targetUserId = this.getTargetUserId();
        final java.lang.Object other$targetUserId = other.getTargetUserId();
        if (this$targetUserId == null ? other$targetUserId != null : !this$targetUserId.equals(other$targetUserId)) return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final java.lang.Object this$sdp = this.getSdp();
        final java.lang.Object other$sdp = other.getSdp();
        if (this$sdp == null ? other$sdp != null : !this$sdp.equals(other$sdp)) return false;
        final java.lang.Object this$candidate = this.getCandidate();
        final java.lang.Object other$candidate = other.getCandidate();
        if (this$candidate == null ? other$candidate != null : !this$candidate.equals(other$candidate)) return false;
        final java.lang.Object this$sdpMid = this.getSdpMid();
        final java.lang.Object other$sdpMid = other.getSdpMid();
        if (this$sdpMid == null ? other$sdpMid != null : !this$sdpMid.equals(other$sdpMid)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof WebRtcSignal;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $sdpMLineIndex = this.getSdpMLineIndex();
        result = result * PRIME + ($sdpMLineIndex == null ? 43 : $sdpMLineIndex.hashCode());
        final java.lang.Object $sessionId = this.getSessionId();
        result = result * PRIME + ($sessionId == null ? 43 : $sessionId.hashCode());
        final java.lang.Object $senderId = this.getSenderId();
        result = result * PRIME + ($senderId == null ? 43 : $senderId.hashCode());
        final java.lang.Object $targetUserId = this.getTargetUserId();
        result = result * PRIME + ($targetUserId == null ? 43 : $targetUserId.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $sdp = this.getSdp();
        result = result * PRIME + ($sdp == null ? 43 : $sdp.hashCode());
        final java.lang.Object $candidate = this.getCandidate();
        result = result * PRIME + ($candidate == null ? 43 : $candidate.hashCode());
        final java.lang.Object $sdpMid = this.getSdpMid();
        result = result * PRIME + ($sdpMid == null ? 43 : $sdpMid.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "WebRtcSignal(sessionId=" + this.getSessionId() + ", senderId=" + this.getSenderId() + ", targetUserId=" + this.getTargetUserId() + ", type=" + this.getType() + ", sdp=" + this.getSdp() + ", candidate=" + this.getCandidate() + ", sdpMid=" + this.getSdpMid() + ", sdpMLineIndex=" + this.getSdpMLineIndex() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public WebRtcSignal() {
    }

    @java.lang.SuppressWarnings("all")
    public WebRtcSignal(final String sessionId, final String senderId, final String targetUserId, final String type, final String sdp, final String candidate, final String sdpMid, final Integer sdpMLineIndex) {
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.targetUserId = targetUserId;
        this.type = type;
        this.sdp = sdp;
        this.candidate = candidate;
        this.sdpMid = sdpMid;
        this.sdpMLineIndex = sdpMLineIndex;
    }
}

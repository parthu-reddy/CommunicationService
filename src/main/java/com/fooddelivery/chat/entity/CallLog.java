package com.fooddelivery.chat.entity;

import com.fooddelivery.chat.enums.CallStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_logs")
public class CallLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    @Column(name = "caller_id", nullable = false)
    private String callerId;
    @Column(name = "callee_id", nullable = false)
    private String calleeId;
    @Column(name = "start_time")
    private Instant startTime;
    @Column(name = "end_time")
    private Instant endTime;
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CallStatus status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @java.lang.SuppressWarnings("all")
    private static Instant $default$createdAt() {
        return Instant.now();
    }

    @java.lang.SuppressWarnings("all")
    private static Instant $default$updatedAt() {
        return Instant.now();
    }


    @java.lang.SuppressWarnings("all")
    public static class CallLogBuilder {
        @java.lang.SuppressWarnings("all")
        private UUID id;
        @java.lang.SuppressWarnings("all")
        private UUID sessionId;
        @java.lang.SuppressWarnings("all")
        private String callerId;
        @java.lang.SuppressWarnings("all")
        private String calleeId;
        @java.lang.SuppressWarnings("all")
        private Instant startTime;
        @java.lang.SuppressWarnings("all")
        private Instant endTime;
        @java.lang.SuppressWarnings("all")
        private Integer durationSeconds;
        @java.lang.SuppressWarnings("all")
        private CallStatus status;
        @java.lang.SuppressWarnings("all")
        private boolean createdAt$set;
        @java.lang.SuppressWarnings("all")
        private Instant createdAt$value;
        @java.lang.SuppressWarnings("all")
        private boolean updatedAt$set;
        @java.lang.SuppressWarnings("all")
        private Instant updatedAt$value;

        @java.lang.SuppressWarnings("all")
        CallLogBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder sessionId(final UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder callerId(final String callerId) {
            this.callerId = callerId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder calleeId(final String calleeId) {
            this.calleeId = calleeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder startTime(final Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder endTime(final Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder durationSeconds(final Integer durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder status(final CallStatus status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder createdAt(final Instant createdAt) {
            this.createdAt$value = createdAt;
            createdAt$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CallLog.CallLogBuilder updatedAt(final Instant updatedAt) {
            this.updatedAt$value = updatedAt;
            updatedAt$set = true;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public CallLog build() {
            Instant createdAt$value = this.createdAt$value;
            if (!this.createdAt$set) createdAt$value = CallLog.$default$createdAt();
            Instant updatedAt$value = this.updatedAt$value;
            if (!this.updatedAt$set) updatedAt$value = CallLog.$default$updatedAt();
            return new CallLog(this.id, this.sessionId, this.callerId, this.calleeId, this.startTime, this.endTime, this.durationSeconds, this.status, createdAt$value, updatedAt$value);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "CallLog.CallLogBuilder(id=" + this.id + ", sessionId=" + this.sessionId + ", callerId=" + this.callerId + ", calleeId=" + this.calleeId + ", startTime=" + this.startTime + ", endTime=" + this.endTime + ", durationSeconds=" + this.durationSeconds + ", status=" + this.status + ", createdAt$value=" + this.createdAt$value + ", updatedAt$value=" + this.updatedAt$value + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static CallLog.CallLogBuilder builder() {
        return new CallLog.CallLogBuilder();
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
    public String getCallerId() {
        return this.callerId;
    }

    @java.lang.SuppressWarnings("all")
    public String getCalleeId() {
        return this.calleeId;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getStartTime() {
        return this.startTime;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getEndTime() {
        return this.endTime;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDurationSeconds() {
        return this.durationSeconds;
    }

    @java.lang.SuppressWarnings("all")
    public CallStatus getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getUpdatedAt() {
        return this.updatedAt;
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
    public void setCallerId(final String callerId) {
        this.callerId = callerId;
    }

    @java.lang.SuppressWarnings("all")
    public void setCalleeId(final String calleeId) {
        this.calleeId = calleeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStartTime(final Instant startTime) {
        this.startTime = startTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setEndTime(final Instant endTime) {
        this.endTime = endTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setDurationSeconds(final Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final CallStatus status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public CallLog() {
        this.createdAt = CallLog.$default$createdAt();
        this.updatedAt = CallLog.$default$updatedAt();
    }

    @java.lang.SuppressWarnings("all")
    public CallLog(final UUID id, final UUID sessionId, final String callerId, final String calleeId, final Instant startTime, final Instant endTime, final Integer durationSeconds, final CallStatus status, final Instant createdAt, final Instant updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.callerId = callerId;
        this.calleeId = calleeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationSeconds = durationSeconds;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

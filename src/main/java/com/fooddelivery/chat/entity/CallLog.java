package com.fooddelivery.chat.entity;

import com.fooddelivery.chat.enums.CallStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_logs")@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder

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

}

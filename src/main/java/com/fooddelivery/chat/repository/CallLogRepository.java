package com.fooddelivery.chat.repository;

import com.fooddelivery.chat.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, UUID> {
    List<CallLog> findBySessionId(UUID sessionId);
    Optional<CallLog> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}

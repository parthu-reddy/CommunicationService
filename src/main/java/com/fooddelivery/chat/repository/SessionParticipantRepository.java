package com.fooddelivery.chat.repository;

import com.fooddelivery.chat.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {
    List<SessionParticipant> findByUserId(String userId);
    List<SessionParticipant> findByChatSessionId(UUID sessionId);
    boolean existsByChatSessionIdAndUserId(UUID sessionId, String userId);
}

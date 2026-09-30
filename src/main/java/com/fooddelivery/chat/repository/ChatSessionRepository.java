package com.fooddelivery.chat.repository;

import com.fooddelivery.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Optional<ChatSession> findBySessionTypeAndReferenceId(String sessionType, String referenceId);

    /**
     * Serializes first creation of one order chat. A row lock cannot protect the no-row-yet case;
     * this PostgreSQL transaction advisory lock does, and the unique index is the final database
     * invariant.
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(:lockKey))) AS locked", nativeQuery = true)
    Integer lockOrderSessionCreation(@Param("lockKey") String lockKey);
}

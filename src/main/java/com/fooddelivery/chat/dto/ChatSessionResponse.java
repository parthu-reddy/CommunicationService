package com.fooddelivery.chat.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder


public class ChatSessionResponse {
    private UUID sessionId;
    private String sessionType;
    private String referenceId;
    private Boolean isActive;
    private Instant createdAt;
    private List<ParticipantDto> participants;


}

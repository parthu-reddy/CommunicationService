package com.fooddelivery.chat.dto;

import java.time.Instant;
import java.util.UUID;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder


public class ChatMessageDto {
    private UUID id;
    private UUID sessionId;
    private String senderId;
    private String senderName;
    private String senderType;
    private String messageType;
    private String content;
    private String imageUrl;
    private Instant timestamp;


}

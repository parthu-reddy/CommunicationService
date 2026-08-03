package com.fooddelivery.chat.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SendMessageRequest {
    private String content;
    private String messageType;
}

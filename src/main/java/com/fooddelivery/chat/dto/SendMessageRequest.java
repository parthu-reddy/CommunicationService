package com.fooddelivery.chat.dto;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder



public class SendMessageRequest {
    /** Optional speaking entity; the server checks this against the canonical roster and membership. */
    private String senderEntityType;
    private String content;
    private String messageType;


}

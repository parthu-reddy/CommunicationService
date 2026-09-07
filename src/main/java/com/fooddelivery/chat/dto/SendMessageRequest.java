package com.fooddelivery.chat.dto;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder



public class SendMessageRequest {
    private String content;
    private String messageType;


}

package com.fooddelivery.chat.dto;

import jakarta.validation.constraints.NotBlank;
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder


public class CreateSessionRequest {
    @NotBlank
    @com.fasterxml.jackson.annotation.JsonProperty(required = true)
    private String orderId;
}

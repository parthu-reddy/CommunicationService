package com.fooddelivery.chat.dto;

import jakarta.validation.constraints.NotBlank;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder


public class ParticipantDto {
    @NotBlank
    private String userId;
    @NotBlank
    private String entityType;
    private String displayName;


}

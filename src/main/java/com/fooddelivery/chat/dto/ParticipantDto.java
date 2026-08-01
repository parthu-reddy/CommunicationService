package com.fooddelivery.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ParticipantDto {
    @NotBlank
    private String userId;

    @NotBlank
    private String entityType;

    private String displayName;
}

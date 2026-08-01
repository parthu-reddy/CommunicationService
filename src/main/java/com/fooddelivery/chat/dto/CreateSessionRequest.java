package com.fooddelivery.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateSessionRequest {
    @NotBlank
    private String orderId;

    @NotEmpty
    @Valid
    private List<ParticipantDto> participants;
}

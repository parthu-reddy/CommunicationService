package com.fooddelivery.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder


public class CreateSessionRequest {
    @NotBlank
    @com.fasterxml.jackson.annotation.JsonProperty(required = true)
    private String orderId;
    @NotEmpty
    @Valid
    private List<ParticipantDto> participants;


}

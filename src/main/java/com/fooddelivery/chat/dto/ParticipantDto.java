package com.fooddelivery.chat.dto;

import jakarta.validation.constraints.NotBlank;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder


public class ParticipantDto {
    /** A restaurant participant represents an outlet, not a permanently selected employee. */
    private String userId;
    @NotBlank
    private String entityId;
    @NotBlank
    private String entityType;
    private String displayName;
    /** Current authorised call recipients; never stored as outlet ownership. */
    private java.util.List<String> contactUserIds;


}

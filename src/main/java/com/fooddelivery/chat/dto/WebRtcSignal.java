package com.fooddelivery.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRtcSignal {
    private String sessionId;
    private String senderId;
    private String targetUserId;
    private String type; // e.g. "OFFER", "ANSWER", "CANDIDATE", "HANGUP"
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
}

package com.fooddelivery.chat.dto;@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
@lombok.Builder


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

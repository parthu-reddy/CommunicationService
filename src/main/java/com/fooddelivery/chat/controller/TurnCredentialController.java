package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.TurnCredentialsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/webrtc")
public class TurnCredentialController {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TurnCredentialController.class);

    @GetMapping("/ice-servers")
    public ResponseEntity<TurnCredentialsResponse> getIceServers() {
        // In a real production environment, this would call Twilio's Network Traversal Service API
        // or a CoTURN server to generate ephemeral credentials.
        // For MVP, we provide public STUN servers and a fallback open TURN server if needed,
        // or just the STUN server for local development.
        log.info("Generating ICE server credentials for WebRTC");
        TurnCredentialsResponse response = TurnCredentialsResponse.builder().iceServers(List.of(TurnCredentialsResponse.IceServer.builder().urls("stun:stun.l.google.com:19302").build(), TurnCredentialsResponse.IceServer.builder().urls("stun:stun1.l.google.com:19302").build(), TurnCredentialsResponse.IceServer.builder().urls("turn:openrelay.metered.ca:80").username("openrelayproject").credential("openrelayproject").build(), TurnCredentialsResponse.IceServer.builder().urls("turn:openrelay.metered.ca:443").username("openrelayproject").credential("openrelayproject").build(), TurnCredentialsResponse.IceServer.builder().urls("turn:openrelay.metered.ca:443?transport=tcp").username("openrelayproject").credential("openrelayproject").build())).build();
        return ResponseEntity.ok(response);
    }
}

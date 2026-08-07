package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.service.ChatMessageService;
import com.fooddelivery.chat.service.ChatSessionService;
import com.fooddelivery.common.service.CloudflareR2Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for uploading audio call recordings in chat sessions.
 * Audio is uploaded to Cloudflare R2 with the folder structure: chat/{sessionId}/
 * After upload, an AUDIO type message is auto-saved and broadcast to all participants.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatAudioUploadController {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatAudioUploadController.class);
    private final CloudflareR2Service cloudflareR2Service;
    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final SimpMessageSendingOperations messagingTemplate;
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB for audio recordings

    @PostMapping("/sessions/{sessionId}/upload-audio")
    public ResponseEntity<Map<String, Object>> uploadAudio(@PathVariable UUID sessionId, @RequestParam("file") MultipartFile file, Authentication authentication) {
        // 1. Authentication check
        String userId = authentication != null ? authentication.getName() : null;
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Authentication required"));
        }
        // 2. Authorization check — must be a participant
        if (!sessionService.isParticipant(sessionId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Access Denied: Not a participant of this chat session"));
        }
        // 3. Validate file presence
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "A file must be provided"));
        }
        // 4. Validate content type (allow audio and video/webm because MediaRecorder default is video/webm in some browsers)
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("audio/") && !contentType.startsWith("video/webm") && !contentType.startsWith("video/mp4"))) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Only audio/webm or audio/mp4 files are allowed"));
        }
        // 5. Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File size exceeds 25MB limit"));
        }
        try {
            byte[] audioBytes = file.getBytes();
            // 7. Determine and sanitize file extension
            String extension = ".webm";
            if (contentType.contains("mp4")) {
                extension = ".mp4";
            }
            // 8. Upload to Cloudflare R2 with folder = chat/{sessionId}
            String folder = "chat/" + sessionId.toString();
            String fileName = "call_recording_" + UUID.randomUUID().toString() + extension;
            String publicUrl = cloudflareR2Service.uploadImage(audioBytes, folder, fileName, contentType);
            log.info("Chat audio recorded and uploaded: session={}, user={}, url={}", sessionId, userId, publicUrl);
            // 9. Auto-save an AUDIO message and broadcast to all subscribers
            ChatMessageDto savedMessage = messageService.saveMessage(sessionId, userId, publicUrl,  // content is the audio URL
            "AUDIO");
            // Broadcast the audio message via STOMP
            messagingTemplate.convertAndSend("/topic/chat/" + sessionId.toString(), savedMessage);
            return ResponseEntity.ok(Map.of("success", true, "message", "Audio recording uploaded successfully", "data", Map.of("audioUrl", publicUrl, "messageId", savedMessage.getId().toString())));
        } catch (Exception e) {
            log.error("Failed to upload chat audio recording for session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to upload audio recording: " + e.getMessage()));
        }
    }

    @java.lang.SuppressWarnings("all")
    public ChatAudioUploadController(final CloudflareR2Service cloudflareR2Service, final ChatSessionService sessionService, final ChatMessageService messageService, final SimpMessageSendingOperations messagingTemplate) {
        this.cloudflareR2Service = cloudflareR2Service;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }
}

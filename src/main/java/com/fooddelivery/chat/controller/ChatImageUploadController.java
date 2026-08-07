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
 * REST controller for uploading images in chat sessions.
 * Images are uploaded to Cloudflare R2 with the folder structure: chat/{sessionId}/
 * After upload, an IMAGE-type message is auto-saved and broadcast to all participants.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatImageUploadController {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatImageUploadController.class);
    private final CloudflareR2Service cloudflareR2Service;
    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final SimpMessageSendingOperations messagingTemplate;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_IMAGE_DIMENSION = 5000; // pixels

    @PostMapping("/sessions/{sessionId}/upload-image")
    public ResponseEntity<Map<String, Object>> uploadImage(@PathVariable UUID sessionId, @RequestParam("file") MultipartFile file, Authentication authentication) {
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
        // 4. Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Only image files are allowed"));
        }
        // 5. Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File size exceeds 5MB limit"));
        }
        // 6. Check image count limit (max 4 per session per user)
        long currentImageCount = messageService.countImagesInSessionByUser(sessionId, userId);
        if (currentImageCount >= 4) {
            log.warn("Upload rejected: User {} in Session {} reached the maximum limit of 4 images. Current count: {}", userId, sessionId, currentImageCount);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Maximum limit of 4 images per user reached for this session"));
        }
        try {
            byte[] imageBytes = file.getBytes();
            // 6. Validate image dimensions (prevent decompression bomb DoS)
            validateImageDimensions(imageBytes);
            // 7. Determine and sanitize file extension
            String extension = ".jpg";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                String extractedExt = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
                if (extractedExt.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                    extension = extractedExt;
                }
            }
            // 8. Upload to Cloudflare R2 with folder = chat/{sessionId}
            String folder = "chat/" + sessionId.toString();
            String fileName = UUID.randomUUID().toString() + extension;
            String publicUrl = cloudflareR2Service.uploadImage(imageBytes, folder, fileName, contentType);
            log.info("Chat image uploaded: session={}, user={}, url={}", sessionId, userId, publicUrl);
            // 9. Auto-save an IMAGE message and broadcast to all subscribers
            ChatMessageDto savedMessage = messageService.saveMessage(sessionId, userId, publicUrl,  // content is the image URL
            "IMAGE");
            // Broadcast the image message via STOMP
            messagingTemplate.convertAndSend("/topic/chat/" + sessionId.toString(), savedMessage);
            return ResponseEntity.ok(Map.of("success", true, "message", "Image uploaded successfully", "data", Map.of("imageUrl", publicUrl, "messageId", savedMessage.getId().toString())));
        } catch (Exception e) {
            log.error("Failed to upload chat image for session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to upload image: " + e.getMessage()));
        }
    }

    /**
     * Validates image dimensions to prevent decompression bomb attacks.
     * A tiny compressed file could decompress to a massive bitmap in memory.
     */
    private void validateImageDimensions(byte[] imageBytes) throws Exception {
        try (
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes);
            javax.imageio.stream.ImageInputStream iis = javax.imageio.ImageIO.createImageInputStream(bais)) {
            java.util.Iterator<javax.imageio.ImageReader> readers = javax.imageio.ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                javax.imageio.ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                        throw new Exception("Image dimensions (" + width + "x" + height + ") exceed the " + MAX_IMAGE_DIMENSION + "x" + MAX_IMAGE_DIMENSION + " safety limit.");
                    }
                } finally {
                    reader.dispose();
                }
            } else {
                throw new Exception("Unsupported or invalid image format.");
            }
        }
    }

    @java.lang.SuppressWarnings("all")
    public ChatImageUploadController(final CloudflareR2Service cloudflareR2Service, final ChatSessionService sessionService, final ChatMessageService messageService, final SimpMessageSendingOperations messagingTemplate) {
        this.cloudflareR2Service = cloudflareR2Service;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }
}

package com.fooddelivery.chat.controller;

import com.fooddelivery.chat.dto.ChatMessageDto;
import com.fooddelivery.chat.dto.ParticipantDto;
import com.fooddelivery.chat.service.*;
import com.fooddelivery.common.service.CloudflareR2Service;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Validates the entity hint before media writes, and forwards it for authoritative sender resolution. */
class ChatMediaSenderIdentityTest {
    private final CloudflareR2Service media=mock(CloudflareR2Service.class);
    private final ChatSessionService sessions=mock(ChatSessionService.class);
    private final ChatSessionAccessService access=mock(ChatSessionAccessService.class);
    private final ChatMessageService messages=mock(ChatMessageService.class);
    private final ChatEventBroadcaster broadcaster=mock(ChatEventBroadcaster.class);
    private final UUID session=UUID.randomUUID();
    private final String user=UUID.randomUUID().toString();
    private final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken(
        user,null,List.of(new SimpleGrantedAuthority("ROLE_RESTAURANT")));

    @ParameterizedTest @ValueSource(strings={"IMAGE","AUDIO"})
    void authorisedStaffMediaKeepsItsRestaurantEntity(String type) throws Exception {
        when(access.canAccessSession(session,auth)).thenReturn(true);
        when(access.participantForUser(session,user,"RESTAURANT")).thenReturn(Optional.of(
            ParticipantDto.builder().entityId(UUID.randomUUID().toString()).entityType("RESTAURANT").build()));
        when(media.uploadImage(any(byte[].class),anyString(),anyString(),anyString())).thenReturn("https://media.test/file");
        when(messages.saveMessage(session,user,"https://media.test/file",type,"RESTAURANT"))
            .thenReturn(ChatMessageDto.builder().id(UUID.randomUUID()).build());
        assertEquals(200,upload(type,"RESTAURANT").getStatusCode().value());
        verify(messages).saveMessage(session,user,"https://media.test/file",type,"RESTAURANT");
        verify(broadcaster).broadcastMessage(eq(session),any(ChatMessageDto.class));
    }

    @ParameterizedTest @ValueSource(strings={"IMAGE","AUDIO"})
    void forgedParticipantEntityIsDeniedBeforeAnyMediaWrite(String type) throws Exception {
        when(access.canAccessSession(session,auth)).thenReturn(true);
        when(access.participantForUser(session,user,"DELIVERY")).thenReturn(Optional.empty());
        assertEquals(403,upload(type,"DELIVERY").getStatusCode().value());
        verifyNoInteractions(media,messages,broadcaster);
    }

    private org.springframework.http.ResponseEntity<?> upload(String type,String entity) throws Exception {
        if("AUDIO".equals(type)){
            return new ChatAudioUploadController(media,sessions,access,messages,broadcaster).uploadAudio(
                session,new MockMultipartFile("file","audio.webm","audio/webm",new byte[]{1,2,3}),entity,auth);
        }
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"png",bytes);
        return new ChatImageUploadController(media,sessions,access,messages,broadcaster).uploadImage(
            session,new MockMultipartFile("file","image.png","image/png",bytes.toByteArray()),entity,auth);
    }
}

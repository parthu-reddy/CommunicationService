package com.fooddelivery.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.chat.entity.ChatMessage;
import com.fooddelivery.chat.repository.ChatMessageRepository;
import com.fooddelivery.common.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** One local history fixture covers newest/older windows, isolation and timestamp tie ordering. */
@DataJpaTest(showSql=false)
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace=org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages="com.fooddelivery.chat.entity")
@EnableJpaRepositories(basePackages="com.fooddelivery.chat.repository")
@Import({ChatMessageService.class,ChatHistoryWindowTest.Config.class})
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:chatHistory;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.flyway.enabled=false","spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false","eureka.client.enabled=false"})
class ChatHistoryWindowTest {
    @TestConfiguration static class Config { @Bean ObjectMapper mapper(){return new ObjectMapper();} }
    @Autowired ChatMessageRepository messages;
    @Autowired ChatMessageService service;
    @MockBean OutboxEventRepository outbox;
    @MockBean ChatSessionAccessService access;

    @Test void newestWindowAndOlderWindowContainOnlyTheSelectedSessionAndUseStableTies() {
        UUID session=UUID.randomUUID(),other=UUID.randomUUID();
        Instant base=Instant.parse("2026-01-01T00:00:00Z");
        for(int i=0;i<61;i++)messages.save(ChatMessage.builder().sessionId(session).senderId("customer").senderEntityId("customer")
                .senderName("Customer").senderType("CUSTOMER").messageType("TEXT").content("message-"+i)
                .createdAt(base.plusSeconds(i)).build());
        messages.saveAndFlush(ChatMessage.builder().sessionId(other).senderId("other-user").senderEntityId("other-user")
                .messageType("TEXT").content("private-other-session").createdAt(base.plusSeconds(500)).build());
        var latest=service.getMessageHistory(session,0,50);
        var older=service.getMessageHistory(session,1,50);
        assertEquals(61,latest.getTotalElements());assertEquals(50,latest.getContent().size());
        assertEquals("message-60",latest.getContent().get(0).getContent());
        assertEquals("message-11",latest.getContent().get(49).getContent());
        assertEquals(11,older.getContent().size());assertEquals("message-10",older.getContent().get(0).getContent());
        assertEquals("message-0",older.getContent().get(10).getContent());
        var all=new HashSet<UUID>();latest.forEach(m->{assertEquals(session,m.getSessionId());assertTrue(all.add(m.getId()));});
        older.forEach(m->{assertEquals(session,m.getSessionId());assertTrue(all.add(m.getId()));});assertEquals(61,all.size());
        assertTrue(service.getMessageHistory(session,2,50).isEmpty());
        // Persist same-time messages to prove a stable page boundary rather than relying on
        // unspecified database ordering for timestamp ties.
        UUID tieSession=UUID.randomUUID();
        for(int i=0;i<3;i++)messages.save(ChatMessage.builder().sessionId(tieSession).senderId("customer").senderEntityId("customer")
                .messageType("TEXT").content("tie-"+i).createdAt(base).build());
        messages.flush();
        var expected=messages.findAll().stream().filter(m->m.getSessionId().equals(tieSession))
                .map(ChatMessage::getId).sorted(Comparator.comparing(UUID::toString).reversed()).toList();
        var actual=new ArrayList<>(service.getMessageHistory(tieSession,0,2).stream().map(m->m.getId()).toList());
        actual.addAll(service.getMessageHistory(tieSession,1,2).stream().map(m->m.getId()).toList());
        assertEquals(expected,actual);
    }
}

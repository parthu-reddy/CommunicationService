package com.fooddelivery.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

@SpringBootTest(classes = OpenApiGenerationTest.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_openapi;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "springdoc.writer-with-default-pretty-printer=true",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.kafka.bootstrap-servers=localhost:9092", "spring.kafka.listener.auto-startup=false", "spring.kafka.admin.fail-fast=true",
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.redis.enabled=false",
    "management.health.redis.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureWebTestClient
public class OpenApiGenerationTest {

    @org.springframework.context.annotation.Configuration
    // Relabels structured responses from */* to application/json. Without it every
    // generated Zod response validator degrades to z.void(); the scoped scan below
    // does not reach com.fooddelivery.common.config.
    @org.springframework.context.annotation.Import({com.fooddelivery.common.config.OpenApiJsonMediaTypeCustomizer.class, com.fooddelivery.common.config.OpenApiPaginationRequiredCustomizer.class})
    @org.springframework.context.annotation.ComponentScan(basePackages = {"com.fooddelivery.chat.controller"})
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration(excludeName = {"org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration", "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration", "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration", "org.springframework.boot.actuate.autoconfigure.security.reactive.ManagementReactiveSecurityAutoConfiguration", "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"})
    static class TestApp {
    }

    @MockBean
    private com.fooddelivery.chat.service.ChatSessionService chatSessionService;
    
    @MockBean
    private com.fooddelivery.chat.service.ChatMessageService chatMessageService;
    
    @MockBean
    private com.fooddelivery.common.service.CloudflareR2Service cloudflareR2Service;
    
    @MockBean
    private com.fooddelivery.chat.service.CallLogService callLogService;
    
    @MockBean
    private com.fooddelivery.common.service.RateLimitingService rateLimitingService;

    @MockBean
    private org.springframework.messaging.simp.SimpMessageSendingOperations messagingTemplate;
    @MockBean
    private com.fooddelivery.common.client.RestaurantServiceClient restaurantServiceClient;
    @MockBean
    private com.fooddelivery.common.client.CustomerServiceClient customerServiceClient;


    @Autowired(required = false)
    private MockMvc mockMvc;

    @Autowired(required = false)
    private WebTestClient webTestClient;

    @Test
    public void generateOpenApi() throws Exception {
        String openApiJson = null;

        if (mockMvc != null) {
            openApiJson = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        } else if (webTestClient != null) {
            byte[] responseBody = webTestClient.get().uri("/v3/api-docs").exchange()
                    .expectStatus().isOk()
                    .expectBody().returnResult().getResponseBody();
            if (responseBody != null) {
                openApiJson = new String(responseBody, StandardCharsets.UTF_8);
            }
        } else {
            throw new IllegalStateException("Neither MockMvc nor WebTestClient is available.");
        }

        if (openApiJson != null && !openApiJson.isEmpty()) {
            Path path = Paths.get("target/openapi.json");
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.write(path, openApiJson.getBytes(StandardCharsets.UTF_8));
            System.out.println("OpenAPI spec written to target/openapi.json");
        } else {
            throw new IllegalStateException("Failed to retrieve OpenAPI spec.");
        }
    }
}

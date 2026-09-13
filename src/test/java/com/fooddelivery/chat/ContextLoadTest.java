package com.fooddelivery.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real application context starts.
 *
 * <p>Added 2026-09-13. This service had no full-context test, so the CommonLibrary split was verified
 * here by reading poms rather than by watching Spring start. That matters because
 * {@code AutoConfiguration.imports} is resolved at startup and Spring Boot aborts on an entry it
 * cannot load: a compiler never sees it, and neither does a unit test that avoids a context.
 *
 * <p>Uses {@code contract-test}, the profile already configured to boot without external
 * infrastructure — H2 for Postgres, the config server off. Beans that need a real server are mocked
 * individually rather than by excluding auto-configuration, so everything common-library contributes
 * is still created for real.
 */
// classes = ... because ContractConsumerTest declares a nested @SpringBootConfiguration in this
// package, and Spring Boot refuses to guess between them: "Found multiple @SpringBootConfiguration
// annotated classes". Naming the real application class is the point of the test anyway.
@SpringBootTest(classes = ChatServiceApplication.class, properties =
        // The contract-test profile sets web-application-type=none, so no servlet security
        // auto-configuration runs and SecurityProperties is never created -- but
        // UserDetailsServiceAutoConfiguration still fires and tries to build a default in-memory
        // user from it. The application excludes CommonSecurityConfig from its scan, so nothing
        // supplies a UserDetailsService either. Excluded rather than stubbed: a default in-memory
        // user is not part of what this test is checking.
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration")
@ActiveProfiles("contract-test")
class ContextLoadTest {

    // The contract-test profile excludes Redis auto-configuration, so nothing supplies these.
    // Mocked individually rather than by excluding more auto-configuration: the point of this test is
    // that everything else -- including every bean common-library contributes -- is created for real.
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.fooddelivery.common.service.RateLimitingService rateLimitingService;

    // Same as ONDC: SecurityContextFilter is @Profile("!contract-test") and ChatSecurityConfig needs it.
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.fooddelivery.common.security.SecurityContextFilter securityContextFilter;

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertTrue(context.getBeanDefinitionCount() > 0, "an empty context is not a started one");
    }
}

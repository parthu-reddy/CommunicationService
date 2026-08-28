package com.fooddelivery.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = ContractConsumerTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "customer-service.url=http://localhost:${stubrunner.runningstubs.food-delivery-backend.port}",
    "restaurant-service.url=http://localhost:${stubrunner.runningstubs.restaurant-application.port}"
})
@AutoConfigureStubRunner(ids = {
    "com.fooddelivery:food-delivery-backend:+:stubs",
    "com.fooddelivery:restaurant-application:+:stubs"
}, stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class ContractConsumerTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.web.client.RestTemplate restTemplate() {
            return new org.springframework.web.client.RestTemplate();
        }
    }

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    // Stub runner assigns this at runtime. It used to be hardcoded to 8090, which collided with the
    // three other modules that also pinned 8090 and made a parallel build race.
    @org.springframework.beans.factory.annotation.Value("${stubrunner.runningstubs.food-delivery-backend.port}")
    private int customerServiceStubPort;

    @Test
    public void testClientInvocations() {
        org.springframework.http.ResponseEntity<String[]> response = restTemplate.getForEntity(
            "http://localhost:" + customerServiceStubPort
                + "/api/v1/internal/orders/123e4567-e89b-12d3-a456-426614174000/participants",
            String[].class
        );
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCodeValue());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
    }
}

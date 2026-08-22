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
    "customer-service.url=http://localhost:8090",
    "restaurant-service.url=http://localhost:8091"
})
@AutoConfigureStubRunner(ids = {
    "com.fooddelivery:food-delivery-backend:+:stubs:8090",
    "com.fooddelivery:restaurant-application:+:stubs:8091"
}, stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class ContractConsumerTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
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

    @Test
    public void testClientInvocations() {
        org.springframework.http.ResponseEntity<String[]> response = restTemplate.getForEntity(
            "http://localhost:8090/api/v1/internal/orders/123e4567-e89b-12d3-a456-426614174000/participants", 
            String[].class
        );
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCodeValue());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
    }
}

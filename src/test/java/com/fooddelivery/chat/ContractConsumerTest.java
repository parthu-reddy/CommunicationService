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
    "stubrunner.idsToServiceIds.food-delivery-backend=customer-service",
    "stubrunner.idsToServiceIds.restaurant-application=restaurant-service"
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
    @org.springframework.cloud.openfeign.EnableFeignClients(basePackages = "com.fooddelivery.common.client")
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public org.springframework.web.client.RestTemplate restTemplate() {
            return new org.springframework.web.client.RestTemplate();
        }
    }

    @Autowired
    private com.fooddelivery.common.client.CustomerServiceClient customerServiceClient;

    @Autowired
    private com.fooddelivery.common.client.RestaurantServiceClient restaurantServiceClient;

    @Test
    public void testCustomerClientInvocations() {
        org.springframework.http.ResponseEntity<java.util.List<String>> response = customerServiceClient.getOrderParticipants(
            "123e4567-e89b-12d3-a456-426614174000",
            "communication-service"
        );
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCodeValue());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
    }

    @Test
    public void testRestaurantClientInvocations() {
        org.springframework.http.ResponseEntity<java.util.Map<String, Object>> response = restaurantServiceClient.getOutletOwner(
            "123e4567-e89b-12d3-a456-426614174000",
            "communication-service"
        );
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCodeValue());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
        org.junit.jupiter.api.Assertions.assertEquals("321e4567-e89b-12d3-a456-426614174000", response.getBody().get("ownerId"));
    }
}

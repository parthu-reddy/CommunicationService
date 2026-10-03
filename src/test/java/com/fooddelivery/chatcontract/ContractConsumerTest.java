// Deliberately OUTSIDE com.fooddelivery.chat. ChatServiceApplication component-scans that package,
// which meant this class's nested @SpringBootConfiguration -- and its
// @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration, HibernateJpaAutoConfiguration}) --
// was scanned into the real application context during tests, leaving it with no
// entityManagerFactory. A test's configuration must not be reachable by the application's own scan.
package com.fooddelivery.chatcontract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.context.annotation.Configuration;

// The only stub-runner test on the platform that did not activate this profile. Without it,
// application-contract-test.yml never loads, stubrunner.stubs-mode is unset, and the annotation
// falls back to its CLASSPATH default -- "No stubs were found on classpath". The other fourteen
// consumers already carried it, which is why this was the single failure.
@org.springframework.test.context.ActiveProfiles("contract-test")
@SpringBootTest(classes = ContractConsumerTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "stubrunner.idsToServiceIds.food-delivery-backend=customer-service",
    "stubrunner.idsToServiceIds.restaurant-application=restaurant-service"
})
@AutoConfigureStubRunner(ids = {
    "com.fooddelivery:food-delivery-backend:+:stubs",
    "com.fooddelivery:restaurant-application:+:stubs"
})
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
        // Production scans this real fallback from common. The isolated consumer context must
        // supply it too when verifying the same enabled circuit breaker path.
        @org.springframework.context.annotation.Bean
        public com.fooddelivery.common.client.RestaurantServiceClientFallback restaurantFallback() {
            return new com.fooddelivery.common.client.RestaurantServiceClientFallback();
        }
    }

    @Autowired
    private com.fooddelivery.common.client.CustomerServiceClient customerServiceClient;

    @Autowired
    private com.fooddelivery.common.client.RestaurantServiceClient restaurantServiceClient;

    @Autowired
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry breakers;

    private void successfulBreaker(String name) {
        var actual = breakers.getAllCircuitBreakers().stream()
                .filter(breaker -> name.equals(breaker.getName())).findFirst().orElseThrow(
                        () -> new AssertionError("No actual Feign circuit breaker was created: " + name));
        org.junit.jupiter.api.Assertions.assertTrue(actual.getMetrics().getNumberOfSuccessfulCalls() > 0,
                "The successful consumer request must run through the real circuit breaker");
    }

    @Test
    public void testCustomerClientInvocations() {
        org.springframework.http.ResponseEntity<java.util.List<String>> response = customerServiceClient.getOrderParticipants(
            "123e4567-e89b-12d3-a456-426614174000",
            "communication-service"
        );
        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCodeValue());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getBody());
        successfulBreaker("CustomerServiceClientgetOrderParticipantsStringString");
    }

    @Test
    public void testRestaurantClientInvocations() {
        var response = restaurantServiceClient.getOutletOrganisation(
                java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.UUID.fromString("321e4567-e89b-12d3-a456-426614174000"), response.organisationId());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), response.outletId());
        successfulBreaker("RestaurantServiceClientgetOutletOrganisationUUID");
    }

    @Test public void testNamedUserOutletPermissionContract() {
        var outlets = restaurantServiceClient.getUserOutlets(
                java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                com.fooddelivery.common.enums.OrganisationPermission.ORG_VIEW);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(
                java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000")), outlets);
        successfulBreaker("RestaurantServiceClientgetUserOutletsUUIDOrganisationPermission");
    }
}

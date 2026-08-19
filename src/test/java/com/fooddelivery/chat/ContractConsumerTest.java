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
    }

    @Test
    public void contextLoads() {
        // This test verifies that the stubs are successfully downloaded and registered.
        // It serves as the baseline consumer contract test for CommunicationService.
    }
}

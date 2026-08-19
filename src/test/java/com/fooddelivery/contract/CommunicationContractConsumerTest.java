package com.fooddelivery.contract;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("contract-test")
@SpringBootTest(classes = CommunicationContractConsumerTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CommunicationContractConsumerTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    static class TestConfig {
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void contextLoads() {
        assertNotNull(restTemplate);
    }
}

package com.fooddelivery.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.context.annotation.Import;

import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;

import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(
    scanBasePackages = {"com.fooddelivery.chat", "com.fooddelivery.common"}
)
@ComponentScan(
    basePackages = {"com.fooddelivery.chat", "com.fooddelivery.common"},
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {com.fooddelivery.common.security.CommonSecurityConfig.class})
)

@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {"com.fooddelivery.chat", "com.fooddelivery.common"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = {"com.fooddelivery.chat", "com.fooddelivery.common"})

@EnableScheduling
@EnableFeignClients(basePackages = {"com.fooddelivery.chat.client", "com.fooddelivery.common.client"})
public class ChatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

    @Bean
    public com.fooddelivery.common.outbox.service.OutboxProcessor outboxProcessor(
            com.fooddelivery.common.outbox.repository.OutboxEventRepository repository,
            org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        System.out.println("MANUALLY CREATING OUTBOX PROCESSOR IN CHAT SERVICE!");
        return new com.fooddelivery.common.outbox.service.OutboxProcessor(repository, kafkaTemplate, meterRegistry);
    }
}


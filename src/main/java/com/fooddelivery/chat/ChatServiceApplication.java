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

@SpringBootApplication(scanBasePackages = {"com.fooddelivery.chat"})
@EnableJpaRepositories(basePackages = {"com.fooddelivery.chat"})
@EntityScan(basePackages = {"com.fooddelivery.chat"})
@EnableScheduling
@Import({com.fooddelivery.common.config.CloudflareR2Config.class, com.fooddelivery.common.service.CloudflareR2Service.class})
public class ChatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

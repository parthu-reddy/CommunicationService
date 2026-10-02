package com.fooddelivery.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(
    scanBasePackages = {"com.fooddelivery.chat", "com.fooddelivery.common"}
)
@ComponentScan(
    basePackages = {"com.fooddelivery.chat", "com.fooddelivery.common"},
    // Keep Boot's exclusions when replacing its default component scan. Otherwise conditional
    // auto-configurations are discovered before their dependencies and skipped permanently.
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = org.springframework.boot.context.TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.fooddelivery.common.security.CommonSecurityConfig.class)
    }
)

@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = {"com.fooddelivery.chat", "com.fooddelivery.common"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = {"com.fooddelivery.chat", "com.fooddelivery.common"})

@EnableFeignClients(basePackages = {"com.fooddelivery.chat.client", "com.fooddelivery.common.client"})
public class ChatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

}

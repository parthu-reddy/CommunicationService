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

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(org.springframework.boot.web.client.RestTemplateBuilder builder) {
        return builder.interceptors((request, body, execution) -> {
            org.springframework.web.context.request.ServletRequestAttributes attributes = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                jakarta.servlet.http.HttpServletRequest servletRequest = attributes.getRequest();
                String[] headersToForward = {
                    com.fooddelivery.common.constants.HeaderConstants.HEADER_USER_ID, com.fooddelivery.common.constants.HeaderConstants.HEADER_USER_ROLES, com.fooddelivery.common.constants.HeaderConstants.HEADER_USER_PHONE,
                    "X-Identity-Signature", "X-Issued-At", "X-Session-Id",
                    "X-Calling-Service", "X-Device-Id", "Authorization"
                };
                for (String headerName : headersToForward) {
                    String headerValue = servletRequest.getHeader(headerName);
                    if (headerValue != null) {
                        request.getHeaders().add(headerName, headerValue);
                    }
                }
            }
            return execution.execute(request, body);
        }).build();
    }
}

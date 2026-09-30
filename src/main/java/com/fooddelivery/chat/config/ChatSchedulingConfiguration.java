package com.fooddelivery.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Production chat workers run scheduled outbox processing. Contract-test contexts intentionally
 * boot without Kafka and a live database, so they must not run those workers in the background.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!contract-test")
@EnableScheduling
public class ChatSchedulingConfiguration {
}

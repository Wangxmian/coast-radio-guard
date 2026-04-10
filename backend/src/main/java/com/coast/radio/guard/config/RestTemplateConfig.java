package com.coast.radio.guard.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder, AiProperties aiProperties) {
        Duration timeout = Duration.ofMillis(aiProperties.getTimeoutMillis());
        return builder
            .setConnectTimeout(timeout)
            .setReadTimeout(timeout)
            .build();
    }
}

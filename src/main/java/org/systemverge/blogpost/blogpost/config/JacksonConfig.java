package org.systemverge.blogpost.blogpost.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring Boot 4 no longer auto-configures Jackson2ObjectMapperBuilder.
 * Embabel's AgentPlatformConfiguration requires this bean, so we register it here.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder();
    }
}

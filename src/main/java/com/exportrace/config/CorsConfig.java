package com.exportrace.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource);
    }
}

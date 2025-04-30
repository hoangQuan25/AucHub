package com.example.liveauctions.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature; // <--- Import this
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register module for Java 8 Date/Time types (needed for Redis)
        mapper.registerModule(new JavaTimeModule());

        // Configure date format (needed for Redis/consistency)
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // *** ADD THIS CONFIGURATION ***
        // Ignore properties in the JSON input that are not defined in the Java DTO
        // This makes Feign clients (and other deserialization) more tolerant
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Add any other global ObjectMapper configurations you need here

        return mapper;
    }
}
package com.example.liveauctions.config;

import com.example.liveauctions.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JavaType; // Import JavaType
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.type.TypeFactory; // No longer needed with mapper.getTypeFactory()
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ChatMessageDto> chatRedisTemplate(
            RedisConnectionFactory cf, ObjectMapper mapper) { // Inject the PRIMARY mapper configured in JacksonConfig

        RedisTemplate<String, ChatMessageDto> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);

        // --- Explicitly create the serializer WITH the injected ObjectMapper ---
        // Get the JavaType representation of ChatMessageDto using the injected mapper's TypeFactory
        JavaType chatMessageDtoType = mapper.getTypeFactory().constructType(ChatMessageDto.class);

        // Create the serializer using the constructor that takes the configured ObjectMapper and JavaType
        Jackson2JsonRedisSerializer<ChatMessageDto> ser =
                new Jackson2JsonRedisSerializer<>(mapper, chatMessageDtoType);
        // --- END Explicit Configuration ---

        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(ser); // Use the explicitly configured serializer
        tpl.setHashKeySerializer(new StringRedisSerializer());
        tpl.setHashValueSerializer(ser); // Use the explicitly configured serializer for hash values too
        tpl.afterPropertiesSet();
        return tpl;
    }


    @Bean("viewerCountRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        // For keys
        tpl.setKeySerializer(new StringRedisSerializer());
        // For simple string values and set members
        tpl.setValueSerializer(new StringRedisSerializer());
        // For hash keys
        tpl.setHashKeySerializer(new StringRedisSerializer());
        // For hash values
        tpl.setHashValueSerializer(new StringRedisSerializer());
        tpl.afterPropertiesSet();
        return tpl;
    }
}
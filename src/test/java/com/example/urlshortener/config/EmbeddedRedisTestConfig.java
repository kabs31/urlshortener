package com.example.urlshortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.embedded.RedisServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

/**
 * Test configuration for embedded Redis server.
 * This allows running tests without requiring an external Redis instance.
 *
 * NOTE: This file should be placed in src/test/java, not src/main/java
 */
@TestConfiguration
@Profile("test")
public class EmbeddedRedisTestConfig {

    @Value("${spring.redis.port:6370}") // Different port for tests
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        try {
            redisServer = RedisServer.builder()
                    .port(redisPort)
                    .setting("maxmemory 128M")
                    .build();
            redisServer.start();
        } catch (Exception e) {
            // If port is already in use, try a different one
            redisPort = findAvailablePort();
            redisServer = RedisServer.builder()
                    .port(redisPort)
                    .setting("maxmemory 128M")
                    .build();
            redisServer.start();
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }

    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(redisPort);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }

    private int findAvailablePort() {
        // Simple port finding logic
        for (int port = 6370; port < 6380; port++) {
            try {
                java.net.ServerSocket socket = new java.net.ServerSocket(port);
                socket.close();
                return port;
            } catch (IOException e) {
                // Port is in use, try next one
            }
        }
        throw new RuntimeException("No available ports found for embedded Redis");
    }
}
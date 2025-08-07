package com.example.urlshortener.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for the URL Shortener application.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.example.urlshortener.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    // Spring Boot auto-configuration will handle DataSource
}
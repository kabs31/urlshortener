package com.example.urlshortener.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Database configuration for the URL Shortener application.
 */
@Slf4j
@Configuration
@EnableJpaRepositories(basePackages = "com.example.urlshortener.repository")
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * Production DataSource configuration with HikariCP.
     */
    @Bean
    @Profile("!test")
    public DataSource dataSource() {
        log.info("Configuring production DataSource with HikariCP");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // HikariCP specific settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Pool name for monitoring
        config.setPoolName("URLShortenerPool");

        log.info("DataSource configured successfully for URL: {}", jdbcUrl);
        return new HikariDataSource(config);
    }

    /**
     * Test DataSource configuration - uses Spring Boot auto-configuration.
     */
    @Bean
    @Profile("test")
    public DataSource testDataSource() {
        log.info("Using Spring Boot auto-configured DataSource for tests");
        // Spring Boot will autoconfigure H2 based on application-test.yml
        return null; // Let Spring Boot handle test datasource
    }
}
package com.example.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the URL Shortener API.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("A REST API for shortening URLs and redirecting to original URLs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("URL Shortener Team")
                                .email("support@urlshortener.com")
                                .url("https://github.com/example/url-shortener"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(baseUrl)
                                .description("URL Shortener API Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.urlshortener.com")
                                .description("Production Server")
                ));
    }
}
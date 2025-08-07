$baseDir = "src/main/java/com/example/urlshortener"
$resDir = "src/main/resources"
$testDir = "src/test/java/com/example/urlshortener"

# Create folders
$folders = @(
    "$baseDir/controller",
    "$baseDir/entity",
    "$baseDir/repository",
    "$baseDir/service/impl",
    "$baseDir/dto",
    "$baseDir/config",
    "$baseDir/exception",
    "$resDir/static",
    "$resDir/templates",
    "$resDir/db/migration",
    "$testDir/controller",
    "$testDir/service",
    "$testDir/repository"
)

foreach ($folder in $folders) {
    if (-not (Test-Path $folder)) {
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
    }
}

# Create files
$files = @(
    "$baseDir/UrlShortenerApplication.java",
    "$baseDir/controller/UrlController.java",
    "$baseDir/entity/Url.java",
    "$baseDir/repository/UrlRepository.java",
    "$baseDir/service/UrlService.java",
    "$baseDir/service/impl/UrlServiceImpl.java",
    "$baseDir/dto/UrlRequest.java",
    "$baseDir/dto/UrlResponse.java",
    "$baseDir/config/DatabaseConfig.java",
    "$baseDir/exception/GlobalExceptionHandler.java",
    "$baseDir/exception/UrlNotFoundException.java",
    "$resDir/application.yml",
    "$resDir/application-dev.yml",
    "$resDir/application-prod.yml",
    "$resDir/db/migration/V1__create_url_table.sql",
    "$testDir/UrlShortenerApplicationTests.java",
    "$testDir/controller/UrlControllerTest.java",
    "$testDir/service/UrlServiceTest.java",
    "$testDir/repository/UrlRepositoryTest.java",
    "README.md",
    ".gitignore",
    "pom.xml"
)

foreach ($file in $files) {
    if (-not (Test-Path $file)) {
        New-Item -ItemType File -Path $file -Force | Out-Null
    }
}

# Basic content
Set-Content "$baseDir/UrlShortenerApplication.java" @"
package com.example.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UrlShortenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
"@

Set-Content "$resDir/db/migration/V1__create_url_table.sql" "-- SQL to create URL table"

if ((Get-Content .gitignore).Length -eq 0) {
    Set-Content .gitignore "target/`n*.log`n*.class`n*.iml`n.env`n.idea/"
}

if ((Get-Content README.md).Length -eq 0) {
    Set-Content README.md "# URL Shortener`n`nSpring Boot based service to shorten and redirect URLs."
}

Write-Host "`n✅ Project structure created successfully!"

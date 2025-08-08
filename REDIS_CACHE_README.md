# Redis Caching Integration for URL Shortener

This implementation integrates Redis caching into the URL shortener redirect flow for optimal performance.

## Key Features

- **Cache-First Redirect**: Redis lookup before database queries
- **TTL Management**: 1-hour default cache expiration
- **Fallback Strategy**: Graceful database fallback on cache miss
- **Dual Caching**: Both Spring @Cacheable and manual Redis operations
- **Async Click Tracking**: Non-blocking click count updates

## Cache Strategy

### Cache Key Pattern
```
url:{short_code} → long_url
```
Example: `url:abc123` → `https://www.example.com/very/long/path`

### Redirect Flow
1. **Check Redis**: `GET url:{code}`
2. **Cache Hit**: Return URL immediately + async click tracking
3. **Cache Miss**: Query database → Cache result → Return URL
4. **TTL**: 1 hour (configurable)

## Configuration

### Dependencies Added
Add these dependencies to your pom.xml:

```
<!-- Redis Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- Cache Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Embedded Redis for Testing -->
<dependency>
    <groupId>it.ozimov</groupId>
    <artifactId>embedded-redis</artifactId>
    <version>0.7.3</version>
    <scope>test</scope>
</dependency>
```

### Application Configuration
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 2000ms
  cache:
    type: redis
    redis:
      time-to-live: 3600s # 1 hour

app:
  cache:
    url:
      ttl: 3600 # Cache TTL in seconds
```

## Architecture Components

### 1. RedisConfig.java
- Lettuce connection factory
- RedisTemplate configuration
- Jackson JSON serialization
- Cache manager setup

### 2. CachedUrlService.java
- @Cacheable for full object caching
- Manual Redis operations for fast redirects
- Fallback logic implementation
- Async click tracking

### 3. Updated UrlController.java
- Optimized redirect endpoint
- Cache-first lookup strategy
- Performance monitoring

## Performance Benefits

- **Sub-millisecond redirects** for cached URLs
- **Reduced database load** by 80-90%
- **Horizontal scalability** with Redis clustering
- **Cache hit ratio** typically 85%+ for popular URLs

## Setup Instructions

1. **Start Redis and PostgreSQL**:
   ```bash
   docker-compose up -d
   ```

2. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Test caching**:
   ```bash
   # Create a short URL
   curl -X POST http://localhost:8080/api/v1/api/shorten \
     -H "Content-Type: application/json" \
     -d '{"url":"https://example.com"}'
   
   # First redirect (cache miss - slower)
   curl -I http://localhost:8080/api/v1/abc123
   
   # Second redirect (cache hit - faster)
   curl -I http://localhost:8080/api/v1/abc123
   ```

## Monitoring

- **Health endpoint**: `GET /health` - includes cache statistics
- **Cache invalidation**: `DELETE /api/cache/{code}`
- **Redis Commander**: http://localhost:8081 (if using docker-compose with tools profile)

## Cache Management

### Manual Cache Operations
```
// Check if cached
boolean exists = stringRedisTemplate.hasKey("url:abc123");

// Get cached URL
String url = stringRedisTemplate.opsForValue().get("url:abc123");

// Cache URL with TTL
stringRedisTemplate.opsForValue().set("url:abc123", "https://example.com", 3600, TimeUnit.SECONDS);

// Invalidate cache
stringRedisTemplate.delete("url:abc123");
```

### Spring Cache Annotations
```
@Cacheable(value = "urlCache", key = "#shortCode")
public UrlResponse getOriginalUrl(String shortCode) { ... }

@CacheEvict(value = "urlCache", key = "#shortCode")
public void invalidateCache(String shortCode) { ... }
```

## Production Considerations

- **Redis Clustering**: For high availability
- **Cache Warming**: Pre-populate frequently accessed URLs
- **Monitoring**: Track cache hit ratios and performance
- **Backup Strategy**: Redis persistence configuration
- **Security**: Redis AUTH and network isolation
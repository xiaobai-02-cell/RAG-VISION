package org.example.cvrag.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RedisConnectivityTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldConnectAndRoundTrip() {
        String pong = redisTemplate.execute(RedisConnection::ping);
        Assertions.assertNotNull(pong, "Redis ping response should not be null");
        Assertions.assertEquals("PONG", pong.toUpperCase(), "Redis ping should return PONG");

        String key = "cv-rag:test:" + UUID.randomUUID();
        String value = "ok-" + System.currentTimeMillis();

        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(30));
        String actual = redisTemplate.opsForValue().get(key);
        Assertions.assertEquals(value, actual, "Redis value round-trip mismatch");

        redisTemplate.delete(key);
    }
}

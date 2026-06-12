package com.yourname.shorten;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;

@SpringBootTest
@ActiveProfiles("dev")
public abstract class AbstractIntegrationTest {

    private static RedisServer redisServer;
    private static int redisPort;

    static {
        try {
            redisPort = findFreePort();
            redisServer = new RedisServer(redisPort);
            redisServer.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start embedded Redis", e);
        }
    }

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> redisPort);
    }

    @PreDestroy
    public void cleanup() {
        if (redisServer != null && redisServer.isActive()) {
            try {
                redisServer.stop();
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        }
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("No free port", e);
        }
    }
}

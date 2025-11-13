package cn.wenyuan.zrpc.transport.netty.config;

import lombok.extern.slf4j.Slf4j;

/**
 * Central place to configure heartbeat idle timeouts so they can be tweaked
 * with JVM system properties or environment variables at runtime.
 */
@Slf4j
public final class HeartbeatConfig {

    private static final String SERVER_READ_IDLE_PROPERTY = "zrpc.server.heartbeat.readIdleSeconds";
    private static final String SERVER_READ_IDLE_ENV = "ZRPC_SERVER_HEARTBEAT_READ_IDLE_SECONDS";
    private static final int DEFAULT_SERVER_READ_IDLE_SECONDS = 30;

    private static final String CLIENT_WRITE_IDLE_PROPERTY = "zrpc.client.heartbeat.writeIdleSeconds";
    private static final String CLIENT_WRITE_IDLE_ENV = "ZRPC_CLIENT_HEARTBEAT_WRITE_IDLE_SECONDS";
    private static final int DEFAULT_CLIENT_WRITE_IDLE_SECONDS = 10;

    private HeartbeatConfig() {
    }

    public static int serverReadIdleSeconds() {
        return resolveSeconds(
            SERVER_READ_IDLE_PROPERTY,
            SERVER_READ_IDLE_ENV,
            DEFAULT_SERVER_READ_IDLE_SECONDS
        );
    }

    public static int clientWriteIdleSeconds() {
        return resolveSeconds(
            CLIENT_WRITE_IDLE_PROPERTY,
            CLIENT_WRITE_IDLE_ENV,
            DEFAULT_CLIENT_WRITE_IDLE_SECONDS
        );
    }

    private static int resolveSeconds(
        String propertyKey,
        String envKey,
        int defaultSeconds
    ) {
        String candidate = System.getProperty(propertyKey);
        if (candidate == null || candidate.isBlank()) {
            candidate = System.getenv(envKey);
        }
        if (candidate == null || candidate.isBlank()) {
            return defaultSeconds;
        }
        try {
            int resolved = Integer.parseInt(candidate.trim());
            if (resolved <= 0) {
                log.warn("Heartbeat config [{}] resolved to <= 0 ({}). Falling back to default {}s.",
                         propertyKey, candidate, defaultSeconds);
                return defaultSeconds;
            }
            return resolved;
        } catch (NumberFormatException ex) {
            log.warn("Failed to parse heartbeat config [{}] / env [{}] value '{}'. Using default {}s.",
                     propertyKey, envKey, candidate, defaultSeconds, ex);
            return defaultSeconds;
        }
    }
}

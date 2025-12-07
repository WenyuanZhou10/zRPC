package cn.wenyuan.zrpc.core.governance;


import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;

/**
 * @ClassName CircuitBreakerProvider
 * @Description TODO
 * @Author CircuitBreakerProvider
 * @Date 2025/12/2 23:45
 * @Version 1.0
 */

public class CircuitBreakerProvider {
    private final CircuitBreakerRegistry registry;

    // 单例 Holder
    private static class Holder {
        static final CircuitBreakerProvider INSTANCE = new CircuitBreakerProvider();
    }

    public static CircuitBreakerProvider getInstance() {
        return Holder.INSTANCE;
    }

    private CircuitBreakerProvider() {
        ZrpcConfig config = ApplicationConfig.getConfig();
        ZrpcConfig.CircuitBreakerConfig breakerConfig =
            config != null ? config.getCircuitBreaker() : null;

        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom();

        float failureThreshold = breakerConfig != null && breakerConfig.getFailureRateThreshold() != null
            ? breakerConfig.getFailureRateThreshold()
            : 50f;
        float slowCallRate = breakerConfig != null && breakerConfig.getSlowCallRateThreshold() != null
            ? breakerConfig.getSlowCallRateThreshold()
            : 100f;
        long slowCallDurationMs = breakerConfig != null && breakerConfig.getSlowCallDurationMs() != null
            ? breakerConfig.getSlowCallDurationMs()
            : 2000L;
        long waitDurationMs = breakerConfig != null && breakerConfig.getWaitDurationMs() != null
            ? breakerConfig.getWaitDurationMs()
            : 5000L;
        int slidingWindowSize = breakerConfig != null && breakerConfig.getSlidingWindowSize() != null
            ? breakerConfig.getSlidingWindowSize()
            : 10;
        int minimumCalls = breakerConfig != null && breakerConfig.getMinimumNumberOfCalls() != null
            ? breakerConfig.getMinimumNumberOfCalls()
            : 5;
        int permittedHalfOpen = breakerConfig != null
            && breakerConfig.getPermittedNumberOfCallsInHalfOpenState() != null
            ? breakerConfig.getPermittedNumberOfCallsInHalfOpenState()
            : 2;

        CircuitBreakerConfig defaultConfig = builder
            .failureRateThreshold(failureThreshold)
            .slowCallRateThreshold(slowCallRate)
            .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationMs))
            .waitDurationInOpenState(Duration.ofMillis(waitDurationMs))
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(minimumCalls)
            .permittedNumberOfCallsInHalfOpenState(permittedHalfOpen)
            .build();

        this.registry = CircuitBreakerRegistry.of(defaultConfig);
    }

    public CircuitBreaker getCircuitBreaker(String serviceName) {
        return registry.circuitBreaker(serviceName);
    }

    public void removeCircuitBreaker(String serviceName) {
        registry.remove(serviceName);
    }
}

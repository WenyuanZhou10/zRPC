package cn.wenyuan.zrpc.core.governance;


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
        // 1. 定义默认熔断规则 (生产环境建议从 ConfigService 读取)
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                                                                 // 失败率阈值：50% (超过50%失败就熔断)
                                                                 .failureRateThreshold(50)
                                                                 // 慢调用阈值：100% (如果响应超过2s算慢调用，慢调用太多也熔断)
                                                                 .slowCallRateThreshold(100)
                                                                 .slowCallDurationThreshold(Duration.ofSeconds(2))
                                                                 // 熔断后等待时长：5秒 (5秒后进入半开状态，尝试放行)
                                                                 .waitDurationInOpenState(Duration.ofSeconds(5))
                                                                 // 滑动窗口大小：10 (统计最近10次请求)
                                                                 .slidingWindowSize(10)
                                                                 // 最小请求数：5 (至少请求5次才开始计算失败率)
                                                                 .minimumNumberOfCalls(5)
                                                                 .permittedNumberOfCallsInHalfOpenState(2) // 半开状态允许2个请求去试探
                                                                 .build();

        // 2. 创建注册中心
        this.registry = CircuitBreakerRegistry.of(defaultConfig);
    }

    public CircuitBreaker getCircuitBreaker(String serviceName) {
        // 根据服务名获取或创建熔断器
        return registry.circuitBreaker(serviceName);
    }
}

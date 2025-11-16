package cn.wenyuan.zrpc.core.ratelimit;


import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.common.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName RateLimiterFactory
 * @Description TODO
 * @Author RateLimiterFactory
 * @Date 2025/11/16 21:20
 * @Version 1.0
 */
@Slf4j
public class RateLimiterFactory {

    private RateLimiterFactory(){}

    private static class LimiterHolder {

        static final Map<String, RateLimiter> LIMITER_MAP = loadLimitersViaSPI();

        private static Map<String, RateLimiter> loadLimitersViaSPI() {
            log.info("开始加载LIMITER_MAP");

            Map<String, RateLimiter> map = new HashMap<>();

            ServiceLoader<RateLimiter> loader = ServiceLoader.load(RateLimiter.class);

            for(RateLimiter rateLimiter : loader){
                String algorithmName = rateLimiter.getAlgorithmName();
                if (!StringUtils.isBlank(algorithmName)) {
                    if (map.containsKey(algorithmName)) {
                        log.warn("重名限流算法");
                    }
                    map.put(algorithmName, rateLimiter);
                } else {
                    log.warn("不合法的算法命名");
                }
            }

            return Collections.unmodifiableMap(map);
        }

    }

    RateLimiter getRateLimiter(String algorithm){
        return LimiterHolder.LIMITER_MAP.get(algorithm);
    }
}

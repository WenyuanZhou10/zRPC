package cn.wenyuan.zrpc.core.ratelimit.impl;


import cn.wenyuan.zrpc.core.config.ConfigService;
import cn.wenyuan.zrpc.core.ratelimit.RateLimiter;
import org.apache.zookeeper.common.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.wenyuan.zrpc.core.ratelimit.RateLimitConstants.*;

/**
 * @ClassName TokenBucketRateLimiter
 * @Description TODO
 * @Author TokenBucketRateLimiter
 * @Date 2025/11/16 21:21
 * @Version 1.0
 */

public class TokenBucketRateLimiter implements RateLimiter {

    private static final int DEFAULT_QPS = 10;
    private static final int DEFAULT_CAPACITY = 10;

    private final long capacity;
    private final long qps;
    private long lastRefillTimestamp; // 上次补充令牌的时间戳

    private final Map<String, BucketState> buckets = new ConcurrentHashMap<>();

    private static class BucketState {
        long currentTokens;
        long lastRefillTimestamp; // 上次补充令牌的时间戳

        public BucketState(long capacity, long lastRefillTimestamp) {
            this.currentTokens = capacity; // 刚创建时，桶是满的
            this.lastRefillTimestamp = lastRefillTimestamp;
        }
    }

    public TokenBucketRateLimiter() {
        this.capacity = ConfigService.getInt(TOKEN_BUCKET_CAPACITY_KEY, DEFAULT_CAPACITY);
        this.qps = ConfigService.getInt(TOKEN_BUCKET_QPS_KEY, DEFAULT_QPS);
    }

    @Override
    public boolean tryAcquire(String key) {
        if (StringUtils.isEmpty(key)) {
            return true;
        }

        BucketState state = buckets.computeIfAbsent(key, k -> new BucketState(this.capacity, System.currentTimeMillis()));

        synchronized (state) {
            refill(state);

            if (state.currentTokens > 0) {
                state.currentTokens--;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME_TOKEN_BUCKET;
    }

    private void refill(BucketState state) {
        long now = System.currentTimeMillis();

        if (qps <= 0) {
            return;
        }
        double secondsPassed = (now - lastRefillTimestamp) / 1000.0;

        long tokensToAdd = (long) (secondsPassed * qps);

        if (tokensToAdd > 0) {
            state.currentTokens = Math.min(this.capacity, state.currentTokens + tokensToAdd);
            state.lastRefillTimestamp = now;
        }
    }
}

package cn.wenyuan.zrpc.core.ratelimit.impl;


import cn.wenyuan.zrpc.core.ratelimit.RateLimiter;

/**
 * @ClassName TokenBucketRateLimiter
 * @Description TODO
 * @Author TokenBucketRateLimiter
 * @Date 2025/11/16 21:21
 * @Version 1.0
 */

public class TokenBucketRateLimiter implements RateLimiter {

    private final long capacity;
    private final long tokensPerSecond;
    private long currentTokens;
    private long lastRefillTimestamp; // 上次补充令牌的时间戳

    public TokenBucketRateLimiter(
        long capacity,
        long tokensPerSecond
    ) {
        this.capacity = capacity;
        this.tokensPerSecond = tokensPerSecond;

        this.currentTokens = capacity;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    @Override
    public synchronized boolean tryAcquire() {
        refill();

        if (currentTokens > 0){
            currentTokens--;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getAlgorithmName() {
        return "TokenBucket";
    }

    private void refill() {
        long now = System.currentTimeMillis();

        double secondsPassed = (now - lastRefillTimestamp) / 1000.0;

        long tokensToAdd = (long) (secondsPassed * tokensPerSecond);

        if (tokensToAdd > 0) {
            currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
            lastRefillTimestamp = now;
        }
    }
}

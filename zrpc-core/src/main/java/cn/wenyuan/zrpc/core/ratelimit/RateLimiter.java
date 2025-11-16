package cn.wenyuan.zrpc.core.ratelimit;

public interface RateLimiter {

    boolean tryAcquire();

    String getAlgorithmName();
}

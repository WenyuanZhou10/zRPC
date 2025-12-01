package cn.wenyuan.zrpc.core.filter.impl;

import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.Filter;
import cn.wenyuan.zrpc.core.filter.FilterChain;
import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import cn.wenyuan.zrpc.core.ratelimit.RateLimiter;
import cn.wenyuan.zrpc.core.ratelimit.RateLimiterFactory;

/**
 * @ClassName RateLimiterFilter
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/11/17 11:35
 * @Version 1.0
 */

public class RateLimiterFilter implements Filter {

    private static final String DEFAULT_ALGORITHM = "token_bucket";

    private RateLimiter rateLimiter;

    public RateLimiterFilter(){
        String algorithm = DEFAULT_ALGORITHM;
        ZrpcConfig config = ApplicationConfig.getConfig();
        if (config != null && config.getRateLimit() != null
            && config.getRateLimit().getDefaultAlgorithm() != null) {
            algorithm = config.getRateLimit().getDefaultAlgorithm();
        }
        this.rateLimiter = RateLimiterFactory.getRateLimiter(algorithm);
    }

    @Override
    public void filter(RpcRequest request, RpcResponse response, FilterChain chain) {
        if (rateLimiter == null) {
            chain.doFilter(request, response);
            return;
        }

        String rateLimitKey = request.getService() + "." + request.getMethodName();
        boolean acquired = rateLimiter.tryAcquire(rateLimitKey);

        if (acquired) {
            chain.doFilter(request, response);
        } else {
            response.setError(new RuntimeException(
                    "Request limit exceeded for: " + rateLimitKey
            ));
            response.setSuccess(false);
            response.setErrorMessage("Request limit exceeded for: " + rateLimitKey);
        }
    }
}

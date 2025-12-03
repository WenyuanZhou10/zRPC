package cn.wenyuan.zrpc.core.filter.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.client.ClientFilter;
import cn.wenyuan.zrpc.core.filter.client.ClientFilterChain;
import cn.wenyuan.zrpc.core.governance.CircuitBreakerProvider;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName ClientCircuitBreakerFilter
 * @Description TODO
 * @Author ClientCircuitBreakerFilter
 * @Date 2025/12/2 23:48
 * @Version 1.0
 */
@Slf4j
public class ClientCircuitBreakerFilter implements ClientFilter {

    @Override
    public CompletableFuture<RpcResponse> filter(RpcRequest request, ClientFilterChain chain) {
        String serviceName = request.getService();
        CircuitBreaker circuitBreaker = CircuitBreakerProvider.getInstance().getCircuitBreaker(serviceName);
        try {
            circuitBreaker.acquirePermission();
        } catch (CallNotPermittedException ex) {
            log.warn("【熔断保护】服务 [{}] 暂时不可用，请求被拦截。", serviceName);
            CompletableFuture<RpcResponse> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }

        long start = System.nanoTime();
        CompletableFuture<RpcResponse> delegate;
        try {
            delegate = chain.doFilter(request);
        } catch (Throwable throwable) {
            circuitBreaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, throwable);
            CompletableFuture<RpcResponse> failed = new CompletableFuture<>();
            failed.completeExceptionally(throwable);
            return failed;
        }

        delegate.whenComplete((resp, throwable) -> {
            long duration = System.nanoTime() - start;
            if (throwable != null) {
                circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, throwable);
                return;
            }
            if (resp == null || !resp.isSuccess()) {
                String message = resp != null ? resp.getErrorMessage() : "unknown client failure";
                circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, new RuntimeException(message));
            } else {
                circuitBreaker.onSuccess(duration, TimeUnit.NANOSECONDS);
            }
        });

        return delegate;
    }
}

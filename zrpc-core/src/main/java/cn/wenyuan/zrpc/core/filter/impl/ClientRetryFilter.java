package cn.wenyuan.zrpc.core.filter.impl;


import cn.wenyuan.zrpc.common.exception.RpcException;
import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import cn.wenyuan.zrpc.core.filter.client.ClientFilter;
import cn.wenyuan.zrpc.core.filter.client.ClientFilterChain;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端重试过滤器：支持按配置对网络/服务器忙等错误进行指数退避重试。
 */
@Slf4j
public class ClientRetryFilter implements ClientFilter {

    private static final ScheduledExecutorService RETRY_SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "zrpc-retry-scheduler");
            t.setDaemon(true);
            return t;
        });

    private final boolean enabled;
    private final int maxAttempts;
    private final long initialDelayMillis;
    private final double backoffMultiplier;
    private final ZrpcConfig.RetryCondition retryCondition;

    public ClientRetryFilter() {
        ZrpcConfig config = ApplicationConfig.getConfig();
        ZrpcConfig.RetryConfig retryConfig = config != null ? config.getRetry() : null;
        if (retryConfig == null) {
            this.enabled = false;
            this.maxAttempts = 1;
            this.initialDelayMillis = 0L;
            this.backoffMultiplier = 1.0;
            this.retryCondition = ZrpcConfig.RetryCondition.NETWORK_ONLY;
            return;
        }
        this.enabled = retryConfig.isEnabled();
        this.maxAttempts = Objects.requireNonNullElse(retryConfig.getMaxAttempts(), 1);
        this.initialDelayMillis = Objects.requireNonNullElse(retryConfig.getInitialDelayMillis(), 0L);
        this.backoffMultiplier = Objects.requireNonNullElse(retryConfig.getBackoffMultiplier(), 1.0);
        this.retryCondition = Objects.requireNonNullElse(retryConfig.getRetryCondition(),
                                                         ZrpcConfig.RetryCondition.NETWORK_ONLY);
    }

    @Override
    public CompletableFuture<RpcResponse> filter(RpcRequest request, ClientFilterChain chain) {
        if (!enabled || maxAttempts <= 1) {
            return chain.doFilter(request);
        }
        int startIndex = chain.currentIndex();
        CompletableFuture<RpcResponse> resultFuture = new CompletableFuture<>();
        executeAttempt(request, chain.forkFrom(startIndex), chain, startIndex, 1, resultFuture);
        return resultFuture;
    }

    private void executeAttempt(
        RpcRequest request,
        ClientFilterChain attemptChain,
        ClientFilterChain originalChain,
        int startIndex,
        int attempt,
        CompletableFuture<RpcResponse> target
    ) {
        attemptChain.doFilter(request).whenComplete((resp, throwable) -> {
            Throwable realThrowable = unwrap(throwable);
            if (shouldRetry(request, resp, realThrowable) && attempt < maxAttempts) {
                long delay = computeDelay(attempt);
                log.warn("RPC 调用 {}.{} 失败，将在 {} ms 后进行第 {} 次重试。",
                         request.getService(), request.getMethodName(), delay, attempt + 1);
                RETRY_SCHEDULER.schedule(
                    () -> executeAttempt(request,
                                         originalChain.forkFrom(startIndex),
                                         originalChain,
                                         startIndex,
                                         attempt + 1,
                                         target),
                    delay,
                    TimeUnit.MILLISECONDS
                );
            } else if (realThrowable != null) {
                target.completeExceptionally(realThrowable);
            } else {
                target.complete(resp);
            }
        });
    }

    private long computeDelay(int attempt) {
        if (initialDelayMillis <= 0) {
            return 0L;
        }
        double multiplier = Math.max(1.0, backoffMultiplier);
        return (long) (initialDelayMillis * Math.pow(multiplier, attempt - 1));
    }

    private boolean shouldRetry(RpcRequest request, RpcResponse resp, Throwable throwable) {
        if (throwable != null) {
            if (throwable instanceof CallNotPermittedException) {
                log.warn("熔断已开启，停止对 {}.{} 的重试。", request.getService(), request.getMethodName());
                return false;
            }
            return isNetworkException(throwable);
        }
        if (resp == null || resp.isSuccess()) {
            return false;
        }
        if (retryCondition == ZrpcConfig.RetryCondition.NETWORK_ONLY) {
            return false;
        }
        return resp.getError() instanceof RpcException;
    }

    private boolean isNetworkException(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        return cause instanceof SocketTimeoutException || cause instanceof SocketException;
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof java.util.concurrent.CompletionException ce && ce.getCause() != null) {
            return ce.getCause();
        }
        if (throwable instanceof java.util.concurrent.ExecutionException ee && ee.getCause() != null) {
            return ee.getCause();
        }
        return throwable;
    }
}

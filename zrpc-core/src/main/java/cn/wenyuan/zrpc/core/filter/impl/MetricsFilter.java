package cn.wenyuan.zrpc.core.filter.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.Filter;
import cn.wenyuan.zrpc.core.filter.FilterChain;
import cn.wenyuan.zrpc.core.filter.client.ClientFilter;
import cn.wenyuan.zrpc.core.filter.client.ClientFilterChain;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端/服务端通用的 Metrics 过滤器，用于输出 QPS、错误率、耗时等简单指标。
 */
@Slf4j
public class MetricsFilter implements Filter, ClientFilter {

    private static final ConcurrentHashMap<String, Metrics> METRICS = new ConcurrentHashMap<>();
    private static final int LOG_INTERVAL = 100;

    private Metrics getMetrics(String service, String method) {
        String key = service + "." + method;
        return METRICS.computeIfAbsent(key, k -> new Metrics(service, method));
    }

    @Override
    public void filter(RpcRequest request, RpcResponse response, FilterChain chain) {
        Metrics metrics = getMetrics(request.getService(), request.getMethodName());
        long start = System.nanoTime();
        metrics.incrementConcurrent();
        boolean success = false;
        try {
            chain.doFilter(request, response);
            success = response != null && response.isSuccess();
        } catch (Throwable throwable) {
            metrics.record(false, System.nanoTime() - start);
            throw throwable;
        }
        metrics.record(success, System.nanoTime() - start);
    }

    @Override
    public CompletableFuture<RpcResponse> filter(RpcRequest request, ClientFilterChain chain) {
        Metrics metrics = getMetrics(request.getService(), request.getMethodName());
        long start = System.nanoTime();
        metrics.incrementConcurrent();
        CompletableFuture<RpcResponse> future;
        try {
            future = chain.doFilter(request);
        } catch (Throwable throwable) {
            metrics.record(false, System.nanoTime() - start);
            CompletableFuture<RpcResponse> failed = new CompletableFuture<>();
            failed.completeExceptionally(throwable);
            return failed;
        }

        future.whenComplete((resp, throwable) -> {
            boolean success = throwable == null && resp != null && resp.isSuccess();
            metrics.record(success, System.nanoTime() - start);
        });
        return future;
    }

    private static final class Metrics {
        private final String service;
        private final String method;
        private final LongAdder total = new LongAdder();
        private final LongAdder error = new LongAdder();
        private final LongAdder totalDurationNanos = new LongAdder();
        private final AtomicInteger concurrent = new AtomicInteger(0);
        private volatile long lastLogTimeNanos = System.nanoTime();
        private volatile long lastLogCount = 0;

        private Metrics(String service, String method) {
            this.service = service;
            this.method = method;
        }

        private void incrementConcurrent() {
            concurrent.incrementAndGet();
        }

        private void record(boolean success, long durationNanos) {
            total.increment();
            if (!success) {
                error.increment();
            }
            totalDurationNanos.add(durationNanos);
            concurrent.decrementAndGet();
            long count = total.longValue();
            if (count % LOG_INTERVAL == 0) {
                synchronized (this) {
                    long now = System.nanoTime();
                    long deltaCount = count - lastLogCount;
                    long deltaNanos = now - lastLogTimeNanos;
                    double avgMs = (totalDurationNanos.doubleValue() / count) / 1_000_000.0;
                    double errRate = count == 0 ? 0 : (error.doubleValue() / count) * 100;
                    double throughput = deltaNanos > 0
                        ? (deltaCount * 1_000_000_000d) / deltaNanos
                        : 0d;
                    log.info("[Metrics] {}.{} total={}, errRate={}%, avgRT={}ms, concurrent={}, throughput={} qps",
                             service, method, count, String.format("%.2f", errRate),
                             String.format("%.3f", avgMs), concurrent.get(),
                             String.format("%.1f", throughput));
                    lastLogCount = count;
                    lastLogTimeNanos = now;
                }
            }
        }
    }
}

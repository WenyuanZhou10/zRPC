package cn.wenyuan.zrpc.core.filter.impl;


import brave.Span;
import brave.Tracer;
import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.client.ClientFilter;
import cn.wenyuan.zrpc.core.filter.client.ClientFilterChain;
import cn.wenyuan.zrpc.core.tracing.TracingProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端 Zipkin 追踪过滤器：为每次调用创建 CLIENT span，并注入 B3 头部。
 */
@Slf4j
public class ClientTracingFilter implements ClientFilter {

    private static final String DEFAULT_TRACE_ID_KEY = "traceId";

    @Override
    public CompletableFuture<RpcResponse> filter(RpcRequest request, ClientFilterChain chain) {
        if (!TracingProvider.isEnabled() || TracingProvider.tracer() == null) {
            ensureTraceId(request);
            return chain.doFilter(request);
        }

        Map<String, String> headers = ensureHeaders(request);
        Tracer tracer = TracingProvider.tracer();
        Span span = tracer.nextSpan().kind(Span.Kind.CLIENT)
            .name(request.getService() + "." + request.getMethodName());

        span.tag("rpc.service", request.getService());
        span.tag("rpc.method", request.getMethodName());
        span.start();

        try {
            TracingProvider.injector().inject(span.context(), headers);
            headers.put(DEFAULT_TRACE_ID_KEY, span.context().traceIdString());
        } catch (Exception ex) {
            log.warn("注入 Zipkin 上下文失败", ex);
        }

        CompletableFuture<RpcResponse> future;
        try {
            future = chain.doFilter(request);
        } catch (Throwable throwable) {
            span.error(throwable);
            span.finish();
            CompletableFuture<RpcResponse> failed = new CompletableFuture<>();
            failed.completeExceptionally(throwable);
            return failed;
        }

        future.whenComplete((resp, throwable) -> {
            if (throwable != null) {
                span.error(throwable);
            } else if (resp != null && !resp.isSuccess()) {
                span.tag("rpc.status", "error");
                if (resp.getErrorMessage() != null) {
                    span.tag("rpc.error", resp.getErrorMessage());
                }
                if (resp.getError() != null) {
                    span.error(resp.getError());
                }
            } else {
                span.tag("rpc.status", "ok");
            }
            span.finish();
        });

        return future;
    }

    private Map<String, String> ensureHeaders(RpcRequest request) {
        Map<String, String> headers = request.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
            request.setHeaders(headers);
        }
        return headers;
    }

    private void ensureTraceId(RpcRequest request) {
        Map<String, String> headers = ensureHeaders(request);
        headers.computeIfAbsent(DEFAULT_TRACE_ID_KEY, k -> java.util.UUID.randomUUID().toString());
    }
}

package cn.wenyuan.zrpc.core.filter.impl;


import brave.propagation.TraceContext;
import cn.wenyuan.zrpc.common.context.RpcContext;
import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.Filter;
import cn.wenyuan.zrpc.core.filter.FilterChain;
import cn.wenyuan.zrpc.core.tracing.TracingProvider;
import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContextOrSamplingFlags;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * 服务端追踪拦截器：确保每个请求都有 traceId，并同步到 RpcContext 与 MDC，方便后续日志透传。
 */
public class TraceFilter implements Filter {

    public static final String TRACE_ID_KEY = "traceId";

    @Override
    public void filter(RpcRequest request, RpcResponse response, FilterChain chain) {
        Map<String, String> headers = ensureHeaders(request);
        if (!TracingProvider.isEnabled() || TracingProvider.tracer() == null) {
            fallbackTrace(headers);
            chain.doFilter(request, response);
            return;
        }

        Tracer tracer = TracingProvider.tracer();
        TraceContext.Extractor<Map<String, String>> extractor = TracingProvider.extractor();
        TraceContextOrSamplingFlags extracted = extractor != null
            ? extractor.extract(headers)
            : TraceContextOrSamplingFlags.EMPTY;
        Span span = extracted.context() != null
            ? tracer.joinSpan(extracted.context())
            : tracer.nextSpan(extracted);
        span.name(request.getService() + "." + request.getMethodName());
        span.kind(Span.Kind.SERVER);
        span.tag("rpc.service", request.getService());
        span.tag("rpc.method", request.getMethodName());
        span.start();

        String traceId = span.context().traceIdString();
        headers.put(TRACE_ID_KEY, traceId);
        RpcContext.setAttachments(headers);
        MDC.put(TRACE_ID_KEY, traceId);

        try {
            chain.doFilter(request, response);
            if (response != null) {
                if (response.getError() != null || !response.isSuccess()) {
                    span.tag("rpc.status", "error");
                    if (response.getErrorMessage() != null) {
                        span.tag("rpc.error", response.getErrorMessage());
                    }
                    if (response.getError() != null) {
                        span.error(response.getError());
                    }
                } else {
                    span.tag("rpc.status", "ok");
                }
            }
        } catch (Throwable throwable) {
            span.error(throwable);
            throw throwable;
        } finally {
            span.finish();
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private Map<String, String> ensureHeaders(RpcRequest request) {
        Map<String, String> headers = request.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
            request.setHeaders(headers);
        }
        return headers;
    }

    private void fallbackTrace(Map<String, String> headers) {
        String traceId = headers.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
            headers.put(TRACE_ID_KEY, traceId);
        }
        RpcContext.setAttachments(headers);
        MDC.put(TRACE_ID_KEY, traceId);
    }
}

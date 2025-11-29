package cn.wenyuan.zrpc.core.filter.impl;


import cn.wenyuan.zrpc.common.context.RpcContext;
import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.Filter;
import cn.wenyuan.zrpc.core.filter.FilterChain;
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
        Map<String, String> headers = request.getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
            request.setHeaders(headers);
        }

        String traceId = headers.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
            headers.put(TRACE_ID_KEY, traceId);
        }

        RpcContext.setAttachments(headers);
        MDC.put(TRACE_ID_KEY, traceId);

        chain.doFilter(request, response);
    }
}

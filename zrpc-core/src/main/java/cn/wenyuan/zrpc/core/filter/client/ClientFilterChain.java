package cn.wenyuan.zrpc.core.filter.client;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端过滤器责任链，最终委托 Invocation 执行实际的网络调用。
 */
public final class ClientFilterChain {

    private final List<ClientFilter> filters;
    private final Invocation invocation;
    private int index = 0;

    ClientFilterChain(List<ClientFilter> filters, Invocation invocation) {
        this.filters = filters;
        this.invocation = invocation;
    }

    public CompletableFuture<RpcResponse> doFilter(RpcRequest request) {
        if (index < filters.size()) {
            ClientFilter filter = filters.get(index++);
            return filter.filter(request, this);
        }
        return invocation.invoke(request);
    }

    @FunctionalInterface
    public interface Invocation {
        CompletableFuture<RpcResponse> invoke(RpcRequest request);
    }
}

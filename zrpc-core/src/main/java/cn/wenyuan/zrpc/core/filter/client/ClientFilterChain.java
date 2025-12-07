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
    private int index;

    ClientFilterChain(List<ClientFilter> filters, Invocation invocation) {
        this(filters, invocation, 0);
    }

    private ClientFilterChain(List<ClientFilter> filters, Invocation invocation, int startIndex) {
        this.filters = filters;
        this.invocation = invocation;
        this.index = startIndex;
    }

    public CompletableFuture<RpcResponse> doFilter(RpcRequest request) {
        if (index < filters.size()) {
            ClientFilter filter = filters.get(index++);
            return filter.filter(request, this);
        }
        return invocation.invoke(request);
    }

    public ClientFilterChain forkFromCurrent() {
        return new ClientFilterChain(this.filters, this.invocation, this.index);
    }

    public ClientFilterChain forkFrom(int startIndex) {
        return new ClientFilterChain(this.filters, this.invocation, startIndex);
    }

    public int currentIndex() {
        return this.index;
    }

    @FunctionalInterface
    public interface Invocation {
        CompletableFuture<RpcResponse> invoke(RpcRequest request);
    }
}

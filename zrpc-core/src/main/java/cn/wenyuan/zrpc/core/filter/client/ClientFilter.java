package cn.wenyuan.zrpc.core.filter.client;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端过滤器接口。允许在发送 RPC 请求前后注入治理逻辑（熔断、重试等）。
 */
public interface ClientFilter {

    /**
     * @param request 当前请求
     * @param chain   调用链，用于继续传递请求
     * @return 代表本次调用的 Future
     */
    CompletableFuture<RpcResponse> filter(RpcRequest request, ClientFilterChain chain);
}

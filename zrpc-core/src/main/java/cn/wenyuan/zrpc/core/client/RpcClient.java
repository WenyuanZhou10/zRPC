package cn.wenyuan.zrpc.core.client;

import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;

import java.util.concurrent.CompletableFuture;

public interface RpcClient {
    CompletableFuture<RpcResponse> sendRequest(RpcRequest request);

    void connect();

    public void close();
}

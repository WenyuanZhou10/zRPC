package cn.wenyuan.zrpc.Client;

import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;

import java.util.concurrent.CompletableFuture;

public interface RpcClient {
    CompletableFuture<RpcResponse> sendRequest(RpcRequest request);

    void connect();

    public void close();
}

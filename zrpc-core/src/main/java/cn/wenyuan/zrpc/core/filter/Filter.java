package cn.wenyuan.zrpc.core.filter;

import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;

public interface Filter {
    void filter(RpcRequest request, RpcResponse response, FilterChain chain);
}

package cn.wenyuan.zrpc.core.filter;

import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;

public interface FilterChain {
    void doFilter(RpcRequest request, RpcResponse response);
}

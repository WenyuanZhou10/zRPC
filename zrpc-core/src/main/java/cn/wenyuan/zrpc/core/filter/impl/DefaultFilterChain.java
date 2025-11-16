package cn.wenyuan.zrpc.core.filter.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.FilterChain;

/**
 * @ClassName DefaultFilterChain
 * @Description TODO
 * @Author DefaultFilterChain
 * @Date 2025/11/16 21:19
 * @Version 1.0
 */

public class DefaultFilterChain implements FilterChain {
    @Override
    public void doFilter(
        RpcRequest request,
        RpcResponse response
    ) {

    }
}

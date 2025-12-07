package cn.wenyuan.zrpc.core.filter.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.Filter;
import cn.wenyuan.zrpc.core.filter.FilterChain;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @ClassName DefaultFilterChain
 * @Description TODO
 * @Author DefaultFilterChain
 * @Date 2025/11/16 21:19
 * @Version 1.0
 */
@Slf4j
public class DefaultFilterChain implements FilterChain {

    private final List<Filter> filters;
    private final Invoker finalInvoker;

    private int index = 0;

    public DefaultFilterChain(List<Filter> filters, Invoker invoker) {
        this.filters = filters;
        this.finalInvoker = invoker;
    }

    @Override
    public void doFilter(
        RpcRequest request,
        RpcResponse response
    ) {
        if (this.index < this.filters.size()) {
            Filter nextFilter = this.filters.get(index);
            this.index++;
            nextFilter.filter(request, response, this);
        } else if (finalInvoker != null) {
            try {
                finalInvoker.invoke(request, response);
            } catch (Exception ex) {
                throw new RuntimeException("执行最终业务逻辑失败", ex);
            }
        } else {
            log.info("所有过滤器执行完毕，准备执行核心业务逻辑");
        }
    }

    @FunctionalInterface
    public interface Invoker {
        void invoke(RpcRequest request, RpcResponse response) throws Exception;
    }
}

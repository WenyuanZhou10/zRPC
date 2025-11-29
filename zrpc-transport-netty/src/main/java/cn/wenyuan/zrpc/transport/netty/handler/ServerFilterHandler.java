package cn.wenyuan.zrpc.transport.netty.handler;

import cn.wenyuan.zrpc.common.context.RpcContext;
import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.FilterChain;
import cn.wenyuan.zrpc.core.filter.FilterManager;
import cn.wenyuan.zrpc.core.filter.impl.TraceFilter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
@ChannelHandler.Sharable
public class ServerFilterHandler extends SimpleChannelInboundHandler<RpcRequest> {

    public static final ServerFilterHandler INSTANCE = new ServerFilterHandler();

    private final FilterManager filterManager = FilterManager.getInstance();

    @Override
    protected void channelRead0(
        ChannelHandlerContext ctx,
        RpcRequest rpcRequest
    ) throws Exception {
        RpcResponse rpcResponse = RpcResponse.builder().requestId(rpcRequest.getRequestId()).build();

        FilterChain filterChain = filterManager.buildChain();
        try {
            filterChain.doFilter(rpcRequest, rpcResponse);

            if (rpcResponse.getError() != null) {
                ctx.writeAndFlush(rpcResponse);
                return;
            }

            ctx.fireChannelRead(rpcRequest);
        } catch (Throwable e) {
            log.error("【ServerFilterHandler】: Filter chain execution error for request: {}",
                      rpcRequest.getRequestId(), e);
            rpcResponse.setError(new RuntimeException("Filter chain error: " + e.getMessage()));

            ctx.writeAndFlush(rpcResponse).addListener(ChannelFutureListener.CLOSE);
        } finally {
            RpcContext.clear();
            MDC.remove(TraceFilter.TRACE_ID_KEY);
        }
    }
}

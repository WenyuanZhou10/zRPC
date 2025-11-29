package cn.wenyuan.zrpc.transport.netty.handler;

import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.core.filter.FilterChain;
import cn.wenyuan.zrpc.core.filter.FilterManager;
import io.netty.channel.*;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.common.StringUtils;

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

        } catch (Throwable e) {
            log.error("【ServerFilterHandler】: Filter chain execution error for request: {}",
                      rpcRequest.getRequestId(), e);
            rpcResponse.setError(new RuntimeException("Filter chain error: " + e.getMessage()));

            // 发生严重错误，直接写回并关闭连接
            ctx.writeAndFlush(rpcResponse).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        if (rpcResponse.getError() != null) {
            ctx.writeAndFlush(rpcResponse);
            return;
        }

        ctx.fireChannelRead(rpcResponse);
    }
}

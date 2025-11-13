package cn.wenyuan.zrpc.transport.netty.client;


import cn.wenyuan.zrpc.common.message.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @ClassName RpcClientHandler
 * @Description 业务处理器：从 pendingRequests Map 中找到对应的 Future，并用 RpcResponse 唤醒它
 * @Author RpcClientHandler
 * @Date 2025/10/31 00:30
 * @Version 1.0
 */
@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests;

    public NettyClientHandler(Map<String, CompletableFuture<RpcResponse>> pendingRequest) {
        this.pendingRequests = pendingRequest;
    }

    @Override
    protected void channelRead0(
        ChannelHandlerContext ctx,
        RpcResponse response
    ) throws Exception {
        String requestId = response.getRequestId();
        CompletableFuture<RpcResponse> future = pendingRequests.remove(requestId);
        if(future != null){
            future.complete(response);
        } else {
            log.warn("收到一个未知的响应 (或已超时的请求): RequestId = {}", requestId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        pendingRequests.forEach((requestId, future) -> {
            future.completeExceptionally(new RuntimeException("连接已断开"));
        });
        pendingRequests.clear();
        ctx.channel().close();
    }
}

package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.common.Message.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @ClassName RpcClientHandler
 * @Description 业务处理器：从 pendingRequests Map 中找到对应的 Future，并用 RpcResponse 唤醒它
 * @Author RpcClientHandler
 * @Date 2025/10/31 00:30
 * @Version 1.0
 */

@Data
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private final NettyClient nettyClient;

    public RpcClientHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    protected void channelRead0(
        ChannelHandlerContext ctx,
        RpcResponse response
    ) throws Exception {
        String requestId = response.getRequestId();
        Map<String, CompletableFuture<RpcResponse>> map = nettyClient.getPendingRequests();
        CompletableFuture<RpcResponse> future = map.get(requestId);
        if(future != null){
            future.complete(response);
        }
    }
}

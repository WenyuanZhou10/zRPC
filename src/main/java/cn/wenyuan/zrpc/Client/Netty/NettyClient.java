package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.Client.RpcClient;
import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @ClassName NettyClient
 * @Description TODO
 * @Author Wenyuan Zhou
 * @Date 2025/10/30 22:59
 * @Version 1.0
 */

@Data
public class NettyClient implements RpcClient {

    private String host;
    private int port;

    private final Bootstrap bootstrap;
    private static final EventLoopGroup group;
    private Channel channel;
    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests;

    public NettyClient(
        String host,
        int port
    ) {
        this.port = port;
        this.host = host;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(group)
                      .channel(NioSocketChannel.class)
                      .handler(new NettyClientInitializer(this));
    }

    static{
        group = new NioEventLoopGroup();
    }

    public void connect(){
        try {
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            this.channel = channelFuture.channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 这是一个同步的调用方法，会一直阻塞直到返回
     * @param request
     * @return
     */
    @Override
    public RpcResponse sendRequest(RpcRequest request) {
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(), future);
        try {
            channel.writeAndFlush(request);
            RpcResponse response = future.get();
            return response;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(request.getRequestId());
        }
    }

    public CompletableFuture<RpcResponse> sendRequestAsync(RpcRequest request){
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        future.whenComplete((res, ex) -> {
            pendingRequests.remove(request.getRequestId());
        });
        pendingRequests.put(request.getRequestId(), future);
        channel.writeAndFlush(request);
        return future;
    }
}

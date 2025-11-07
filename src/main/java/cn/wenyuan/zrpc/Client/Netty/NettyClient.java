package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.Client.RpcClient;
import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;
import cn.wenyuan.zrpc.network.client.RpcTimeoutManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @ClassName NettyClient
 * @Description TODO
 * @Author Wenyuan Zhou
 * @Date 2025/10/30 22:59
 * @Version 1.0
 */
@Slf4j
public class NettyClient implements RpcClient {

    private String host;
    private int port;

    private final Bootstrap bootstrap;
    private final EventLoopGroup group;
    private volatile Channel channel;
    @Getter
    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests;

    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public NettyClient(
        String host,
        int port
    ) {
        this.port = port;
        this.host = host;
        this.group = new NioEventLoopGroup();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(group)
                      .channel(NioSocketChannel.class)
                      .handler(new NettyClientInitializer(this.pendingRequests));
    }

    public void connect(){
        if (isClosed.get() || !isConnecting.compareAndSet(false, true)){
            return; // 如果已经在关或者已经在连接
        }
        log.info("zRPC 客户端开始连接 {}:{}...", host, port);
        ChannelFuture connectFuture = bootstrap.connect(host, port); // 异步连接

        connectFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("zRPC 客户端成功连接到 {}:{}", host, port);

                this.channel = future.channel();

                // 添加“关闭”监听器、Channel 断开时，会被触发
                // 在 Lambda 内部，我们明确知道 'future' 就是 'closeFuture'
                // 所以我们可以安全地调用我们自己的方法
                this.channel.closeFuture().addListener((ChannelFutureListener) this::onChannelClosed);

                isConnecting.set(false);
            } else {
                log.error("zRPC 客户端连接 {}:{} 失败, 5秒后重试...",
                        host, port, future.cause());

                isConnecting.set(false);

                scheduleReconnect(future.channel().eventLoop());
            }
        });
    }

    private void onChannelClosed(ChannelFuture closeFuture){
        // 主动关闭连接
        if(isClosed.get()){
            log.info("连接已正常关闭，将不再重连。");
            return;
        }
        // 意外断开
        log.warn("连接意外断开 {}:{}. 将在 5 秒后尝试重连...", host, port);
        // 调度重连
        scheduleReconnect(closeFuture.channel().eventLoop());
    }

    private void scheduleReconnect(EventLoop eventLoop) {
        if (isClosed.get() || !isConnecting.compareAndSet(false, true)) {
            return;
        }
        log.info("将在 5 秒后尝试重连...");
        eventLoop.schedule(() -> {
            isConnecting.set(false);
            connect();
        }, 5, TimeUnit.SECONDS);
    }

    public void close(){
        log.info("正在关闭 NettyClient...");
        isClosed.set(true); // 1. 设置标志，停止所有重连

        pendingRequests.forEach((id, future) ->{
            future.completeExceptionally(new RuntimeException("客户端已关闭"));
        });
        pendingRequests.clear();

        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    /**
     * 这是一个同步的调用方法，会一直阻塞直到返回
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<RpcResponse> sendRequest(RpcRequest request) {
        // 检查连接状态
        if(isClosed.get()){
            throw new RuntimeException("客户端已关闭");
        }
        final Channel currentChannel = this.channel;
        if(currentChannel == null || !currentChannel.isActive()){
            throw new RuntimeException("连接不可用，正在重连！");
        }

        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(), future);
        // 超时的兜底
        RpcTimeoutManager.scheduleTimeout(request.getRequestId(), future, request.getTimeoutMillis());
        // 正常完成时清理 pendingRequests 或由其他兜底所触发的清理
        future.whenComplete((res, ex) -> {
            pendingRequests.remove(request.getRequestId());
        });

        // 异步发送
        currentChannel.writeAndFlush(request).addListener(f -> {
            // 发送失败的兜底
            if(!f.isSuccess()){
                future.completeExceptionally(f.cause());// 触发 future.whenComplete 方法
            }
        });

        return future;
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

package cn.wenyuan.zrpc.transport.netty.client;


import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.transport.netty.codec.RpcFrameDecoder;
import cn.wenyuan.zrpc.transport.netty.codec.RpcMessageDecoder;
import cn.wenyuan.zrpc.transport.netty.codec.RpcMessageEncoder;
import cn.wenyuan.zrpc.transport.netty.config.HeartbeatConfig;
import cn.wenyuan.zrpc.transport.netty.handler.HeartbeatHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName NettyClientInitializer
 * @Description TODO
 * @Author NettyClientInitializer
 * @Date 2025/10/30 23:52
 * @Version 1.0
 */

@Slf4j
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    private final Map<String, CompletableFuture<RpcResponse>> pendingRequests;
    private final EventExecutorGroup businessGroup;

    public NettyClientInitializer(Map<String, CompletableFuture<RpcResponse>> pendingRequests,
                                  EventExecutorGroup businessGroup
    ) {
        this.pendingRequests = pendingRequests;
        this.businessGroup = businessGroup;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        // --- Outbound 编码器 (出站，从上到下执行) ---
        // 顺序: RpcRequest -> [RequestEncoder] -> ByteBuf -> [LengthPrepender] -> 带长度的ByteBuf

//        pipeline.addLast(new LengthFieldPrepender(4));
//        pipeline.addLast(new ObjectEncoder());
//
//
//        // --- Inbound 解码器 (入站，从上到下执行) ---
//        // 顺序: 带长度的ByteBuf -> [FrameDecoder] -> 裸ByteBuf -> [ResponseDecoder] -> RpcResponse
//
//        // 3. 帧解码器：在 RpcResponseDecoder 之前
//        //    解决粘包/半包问题。
//        //    它会读取 4 字节的长度头，并只传递一个完整的帧给下一个 handler。
//        pipeline.addLast(new LengthFieldBasedFrameDecoder(
//            Integer.MAX_VALUE, // 最大帧长
//            0,                 // 长度字段偏移量
//            4,                 // 长度字段字节数
//            0,                 // 长度调整值 (0 = 长度值就是消息体长度)
//            4                  // 剥离的字节数 (剥离 4 字节的长度头)
//        ));
//
//        // 4. RpcResponse 解码器：将 ByteBuf 反序列化为 RpcResponse 对象
//        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
//            @Override
//            public Class<?> resolve(String s) throws ClassNotFoundException {
//                return Class.forName(s);
//            }
//        }));

        // 出站
        pipeline.addLast("encoder", new RpcMessageEncoder());

        // 入站：客户端只关心写空闲，用于主动发送心跳
        int clientWriteIdleSeconds = HeartbeatConfig.clientWriteIdleSeconds();
        log.info("Configuring client channel {} with write idle timeout {}s",
                 channel.id(), clientWriteIdleSeconds);
        pipeline.addLast("idleStateHandler",
                         new IdleStateHandler(0, clientWriteIdleSeconds, 0, TimeUnit.SECONDS));
        pipeline.addLast("frameDecoder", new RpcFrameDecoder());
        pipeline.addLast("messageDecoder", new RpcMessageDecoder());
        pipeline.addLast("heartbeatHandler", HeartbeatHandler.INSTANCE);

        // 将 RpcClientHandler 添加到 "businessGroup" 中
        // Netty 会保证 RpcClientHandler 的所有事件（如 channelRead0, exceptionCaught）
        // 都在 businessGroup 的线程中执行，而不是 IO 线程
        if (businessGroup != null) {
            pipeline.addLast(businessGroup,"RpcClientHandler", new NettyClientHandler(this.pendingRequests));
        } else {
            pipeline.addLast("RpcClientHandler", new NettyClientHandler(this.pendingRequests));
        }
    }
}

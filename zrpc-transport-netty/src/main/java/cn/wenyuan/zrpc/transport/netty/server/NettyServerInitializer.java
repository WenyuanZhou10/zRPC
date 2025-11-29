package cn.wenyuan.zrpc.transport.netty.server;

import cn.wenyuan.zrpc.core.registry.impl.LocalServiceCache;
import cn.wenyuan.zrpc.transport.netty.codec.RpcFrameDecoder;
import cn.wenyuan.zrpc.transport.netty.codec.RpcMessageDecoder;
import cn.wenyuan.zrpc.transport.netty.codec.RpcMessageEncoder;
import cn.wenyuan.zrpc.transport.netty.config.HeartbeatConfig;
import cn.wenyuan.zrpc.transport.netty.handler.HeartbeatHandler;
import cn.wenyuan.zrpc.transport.netty.handler.ServerFilterHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName NettyServerInitializer
 * @Description TODO
 * @Author wenyuan.zhou
 * @Date 2025/10/31 10:12
 * @Version 1.0
 */

@Slf4j
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final LocalServiceCache serviceCache;
    private final EventExecutorGroup businessExecutorGroup;

    public NettyServerInitializer(
        LocalServiceCache serviceCache,
        EventExecutorGroup businessExecutorGroup
    ) {
        this.serviceCache = serviceCache;
        this.businessExecutorGroup = businessExecutorGroup;
    }


    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        // [入站] (Inbound - 处理 RpcRequest)
        // 1. 帧解码器：解决粘包/半包
        //    (这必须与客户端的 LengthFieldPrepender(4) 完全对应)
//        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
//        // 2. Java 对象解码器
//        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
//            @Override
//            public Class<?> resolve(String s) throws ClassNotFoundException {
//                return Class.forName(s);
//            }
//        }));
//        // [出站] (Outbound - 处理 RpcResponse)
//        // 3. 帧编码器：在响应包前添加 4 字节长度
//        //    (这必须与客户端的 LengthFieldBasedFrameDecoder 对应)
//        pipeline.addLast(new LengthFieldPrepender(4));
//
//        // 4. Java 对象编码器
//        //    (这必须与客户端的 ObjectDecoder 对应)
//        pipeline.addLast(new ObjectEncoder());

        // 出站
        pipeline.addLast("encoder", new RpcMessageEncoder());

        int serverReadIdleSeconds = HeartbeatConfig.serverReadIdleSeconds();
        log.info("Configuring server channel {} with read idle timeout {}s",
                 channel.remoteAddress(), serverReadIdleSeconds);

        // 入站：只关心读空闲
        pipeline.addLast("idleStateHandler",
                         new IdleStateHandler(serverReadIdleSeconds, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast("frameDecoder", new RpcFrameDecoder());
        pipeline.addLast("messageDecoder", new RpcMessageDecoder());

        pipeline.addLast("heartbeatHandler", HeartbeatHandler.INSTANCE);

        pipeline.addLast("filterHandler", ServerFilterHandler.INSTANCE);

        // 5. 执行业务，查找服务并执行；默认跑在用户提供的业务线程池里，避免占用 I/O 线程
        if (businessExecutorGroup != null) {
            pipeline.addLast(businessExecutorGroup, "RpcServerHandler", new NettyServerHandler(this.serviceCache));
        } else {
            pipeline.addLast("RpcServerHandler", new NettyServerHandler(this.serviceCache));
        }
    }
}

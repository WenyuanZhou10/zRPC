package cn.wenyuan.zrpc.Server.Netty.nettyInitializer;

import cn.wenyuan.zrpc.Server.Netty.handler.RpcServerHandler;
import cn.wenyuan.zrpc.common.Service.LocalServiceCache;
import cn.wenyuan.zrpc.network.codec.RpcFrameDecoder;
import cn.wenyuan.zrpc.network.codec.RpcMessageDecoder;
import cn.wenyuan.zrpc.network.codec.RpcMessageEncoder;
import cn.wenyuan.zrpc.network.config.HeartbeatConfig;
import cn.wenyuan.zrpc.network.handler.HeartbeatHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
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

    public NettyServerInitializer(LocalServiceCache serviceCache) {
        this.serviceCache = serviceCache;
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

        // 5. 执行业务，查找服务并执行
        pipeline.addLast("RpcServerHandler", new RpcServerHandler(this.serviceCache));
    }
}

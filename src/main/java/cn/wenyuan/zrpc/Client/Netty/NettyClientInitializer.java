package cn.wenyuan.zrpc.Client.Netty;


import cn.wenyuan.zrpc.common.Message.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * @ClassName NettyClientInitializer
 * @Description TODO
 * @Author NettyClientInitializer
 * @Date 2025/10/30 23:52
 * @Version 1.0
 */

public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    private NettyClient nettyClient;

    public NettyClientInitializer(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        // --- Outbound 编码器 (出站，从上到下执行) ---
        // 顺序: RpcRequest -> [RequestEncoder] -> ByteBuf -> [LengthPrepender] -> 带长度的ByteBuf

        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(new ObjectEncoder());


        // --- Inbound 解码器 (入站，从上到下执行) ---
        // 顺序: 带长度的ByteBuf -> [FrameDecoder] -> 裸ByteBuf -> [ResponseDecoder] -> RpcResponse

        // 3. 帧解码器：在 RpcResponseDecoder 之前
        //    解决粘包/半包问题。
        //    它会读取 4 字节的长度头，并只传递一个完整的帧给下一个 handler。
        pipeline.addLast(new LengthFieldBasedFrameDecoder(
            Integer.MAX_VALUE, // 最大帧长
            0,                 // 长度字段偏移量
            4,                 // 长度字段字节数
            0,                 // 长度调整值 (0 = 长度值就是消息体长度)
            4                  // 剥离的字节数 (剥离 4 字节的长度头)
        ));

        // 4. RpcResponse 解码器：将 ByteBuf 反序列化为 RpcResponse 对象
        pipeline.addLast(new ObjectEncoder());

        pipeline.addLast(new RpcClientHandler(nettyClient));
    }
}

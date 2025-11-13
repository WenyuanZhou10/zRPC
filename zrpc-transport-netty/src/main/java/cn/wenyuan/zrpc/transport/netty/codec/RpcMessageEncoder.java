package cn.wenyuan.zrpc.transport.netty.codec;


import cn.wenyuan.zrpc.common.message.HeartbeatRequest;
import cn.wenyuan.zrpc.common.message.HeartbeatResponse;
import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.common.protocol.RpcProtocolConstants;
import cn.wenyuan.zrpc.core.serializer.Serializer;
import cn.wenyuan.zrpc.core.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName RpcMessageEncoder
 * @Description 负责对消息进行打包
 * @Author RpcMessageEncoder
 * @Date 2025/11/1 21:00
 * @Version 1.0
 */
@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<Object> {
    @Override
    protected void encode(
        ChannelHandlerContext ctx,
        Object msg,
        ByteBuf out
    ) throws Exception {
        if(!(msg instanceof RpcRequest || msg instanceof RpcResponse
             || msg instanceof HeartbeatRequest
             || msg instanceof HeartbeatResponse)){
            log.warn("非法的消息类型被请求编码: {}", msg.getClass().getName());
            // ctx.write(msg); // 传给下一个 handler
            return; // 或者直接丢弃
        }

        out.writeInt(RpcProtocolConstants.MAGIC_NUMBER); // 写入魔数

        out.writeByte(RpcProtocolConstants.VERSION); // 写入版本

        // 写入序列化器代码（1字节）
        Serializer serializer = SerializerFactory.getDefault();
        out.writeByte(serializer.getCode());

        byte[] body = serializer.serialize(msg);

        out.writeInt(body.length); // 写入数据体长度

        out.writeBytes(body); // 写入数据体
    }
}

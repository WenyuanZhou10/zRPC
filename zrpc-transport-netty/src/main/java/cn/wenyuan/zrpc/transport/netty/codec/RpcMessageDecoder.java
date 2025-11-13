package cn.wenyuan.zrpc.transport.netty.codec;


import cn.wenyuan.zrpc.common.protocol.RpcProtocolFrame;
import cn.wenyuan.zrpc.core.serializer.Serializer;
import cn.wenyuan.zrpc.core.serializer.SerializerFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @ClassName RpcMessageDecoder
 * @Description 接收 RpcProtocolFrame 对象，获取特定序列器、反序列化
 * @Author RpcMessageDecoder
 * @Date 2025/11/1 21:30
 * @Version 1.0
 */
@Slf4j
public class RpcMessageDecoder extends MessageToMessageDecoder<RpcProtocolFrame> {
    @Override
    protected void decode(
        ChannelHandlerContext ctx,
        RpcProtocolFrame rpcProtocolFrame,
        List<Object> out
    ) throws Exception {
        byte serializerCode = rpcProtocolFrame.getSerializerCode();

        Serializer serializer = SerializerFactory.get(serializerCode);
        if(serializer == null){
            log.error("未找到 code [{}] 对应的序列化器", serializerCode);
            // 无法反序列化，丢弃该包
            return;
        }

        byte[] body = rpcProtocolFrame.getBody();
        if (body == null || body.length == 0) {
            log.warn("解码器收到了一个空的数据体");
            return;
        }

        Object msg = serializer.deserialize(body, Object.class);

        out.add(msg);
    }
}

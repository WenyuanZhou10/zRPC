package cn.wenyuan.zrpc.transport.netty.codec;


import cn.wenyuan.zrpc.common.protocol.RpcProtocolConstants;
import cn.wenyuan.zrpc.common.protocol.RpcProtocolFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @ClassName RpcFrameDecoder
 * @Description 读取字节流、解决黏包、半包的问题
 *              ByteToMessageDecoder 会通过 out 的大小是否有改变来判断当前是不是正确读取了，如果只是半包会重新调用 decode 方法
 * @Author RpcFrameDecoder
 * @Date 2025/11/1 21:14
 * @Version 1.0
 */
@Slf4j
public class RpcFrameDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(
        ChannelHandlerContext ctx,
        ByteBuf in,
        List<Object> out
    ) throws Exception {
        // 1. 检查是否足够读取头部，如果不够则说明是一个半包
        if(in.readableBytes() < RpcProtocolConstants.HEADER_TOTAL_LEN){
            // ByteToMessageDecoder 会自动缓存 `in` 中的数据
            return;
        }

        // 2. 记录当前读指针位置
        // 防止之后读取到半包需要回溯到当前位置
        in.markReaderIndex();

        // 3. 魔数校验
        int magic = in.readInt();
        if(magic != RpcProtocolConstants.MAGIC_NUMBER){
            log.error("无效的 Magic Number: {}. 判定为非法连接，即将关闭。", magic);
            in.resetReaderIndex();
            ctx.channel().close();
            return;
        }

        // 4.版本号
        byte version = in.readByte();
        if (version != RpcProtocolConstants.VERSION) {
            log.error("协议版本不匹配. 期望: {}, 收到: {}",
                      RpcProtocolConstants.VERSION, version);
            ctx.channel().close();
            return;
        }

        // 5. 序列化器代码
        byte serializerCode = in.readByte();

        // 6. 消息体长度
        int length = in.readInt();

        // 7. 检验消息提是否完整
        if(in.readableBytes() < length){
            log.warn("检测到半包: 头部声明长度 {}, 实际可读 {}, 等待更多数据...",
                     length, in.readableBytes());
            in.resetReaderIndex();
            // 等待更多数据
            return;
        }

        // 8. 读取消息体
        byte[] body = new byte[length];
        in.readBytes(body);

        RpcProtocolFrame frame = new RpcProtocolFrame();
        frame.setMagic(magic);
        frame.setVersion(version);
        frame.setSerializerCode(serializerCode);
        frame.setLength(length);
        frame.setBody(body);

        out.add(frame); // 讲完整的帧交付给下一个 Handler
    }
}

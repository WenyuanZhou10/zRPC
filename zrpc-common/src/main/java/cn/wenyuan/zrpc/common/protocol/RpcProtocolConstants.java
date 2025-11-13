package cn.wenyuan.zrpc.common.protocol;


/**
 * @ClassName RpcProtocolConstants
 * @Description TODO
 * @Author RpcProtocolConstants
 * @Date 2025/11/1 20:59
 * @Version 1.0
 */

public class RpcProtocolConstants {
    /**
     * 魔数 (Magic Number)，4 字节
     * 用于快速识别是否为本 RPC 协议的数据包
     */
    public static final int MAGIC_NUMBER = 0x5A525043; // "ZRPC"

    /**
     * 协议版本号，1 字节
     */
    public static final byte VERSION = 0x01;

    /**
     * 头部总长度 (Magic + Version + Serializer + Type + RequestId + Length)
     * 让我们先用一个简单的协议 (Simple Protocol v1)
     * Magic(4) + Version(1) + Serializer(1) + Length(4) = 10 字节
     */
    public static final int HEADER_TOTAL_LEN = 10;

    // --- 消息类型 (1 字节) ---
    // (我们先在 Encoder 里定义，因为这个简单协议 v1 里头部没有 MsgType)
    // (Kryo 的 writeClassAndObject 已经帮我们解决了类型问题)
}

package cn.wenyuan.zrpc.common.protocol; // (请使用你自己的包路径)

import lombok.Data;

/**
 * 自定义协议的数据帧
 * * RpcFrameDecoder 的输出，和 RpcMessageDecoder 的输入。
 */
@Data
public class RpcProtocolFrame {
    
    // ---- 头部信息 ----
    
    /**
     * 魔数
     */
    private int magic;

    /**
     * 协议版本
     */
    private byte version;

    /**
     * 序列化器代码
     */
    private byte serializerCode;

    /**
     * 数据体长度
     */
    private int length;
    
    // ---- 数据体 ----
    
    /**
     * 数据体 (RpcRequest / RpcResponse)
     */
    private byte[] body;
}
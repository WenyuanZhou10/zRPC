package cn.wenyuan.zrpc.core.serializer;

public interface Serializer {

    byte[] serialize(Object obj) throws Exception;

    <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception;

    // 返回此序列化器的唯一标识码。
    byte getCode();
}

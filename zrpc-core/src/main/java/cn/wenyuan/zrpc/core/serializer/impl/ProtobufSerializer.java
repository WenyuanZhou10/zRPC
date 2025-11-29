package cn.wenyuan.zrpc.core.serializer.impl;


import cn.wenyuan.zrpc.core.serializer.Serializer;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * 使用 Protostuff(Runtime Schema) 将 POJO 转成 Protobuf 二进制.
 * 由于框架在协议头里没有附带类型信息，我们在包体头部附带类名长度 + 类名，方便解码端动态构造类型。
 */
@Slf4j
public class ProtobufSerializer implements Serializer {

    private static final byte PROTOBUF_CODE = 0x02;
    private static final int NULL_FLAG = -1;

    private final Map<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

    @Override
    public byte[] serialize(Object obj) throws Exception {
        if (obj == null) {
            return ByteBuffer.allocate(Integer.BYTES).putInt(NULL_FLAG).array();
        }

        Class<?> clazz = obj.getClass();
        Schema<Object> schema = (Schema<Object>) getSchema(clazz);

        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        byte[] payload;
        try {
            payload = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }

        byte[] classNameBytes = clazz.getName().getBytes(StandardCharsets.UTF_8);

        ByteBuffer byteBuffer =
            ByteBuffer.allocate(Integer.BYTES + classNameBytes.length + payload.length);
        byteBuffer.putInt(classNameBytes.length);
        byteBuffer.put(classNameBytes);
        byteBuffer.put(payload);

        return byteBuffer.array();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        if (byteBuffer.remaining() < Integer.BYTES) {
            throw new IllegalArgumentException("非法的 Protobuf 数据，长度不足以读取类型信息");
        }

        int classNameLength = byteBuffer.getInt();
        if (classNameLength == NULL_FLAG) {
            return null;
        }
        if (classNameLength <= 0 || classNameLength > byteBuffer.remaining()) {
            throw new IllegalStateException("非法的类型名长度: " + classNameLength);
        }

        byte[] classNameBytes = new byte[classNameLength];
        byteBuffer.get(classNameBytes);
        String className = new String(classNameBytes, StandardCharsets.UTF_8);

        Class<?> targetClass = Class.forName(className);
        Schema<Object> schema = (Schema<Object>) getSchema(targetClass);

        Object message = schema.newMessage();
        byte[] payload = new byte[byteBuffer.remaining()];
        byteBuffer.get(payload);
        ProtostuffIOUtil.mergeFrom(payload, message, schema);
        return (T) message;
    }

    @Override
    public byte getCode() {
        return PROTOBUF_CODE;
    }

    @SuppressWarnings("unchecked")
    private <T> Schema<T> getSchema(Class<T> clazz) {
        return (Schema<T>) schemaCache.computeIfAbsent(clazz, RuntimeSchema::getSchema);
    }
}

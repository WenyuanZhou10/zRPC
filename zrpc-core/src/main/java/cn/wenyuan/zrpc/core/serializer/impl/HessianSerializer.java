package cn.wenyuan.zrpc.core.serializer.impl;


import cn.wenyuan.zrpc.core.serializer.Serializer;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 基于 Hessian2 的通用序列化器，适合跨语言和多版本兼容的场景。
 */
public class HessianSerializer implements Serializer {

    private static final byte HESSIAN_CODE = 0x03;

    private final SerializerFactory serializerFactory;

    public HessianSerializer() {
        SerializerFactory factory = new SerializerFactory();
        factory.setAllowNonSerializable(true);
        this.serializerFactory = factory;
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Hessian2Output output = new Hessian2Output(byteArrayOutputStream);
            output.setSerializerFactory(serializerFactory);
            try {
                output.writeObject(obj);
                output.flush();
                return byteArrayOutputStream.toByteArray();
            } finally {
                output.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            Hessian2Input input = new Hessian2Input(byteArrayInputStream);
            input.setSerializerFactory(serializerFactory);
            try {
                if (clazz == null || clazz == Object.class) {
                    return (T) input.readObject();
                }
                return (T) input.readObject(clazz);
            } finally {
                input.close();
            }
        }
    }

    @Override
    public byte getCode() {
        return HESSIAN_CODE;
    }
}

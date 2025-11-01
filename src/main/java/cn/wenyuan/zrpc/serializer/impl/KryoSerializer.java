package cn.wenyuan.zrpc.serializer.impl;


import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Message.RpcResponse;
import cn.wenyuan.zrpc.example.dto.User;
import cn.wenyuan.zrpc.serializer.Serializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.checkerframework.checker.units.qual.K;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @ClassName KryoSerializer
 * @Description TODO
 * @Author KryoSerializer
 * @Date 2025/11/1 20:11
 * @Version 1.0
 */

public class KryoSerializer implements Serializer {

    private static final byte KRYO_CODE = 0x01;

    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() ->{
        Kryo kryo = new Kryo();
        // 注册 RpcRequest 类
        kryo.register(RpcRequest.class);
        // 注册 RpcResponse 类
        kryo.register(RpcResponse.class);
        kryo.register(User.class);
        // 开启注册（Registration Required），提高安全性，防止反序列化漏洞
        // 设置为 false 可以序列化任何类，但有安全风险且性能稍低
        kryo.setRegistrationRequired(true);

        // 支持循环引用（如果你的对象图中有循环，需要开启）
        // kryo.setReferences(true);

        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) throws Exception {
        Kryo kryo = kryoThreadLocal.get();
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream)){

            kryo.writeClassAndObject(output, RpcRequest.class);

            output.flush();
            return byteArrayOutputStream.toByteArray();
        }
    }

    @Override
    public <T> T deserialize(
        byte[] bytes,
        Class<T> clazz
    ) throws Exception {
        Kryo kryo = kryoThreadLocal.get();
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {

            // 从 input 中读取对象
            T result = (T) kryo.readClassAndObject(input); // 对应 writeClassAndObject

            return result;
        }
    }

    @Override
    public byte getCode() {
        return KRYO_CODE;
    }
}

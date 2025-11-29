package cn.wenyuan.zrpc.core.serializer.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.message.RpcResponse;
import cn.wenyuan.zrpc.common.message.HeartbeatRequest;
import cn.wenyuan.zrpc.common.message.HeartbeatResponse;
import cn.wenyuan.zrpc.core.serializer.Serializer;
import cn.wenyuan.zrpc.core.serializer.spi.KryoRegistrar;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName KryoSerializer
 * @Description TODO
 * @Author KryoSerializer
 * @Date 2025/11/1 20:11
 * @Version 1.0
 */
@Slf4j
public class KryoSerializer implements Serializer {

    private static final byte KRYO_CODE = 0x01;
    private static final List<KryoRegistrar> REGISTRARS = loadRegistrars();

    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() ->{
        Kryo kryo = new Kryo();
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        kryo.register(HeartbeatRequest.class);
        kryo.register(HeartbeatResponse.class);
        // 请求里包含方法签名信息，需要显式注册 Class 及其数组类型
        kryo.register(Class.class);
        kryo.register(Class[].class);
        // 方法参数会以 Object[] 的形式承载
        kryo.register(Object[].class);

        for (KryoRegistrar registrar : REGISTRARS) {
            try {
                registrar.register(kryo);
            } catch (Exception ex) {
                log.warn("执行 KryoRegistrar {} 失败", registrar.getClass().getName(), ex);
            }
        }

        // 开启注册（Registration Required），提高安全性
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

            kryo.writeClassAndObject(output, obj);

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

    private static List<KryoRegistrar> loadRegistrars() {
        List<KryoRegistrar> registrars = new ArrayList<>();
        try {
            ServiceLoader<KryoRegistrar> loader = ServiceLoader.load(KryoRegistrar.class);
            for (KryoRegistrar registrar : loader) {
                registrars.add(registrar);
                log.info("Loaded Kryo registrar: {}", registrar.getClass().getName());
            }
            // 严格保证注册顺序和ID
            registrars.sort(Comparator.comparing(o -> o.getClass().getName()));
        } catch (Throwable ex) {
            log.warn("加载 KryoRegistrar SPI 失败", ex);
        }
        return Collections.unmodifiableList(registrars);
    }
}

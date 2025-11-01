package cn.wenyuan.zrpc.serializer;


import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName SerializerFactory
 * @Description TODO
 * @Author SerializerFactory
 * @Date 2025/11/1 21:32
 * @Version 1.0
 */
@Slf4j
public class SerializerFactory {
    private static final Map<Byte, Serializer> SERIALIZER_CACHE = new ConcurrentHashMap<>();

    private static final Serializer DEFAULT_SERIALIZER;

    static {
        log.info("开始加载 Serializer SPI...");
        ServiceLoader<Serializer> loader = ServiceLoader.load(Serializer.class);

        for (Serializer serializer : loader) {
            byte code = serializer.getCode();
            if (SERIALIZER_CACHE.containsKey(code)) {
                log.error("发现重复的 Serializer code: {}", code);
                throw new IllegalStateException("Serializer code " + code + " 重复！");
            }
            SERIALIZER_CACHE.put(code, serializer);
            log.info("成功加载序列化器: code={}, class={}", code, serializer.getClass().getName());
        }

        // 将 Kryo (code 0x01) 设为默认
        DEFAULT_SERIALIZER = SERIALIZER_CACHE.get((byte) 0x01);
        if (DEFAULT_SERIALIZER == null) {
            log.error("未找到 code 为 0x01 (Kryo) 的默认序列化器！");
        }
    }

    /**
     * 根据 code 获取序列化器实例
     */
    public static Serializer get(byte code) {
        Serializer serializer = SERIALIZER_CACHE.get(code);
        if (serializer == null) {
            log.warn("未找到 code 为 [{}] 的序列化器，将使用默认序列化器", code);
            return DEFAULT_SERIALIZER;
        }
        return serializer;
    }

    /**
     * 获取默认的序列化器 (Kryo)
     */
    public static Serializer getDefault() {
        return DEFAULT_SERIALIZER;
    }
}

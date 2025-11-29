package cn.wenyuan.zrpc.core.serializer;


import cn.wenyuan.zrpc.core.config.ApplicationConfig;
import cn.wenyuan.zrpc.core.config.ZrpcConfig;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

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

        DEFAULT_SERIALIZER = determineDefaultSerializer();
        if (DEFAULT_SERIALIZER != null) {
            log.info("默认序列化器加载完成: {}", DEFAULT_SERIALIZER.getClass().getName());
        } else {
            log.error("未能确定默认序列化器，检查 SPI 配置是否正确！");
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

    private static Serializer determineDefaultSerializer() {
        ZrpcConfig config = ApplicationConfig.getConfig();
        byte desiredCode = resolveDefaultSerializerCode(config != null ? config.getSerialization() : null);
        Serializer serializer = SERIALIZER_CACHE.get(desiredCode);
        if (serializer != null) {
            return serializer;
        }
        log.warn("配置的默认序列化器 code [{}] 未找到，尝试回退到 Kryo。", desiredCode);
        Serializer fallback = SERIALIZER_CACHE.get(SerializerType.KRYO.getCode());
        if (fallback != null) {
            return fallback;
        }
        return SERIALIZER_CACHE.values().stream().findFirst().orElse(null);
    }

    private static byte resolveDefaultSerializerCode(ZrpcConfig.SerializationConfig serializationConfig) {
        if (serializationConfig == null) {
            return SerializerType.KRYO.getCode();
        }
        if (serializationConfig.getDefaultCode() != null) {
            return serializationConfig.getDefaultCode();
        }
        return SerializerType.fromName(serializationConfig.getDefaultType())
            .map(SerializerType::getCode)
            .orElse(SerializerType.KRYO.getCode());
    }
}

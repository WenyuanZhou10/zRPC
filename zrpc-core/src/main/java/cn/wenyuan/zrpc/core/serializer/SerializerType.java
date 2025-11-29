package cn.wenyuan.zrpc.core.serializer;


import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 内置序列化器的枚举，方便通过配置名称映射到对应的 code。
 */
@Getter
public enum SerializerType {
    KRYO((byte) 0x01, "kryo"),
    PROTOBUF((byte) 0x02, "protobuf");

    private final byte code;
    private final String configName;

    SerializerType(byte code, String configName) {
        this.code = code;
        this.configName = configName;
    }

    public static Optional<SerializerType> fromName(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        final String normalized = name.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(type -> type.configName.equals(normalized))
            .findFirst();
    }

    public static Optional<SerializerType> fromCode(byte code) {
        return Arrays.stream(values())
            .filter(type -> type.code == code)
            .findFirst();
    }
}

package cn.wenyuan.zrpc.example.serialization;

import cn.wenyuan.zrpc.example.dto.User;
import cn.wenyuan.zrpc.serializer.spi.KryoRegistrar;
import com.esotericsoftware.kryo.Kryo;

/**
 * 示例项目的 Kryo 注册扩展，演示如何为业务 DTO 注册序列化支持。
 */
public class DemoKryoRegistrar implements KryoRegistrar {
    @Override
    public void register(Kryo kryo) {
        kryo.register(User.class);
    }
}

package cn.wenyuan.zrpc.serializer.spi;

import com.esotericsoftware.kryo.Kryo;

/**
 * SPI 接口：允许模块向 KryoSerializer 注册额外的业务类。
 */
public interface KryoRegistrar {

    /**
     * 在创建 Kryo 实例时被调用，调用方可以在这里执行自定义的 kryo.register。
     */
    void register(Kryo kryo);
}

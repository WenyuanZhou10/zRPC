package cn.wenyuan.zrpc.core.registry;

import cn.wenyuan.zrpc.common.service.ServiceInstance;

public interface ServiceRegistry {

    void register(ServiceInstance instance) throws Exception;

    void unregister(ServiceInstance instance) throws Exception;

    void close();

}

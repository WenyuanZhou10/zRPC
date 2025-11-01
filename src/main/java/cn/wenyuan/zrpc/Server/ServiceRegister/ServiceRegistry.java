package cn.wenyuan.zrpc.Server.ServiceRegister;

import cn.wenyuan.zrpc.common.Service.ServiceInstance;

public interface ServiceRegistry {

    void register(ServiceInstance instance) throws Exception;

    void unregister(ServiceInstance instance) throws Exception;

    void close();

}

package cn.wenyuan.zrpc.core.registry;

import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.service.ServiceInstance;

import java.util.List;

public interface ServiceDiscovery {
    List<ServiceInstance> getInstances(String serviceName) throws Exception;

    ServiceInstance getInstance(String serviceName, RpcRequest request) throws Exception;

    void close();
}

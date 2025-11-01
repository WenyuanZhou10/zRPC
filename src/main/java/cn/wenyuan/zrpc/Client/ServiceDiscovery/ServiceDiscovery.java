package cn.wenyuan.zrpc.Client.ServiceDiscovery;

import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Service.ServiceInstance;

import java.util.List;

public interface ServiceDiscovery {
    List<ServiceInstance> getInstances(String serviceName) throws Exception;

    ServiceInstance getInstance(String serviceName, RpcRequest request) throws Exception;

    void close();
}

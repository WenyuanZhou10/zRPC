package cn.wenyuan.zrpc.registry.loadbalance;

import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Service.ServiceInstance;

import java.util.List;

public interface LoadBalancer {
    ServiceInstance select(List<ServiceInstance> instances, RpcRequest request);

    /**
     * 返回此负载均衡策略的唯一名称。
     * 这个名称将用于 SPI 的查找。
     * @return 策略名称 (例如: "roundrobin", "consistenthash")
     */
    String getName();
}

package cn.wenyuan.zrpc.core.loadbalance.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.service.ServiceInstance;
import cn.wenyuan.zrpc.core.loadbalance.LoadBalancer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ClassName RoundRobinLoadBalancer
 * @Description 轮询法负载均衡器
 * @Author RoundRobinLoadBalancer
 * @Date 2025/11/1 13:59
 * @Version 1.0
 */
@Slf4j
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public ServiceInstance select(List<ServiceInstance> instances, RpcRequest request) {
        if(instances.isEmpty()){
            return null;
        }

        if(instances.size() == 1){
            return instances.getFirst();
        }

        int currentIndex = getNextIndex(instances.size());
        ServiceInstance instance = instances.get(currentIndex);

        log.info("轮询负载均衡选择了第 {} 号服务器，地址是：{}", currentIndex, instance.getHost() + ":" + instance.getPort());

        return instance;
    }

    @Override
    public String getName() {
        return "roundrobin";
    }

    private int getNextIndex(int size) {
        int currentVal = index.getAndIncrement();// 返回旧值
        // 防止溢出
        // 如果 currentVal 是正数，结果不变
        // 如果 currentVal 是负数（溢出导致），这个操作会强制将其符号位变为0
        return (currentVal & 0x7FFFFFFF) % size;
    }
}

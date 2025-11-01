package cn.wenyuan.zrpc.registry.loadbalance.impl;


import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Service.ServiceInstance;
import cn.wenyuan.zrpc.registry.loadbalance.LoadBalancer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @ClassName WeightedLoadBalancer
 * @Description 权重负载均衡器
 * @Author WeightedLoadBalancer
 * @Date 2025/11/1 15:04
 * @Version 1.0
 */

@Slf4j
public class WeightedLoadBalancer implements LoadBalancer {

    private static final int DEFAULT_WEIGHT = 1;

    @Override
    public ServiceInstance select(List<ServiceInstance> instances, RpcRequest request) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        if (instances.size() == 1) {
            return instances.get(0);
        }

        int totalWeight = 0;
        boolean allSameWeight = true;
        int prevWeight = -1;

        for(ServiceInstance instance : instances){
            int weight = getWeight(instance);
            totalWeight += weight;

            if(prevWeight == -1){
                prevWeight = weight;
            } else if(prevWeight != weight){
                allSameWeight = false;
            }
        }

        ServiceInstance serviceInstance = instances.getFirst();
        if(allSameWeight){
            // 权重相同，直接随机
            int index = ThreadLocalRandom.current().nextInt(instances.size());
            serviceInstance = instances.get(index);
        } else {
            // 权重算法
            // 在 [0, totalWeight) 中选一个随机数
            int randomOffset = ThreadLocalRandom.current().nextInt(totalWeight);
            int currentSum = 0;
            for(ServiceInstance instance : instances){
                int currentWeight = getWeight(instance);
                currentSum += currentWeight;

                if(randomOffset < currentSum){
                    serviceInstance = instance;
                }
            }
        }

        log.info("权重负载均衡选择了服务器，地址是：{}", serviceInstance.getHost() + ":" + serviceInstance.getPort());

        return serviceInstance;
    }

    private int getWeight(ServiceInstance instance) {
        Integer weight = instance.getWeight();
        if(weight == null || weight <= 0){
            return DEFAULT_WEIGHT;
        }
        return weight;
    }
}

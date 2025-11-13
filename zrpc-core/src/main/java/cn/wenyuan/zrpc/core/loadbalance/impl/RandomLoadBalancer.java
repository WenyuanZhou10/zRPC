package cn.wenyuan.zrpc.core.loadbalance.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.service.ServiceInstance;
import cn.wenyuan.zrpc.core.loadbalance.LoadBalancer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;

/**
 * @ClassName RandomLoadBalancer
 * @Description 随机法负载均衡器
 * @Author RandomLoadBalancer
 * @Date 2025/11/1 14:29
 * @Version 1.0
 */
@Slf4j
public class RandomLoadBalancer implements LoadBalancer {



    private final Random random = new Random();

    @Override
    public ServiceInstance select(List<ServiceInstance> instances, RpcRequest request) {
        if(instances == null || instances.isEmpty()){
            throw new RuntimeException("instances is null");
        }

        int index = random.nextInt(instances.size());
        ServiceInstance instance = instances.get(index);

        log.info("随机负载均衡选择了第 {} 号服务器，地址是：{}", index, instance.getHost() + ":" + instance.getPort());

        return instance;
    }

    @Override
    public String getName() {
        return "random";
    }
}

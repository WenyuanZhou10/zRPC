package cn.wenyuan.zrpc.registry.loadbalance.impl;


import cn.wenyuan.zrpc.common.Message.RpcRequest;
import cn.wenyuan.zrpc.common.Service.ServiceInstance;
import cn.wenyuan.zrpc.registry.loadbalance.LoadBalancer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.hash;

/**
 * @ClassName ConsistentHashLoadBalancer
 * @Description 一致性哈希轮询器
 * @Author ConsistentHashLoadBalancer
 * @Date 2025/11/1 16:10
 * @Version 1.0
 */

@Slf4j
public class ConsistentHashLoadBalancer implements LoadBalancer {

    private final Map<String, HashRing> rings = new ConcurrentHashMap<>();

    // 虚拟节点数量，数量越多，分布越均匀，成本和内存开销越大
    private static final int DEFAULT_VIRTUAL_NODES = 20;

    // 默认权重
    private static final int DEFAULT_WEIGHT = 1;
    @Override
    public ServiceInstance select(List<ServiceInstance> instances, RpcRequest request) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        String serviceName = request.getService();
        HashRing ring = rings.get(serviceName);

        // 检查哈希环是否需要重建（服务列表是否变化）
        int instancesHash = instances.hashCode();
        if (ring == null || ring.getInstancesHash() != instancesHash) {
            log.info("服务列表变更，正在为 [{}] 重建一致性哈希环...", serviceName);
            // 重建哈希环
            ring = new HashRing(instances);
            rings.put(serviceName, ring);
        }
        String key = getHashKey(request);
        return ring.getNode(key);
    }

    private String getHashKey(RpcRequest request){
        if(request.getParams() != null && request.getParams().length > 0){
            Object paramOne = request.getParams()[0];
            if(paramOne != null){
                return paramOne.toString();
            }
        }
        return request.getRequestId();
    }


    /**
     * 内部类：表示一个服务的一致性哈希环
     */
    private static class HashRing{
        private final SortedMap<Long, ServiceInstance> virtualNodes;

        // 用来检测当前实例是否发生变化
        @Getter
        private final int instancesHash;

        private HashRing(
            List<ServiceInstance> instances
        ) {
            this.virtualNodes = new TreeMap<>();
            this.instancesHash = instances.hashCode();


            for(ServiceInstance instance : instances){
                // 根据权重创建虚拟节点数量
                int weight = getWeight(instance);
                int nodes = weight * DEFAULT_VIRTUAL_NODES;

                for(int i = 0; i < nodes; i++){
                    String virtualNodeKey = instance.getInstanceKey() + "-VN-" + i;
                    long hash = hash(virtualNodeKey);
                    virtualNodes.put(hash, instance);
                }
            }
        }

        private int getWeight(ServiceInstance instance) {
            if (instance.getWeight() == null || instance.getWeight() <= 0) {
                return DEFAULT_WEIGHT;
            }
            return instance.getWeight();
        }

        public ServiceInstance getNode(String key){
            if (virtualNodes.isEmpty()) {
                return null;
            }

            long hash = hash(key);

            SortedMap<Long, ServiceInstance> tailMap = virtualNodes.tailMap(hash);
            if(tailMap.isEmpty()){
                return virtualNodes.get(virtualNodes.firstKey());
            }

            return tailMap.get(tailMap.firstKey());
        }
    }


}

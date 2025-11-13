package cn.wenyuan.zrpc.core.registry.impl;


import cn.wenyuan.zrpc.common.message.RpcRequest;
import cn.wenyuan.zrpc.common.service.ServiceInstance;
import cn.wenyuan.zrpc.core.loadbalance.LoadBalancer;
import cn.wenyuan.zrpc.core.loadbalance.LoadBanlancerFactory;
import cn.wenyuan.zrpc.core.registry.ServiceRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName ZKServiceRegister
 * @Description ZooKeeper
 * @Author ZKServiceRegister
 * @Date 2025/11/1 00:12
 * @Version 1.0
 */

public class ZKServiceRegistry implements ServiceRegistry, cn.wenyuan.zrpc.core.registry.ServiceDiscovery {

    // Curator 客户端
    private final CuratorFramework client;
    // Curator 提供的服务发现 API
    private final ServiceDiscovery<ServiceInstance> serviceDiscovery;
    // ZK 中的根路径
    private static final String ZK_BASE_PATH = "/zrpc/service";
    // 负载均衡策略
    private final LoadBalancer loadBalancer = LoadBanlancerFactory.get("consistenthash");

    public ZKServiceRegistry(String zkAddress) throws Exception {
        this.client = CuratorFrameworkFactory.builder()
            .connectString("127.0.0.1:21888")
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();
        this.client.start();
        JsonInstanceSerializer<ServiceInstance> serializer =
            new JsonInstanceSerializer<>(ServiceInstance.class);

        this.serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceInstance.class)
                                                       .client(client)
                                                       .basePath(ZK_BASE_PATH) // ZK 中的根节点
                                                       .serializer(serializer)
                                                       .build();
        this.serviceDiscovery.start();
    }

    @Override
    public void register(ServiceInstance instance) throws Exception {
        org.apache.curator.x.discovery.ServiceInstance<ServiceInstance> curatorInstance =
            org.apache.curator.x.discovery.ServiceInstance.<ServiceInstance>builder()
                                            .name(instance.getServiceName()) // 服务名
                                            .address(instance.getHost())     // host
                                            .port(instance.getPort())        // port
                                            .payload(instance)               // [关键] 完整的实例信息作为 payload
                                            .build();
        serviceDiscovery.registerService(curatorInstance);
    }

    @Override
    public void unregister(ServiceInstance instance) throws Exception {
        org.apache.curator.x.discovery.ServiceInstance<ServiceInstance> curatorInstance =
            org.apache.curator.x.discovery.ServiceInstance.<ServiceInstance>builder()
                                                          .name(instance.getServiceName())
                                                          .address(instance.getHost())
                                                          .port(instance.getPort())
                                                          .payload(instance)
                                                          .build();
        serviceDiscovery.unregisterService(curatorInstance);
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws Exception {
        return serviceDiscovery.queryForInstances(serviceName).stream()
            .map(org.apache.curator.x.discovery.ServiceInstance::getPayload)
            .collect(Collectors.toList());
    }


    // TODO:使用SPI机制自定义负载均衡策略
    @Override
    public ServiceInstance getInstance(String serviceName, RpcRequest request) throws Exception {
        List<ServiceInstance> instances = getInstances(serviceName);
        ServiceInstance serviceInstance = loadBalancer.select(instances, request);
        return serviceInstance;
    }

    @Override
    public void close() {
        try {
            if (serviceDiscovery != null) {
                serviceDiscovery.close();
            }
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
